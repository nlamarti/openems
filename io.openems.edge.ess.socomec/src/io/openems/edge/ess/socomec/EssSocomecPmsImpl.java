package io.openems.edge.ess.socomec;

import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class EssSocomecPmsImpl extends AbstractOpenemsSunSpecComponent implements EssSocomecPms,
    SymmetricEss, ManagedSymmetricEss, EventHandler, ModbusComponent, ModbusSlave, OpenemsComponent {

  @Reference
  private Power power;


  @Reference
  private ConfigurationAdmin cm;

  private int alarmCount;
  private Instant lastAlarmTime;

  public static final ElementToChannelConverter GRID_MODE_CONVERTER = new ElementToChannelConverter(
      // element -> channel
      value -> {
        if (GridMode.ON_GRID.equals(value)) {
          return DefaultSunSpecModel.S701_ConnSt.CONNECTED;
        }
        if (GridMode.OFF_GRID.equals(value)) {
          return DefaultSunSpecModel.S701_ConnSt.DISCONNECTED;
        }
        return DefaultSunSpecModel.S701_ConnSt.UNDEFINED;
      }, //
      // channel -> element
      value -> {
        if (DefaultSunSpecModel.S701_ConnSt.CONNECTED.equals(value)) {
          return GridMode.ON_GRID;
        }
        if (DefaultSunSpecModel.S701_ConnSt.DISCONNECTED.equals(value)) {
          return GridMode.OFF_GRID;
        }
        return GridMode.UNDEFINED;
      });
  private final Logger log = LoggerFactory.getLogger(EssSocomecPmsImpl.class);
  private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
      .put(DefaultSunSpecModel.S_1, Priority.LOW)
      .put(DefaultSunSpecModel.S_701, Priority.HIGH)
      .put(DefaultSunSpecModel.S_702, Priority.LOW)
      .put(DefaultSunSpecModel.S_704, Priority.HIGH)
      .put(DefaultSunSpecModel.S_713, Priority.LOW)
      .put(DefaultSunSpecModel.S_802, Priority.LOW)
      .put(DefaultSunSpecModel.S_715, Priority.HIGH)
      .put(SocomecSunspecModel.S_64901, Priority.LOW)
      .build();
  // Further available SunSpec blocks provided by Socomec PMS are:
  // .put(DefaultSunSpecModel.S_703, Priority.LOW)
  // .put(DefaultSunSpecModel.S_705, Priority.LOW)
  // .put(DefaultSunSpecModel.S_706, Priority.LOW)
  // .put(DefaultSunSpecModel.S_803, Priority.LOW)
  // 64902 Socomec specific

  /**
   * Constructs a EssSocomesPMS.
   *
   * @throws OpenemsException on error
   */
  public EssSocomecPmsImpl() throws OpenemsException {
    super(ACTIVE_MODELS, //
        OpenemsComponent.ChannelId.values(), //
        ModbusComponent.ChannelId.values(), //
        ManagedSymmetricEss.ChannelId.values(), //
        SymmetricEss.ChannelId.values(),
        EssSocomecPms.ChannelId.values());
  }


  @Activate
  private boolean activate(ComponentContext context, Config config) throws OpenemsException {
    return super.activate(context, config.id(), config.alias(), config.enabled(),
        config.modbusUnitId(), this.cm, "Modbus", config.modbus_id(), 1);
  }

  @Override
  protected void onSunSpecInitializationCompleted() {
    this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.CAPACITY,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S802.W_H_RTG);

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.GRID_MODE,
        GRID_MODE_CONVERTER,
        DefaultSunSpecModel.S701.CONN_ST);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.STATUS,
        Status.STATUS_CONVERTER,
        DefaultSunSpecModel.S701.INV_ST);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.HEARTBEAT,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S715.D_E_R_HB);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.SET_HEARTBEAT,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S715.CONTROLLER_HB);

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.MAX_APPARENT_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S702.W_MAX_RTG);

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.SOC,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S802.SO_C);

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.ACTIVE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S701.W);

    this.mapFirstPointToChannel(SymmetricEss.ChannelId.REACTIVE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S701.VAR);

    this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S702.W_CHA_RTE_MAX, DefaultSunSpecModel.S802.W_CHA_RTE_MAX);

    this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S702.W_DIS_CHA_RTE_MAX, DefaultSunSpecModel.S802.W_DIS_CHA_RTE_MAX);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.SET_OPERATION,
        SetOperation.SET_OPERATION_CONTROL_CONVERTER,
        DefaultSunSpecModel.S715.OP_CTL);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.BATTERY_CONTROL,
        Control.BATTERY_CONTROL_CONVERTER,
        DefaultSunSpecModel.S802.SET_OP);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.ACTIVE_POWER_CONTROL,
        Control.ACTIVE_POWER_CONTROL_CONVERTER,
        DefaultSunSpecModel.S704.W_SET_ENA);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.SET_ACTIVE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S704.W_SET);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.ACTIVE_POWER_MODE,
        PowerMode.ACTIVE_POWER_MODE_CONVERTER,
        DefaultSunSpecModel.S704.W_SET_MOD);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.REACTIVE_POWER_CONTROL,
        Control.REACTIVE_POWER_CONTROL_CONVERTER,
        DefaultSunSpecModel.S704.VAR_SET_ENA);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.SET_REACTIVE_POWER,
        ElementToChannelConverter.DIRECT_1_TO_1,
        DefaultSunSpecModel.S704.VAR_SET);

    this.mapFirstPointToChannel(EssSocomecPms.ChannelId.REACTIVE_POWER_MODE,
        PowerMode.REACTIVE_POWER_MODE_CONVERTER,
        DefaultSunSpecModel.S704.VAR_SET_MOD);

    this.channel(EssSocomecPms.ChannelId.STATUS).onUpdate(v -> {
      switch ((Status) v.asEnum()) {
        case OFF -> {
          this.alarmCount = 0;
          this.channel(EssSocomecPms.ChannelId.BATTERY_CONTROL).setNextValue(Control.ON);
          this.channel(EssSocomecPms.ChannelId.SET_OPERATION).setNextValue(SetOperation.START);
        }
        case RUNNING -> {
          this.channel(EssSocomecPms.ChannelId.ACTIVE_POWER_CONTROL).setNextValue(Control.ON);
          this.channel(EssSocomecPms.ChannelId.ACTIVE_POWER_MODE).setNextValue(PowerMode.WATTS);
          this.channel(EssSocomecPms.ChannelId.REACTIVE_POWER_CONTROL).setNextValue(Control.ON);
          this.channel(EssSocomecPms.ChannelId.REACTIVE_POWER_MODE).setNextValue(PowerMode.VARS);
        }
        default -> {
        }
      }
    });
  }

  private void recoverAlarm() {
    if (Duration.between(this.lastAlarmTime, Instant.now()).toSeconds() > Duration.ofSeconds(30).toSeconds()) {
      Status s = this.channel(EssSocomecPms.ChannelId.STATUS).getNextValue().asEnum();
      if (s.equals(Status.FAULT)) {
        getSunSpecChannel(SocomecSunspecModel.S64901.SW2).ifPresent(channel -> {
          if (!channel.getNextValue().asEnum().equals(SocomecSunspecModel.S64901_SW2.PCS_DRYING_IN_PROGRESS) && this.alarmCount < 3) {
            getSunSpecChannel(DefaultSunSpecModel.S715.ALARM_RESET).ifPresent(c -> {
              c.setNextValue(1);
              this.lastAlarmTime = Instant.now();
              this.alarmCount++;
            });
          }
        });
      }
    }
  }

  @Override
  public void handleEvent(Event event) {
    switch (event.getTopic()) {
      case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.setHeartbeat();
      case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE -> this.recoverAlarm();
    }
  }

  private void setHeartbeat() {
    Channel<Integer> h = this.channel(EssSocomecPms.ChannelId.SET_HEARTBEAT);
    h.setNextValue(h.value().orElse(0) + 1);
  }

  @Override
  public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
    return new ModbusSlaveTable(//
        OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
        SymmetricEss.getModbusSlaveNatureTable(accessMode), //
        ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode));
  }

  @Override
  public Power getPower() {
    return this.power;
  }

  @Override
  public void applyPower(int activePower, int reactivePower) throws OpenemsError.OpenemsNamedException {
    IntegerWriteChannel setActivePowerChannel = this.channel(EssSocomecPms.ChannelId.SET_ACTIVE_POWER);
    IntegerWriteChannel setReactivePowerChannel = this.channel(EssSocomecPms.ChannelId.SET_REACTIVE_POWER);

    setActivePowerChannel.setNextWriteValue(activePower);
    setReactivePowerChannel.setNextWriteValue(reactivePower);
  }

  @Override
  public int getPowerPrecision() {
    return 1;
  }

  @Override
  public Constraint[] getStaticConstraints() throws OpenemsError.OpenemsNamedException {
    Constraint[] constraints = {
        this.createPowerConstraint("PMS not running", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
        this.createPowerConstraint("PMS not running", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0)
    };
    Status status = this.channel(EssSocomecPms.ChannelId.STATUS).value().asEnum();
    switch (status) {
      case THROTTLED, RUNNING -> {
        return Power.NO_CONSTRAINTS;
      }
    }
    ;
    return constraints;
  }

  @Override
  public String debugLog() {
    return "SoC:" + this.getSoc().asString() //
        + "|L:" + this.getActivePower().asString() //
        + "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
        + this.getAllowedDischargePower().asString() //
        + "|" + this.getGridModeChannel().value().asOptionString();
  }
}
