package io.openems.edge.ess.socomec;

import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongWriteChannel;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Ess.Socomec.PMS", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, })
public class EssSocomecPmsImpl extends AbstractOpenemsSunSpecComponent implements EssSocomecPms, SymmetricEss,
		ManagedSymmetricEss, EventHandler, ModbusComponent, ModbusSlave, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssSocomecPmsImpl.class);

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	protected Config config;

	private int alarmCount;
	private Instant lastAlarmTime;

	public static final ElementToChannelConverter GRID_MODE_CONVERTER = new ElementToChannelConverter(
			// channel -> element
			value -> {
				int v = (int) value;
				if (DefaultSunSpecModel.S701_ConnSt.CONNECTED.getValue() == v) {
					return GridMode.ON_GRID;
				}
				if (DefaultSunSpecModel.S701_ConnSt.DISCONNECTED.getValue() == v) {
					return GridMode.OFF_GRID;
				}
				return GridMode.UNDEFINED;
			});

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW).put(DefaultSunSpecModel.S_701, Priority.HIGH)
			.put(DefaultSunSpecModel.S_702, Priority.LOW).put(DefaultSunSpecModel.S_704, Priority.HIGH)
			.put(DefaultSunSpecModel.S_713, Priority.LOW).put(DefaultSunSpecModel.S_802, Priority.LOW)
			.put(DefaultSunSpecModel.S_715, Priority.HIGH)
			// Further available SunSpec blocks provided by Socomec PMS are:
			// .put(DefaultSunSpecModel.S_703, Priority.LOW)
			// .put(DefaultSunSpecModel.S_705, Priority.LOW)
			// .put(DefaultSunSpecModel.S_706, Priority.LOW)
			// .put(DefaultSunSpecModel.S_803, Priority.LOW)
			// .put(SocomecSunspecModel.S_64901, Priority.LOW)
			// .put(SocomecSunspecModel.S_64902, Priority.LOW)
			.build();

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
				SymmetricEss.ChannelId.values(), EssSocomecPms.ChannelId.values());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), 1)) {
			return;
		}
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.CAPACITY, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S802.W_H_RTG);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.GRID_MODE, GRID_MODE_CONVERTER,
				DefaultSunSpecModel.S701.CONN_ST);

		this.mapFirstPointToChannel(EssSocomecPms.ChannelId.STATUS, Status.STATUS_CONVERTER,
				DefaultSunSpecModel.S701.INV_ST);

		this.mapFirstPointToChannel(EssSocomecPms.ChannelId.HEARTBEAT, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S715.D_E_R_HB);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.MAX_APPARENT_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S702.V_A_MAX_RTG);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.MAX_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S802.CELL_V_MAX);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.MIN_CELL_VOLTAGE, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S802.CELL_V_MIN);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.SOC, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S802.SO_C);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.ACTIVE_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S701.W);

		this.mapFirstPointToChannel(SymmetricEss.ChannelId.REACTIVE_POWER, ElementToChannelConverter.DIRECT_1_TO_1,
				DefaultSunSpecModel.S701.VAR);

		this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
				ElementToChannelConverter.INVERT, DefaultSunSpecModel.S802.W_CHA_RTE_MAX);

		this.mapFirstPointToChannel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
				ElementToChannelConverter.DIRECT_1_TO_1, DefaultSunSpecModel.S802.W_DIS_CHA_RTE_MAX);

		// Change this to run in the handle function
		this.channel(EssSocomecPms.ChannelId.STATUS).onUpdate(v -> {
			switch ((Status) v.asEnum()) {
			case OFF -> start();
			case RUNNING -> enableCommunication();
			default -> {
			}
			}
		});
	}

	private void start() {
		try {
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S802.SET_OP))
					.setNextWriteValue(DefaultSunSpecModel.S802_SetOp.CONNECT);
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S715.OP_CTL))
					.setNextWriteValue(DefaultSunSpecModel.S715_OpCtl.START);
		} catch (OpenemsNamedException e) {
			this.logError(log, e.getMessage());
		}
	}

	private void enableCommunication() {
		this.channel(OpenemsComponent.ChannelId.STATE).setNextValue(Level.OK);
		try {
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.W_SET_ENA))
					.setNextWriteValue(DefaultSunSpecModel.S704_WSetEna.ENABLED);
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.W_SET_MOD))
					.setNextWriteValue(DefaultSunSpecModel.S704_WSetMod.WATTS);
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.VAR_SET_ENA))
					.setNextWriteValue(DefaultSunSpecModel.S704_VarSetEna.ENABLED);
			((EnumWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.VAR_SET_MOD))
					.setNextWriteValue(DefaultSunSpecModel.S704_VarSetMod.VARS);
		} catch (OpenemsNamedException e) {
			this.logError(log, e.getMessage());
		}
	}

	private void recoverAlarm() throws OpenemsNamedException {
		if (Duration.between(Optional.ofNullable(this.lastAlarmTime).orElse(Instant.now()), Instant.now())
				.toSeconds() > Duration.ofSeconds(30).toSeconds()) {
			Status s = this.channel(EssSocomecPms.ChannelId.STATUS).getNextValue().asEnum();
			if (s.equals(Status.FAULT) && this.alarmCount < 3) {
				((IntegerWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S715.ALARM_RESET))
						.setNextWriteValue(1);
				this.lastAlarmTime = Instant.now();
				this.alarmCount++;
			} else if (s.equals(Status.FAULT) && this.alarmCount >= 3) {
				this.channel(OpenemsComponent.ChannelId.STATE).setNextValue(Level.FAULT);
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || !this.isSunSpecInitializationCompleted()) {
			return;
		}
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE)) {
			try {
				this.setHeartbeat();
				this.recoverAlarm();
			} catch (OpenemsNamedException e) {
				this.logError(log, e.getMessage());
			}
		}
	}

	private void setHeartbeat() throws OpenemsNamedException {
		Channel<Integer> h = this.channel(EssSocomecPms.ChannelId.HEARTBEAT);
		LongWriteChannel c = this.getSunSpecChannelOrError(DefaultSunSpecModel.S715.CONTROLLER_HB);
		c.setNextWriteValue((long) (h.value().orElse(0) + 1));
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
		((FloatWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.W_SET))
				.setNextWriteValue(Float.valueOf(activePower));
		((FloatWriteChannel) this.getSunSpecChannelOrError(DefaultSunSpecModel.S704.VAR_SET))
				.setNextWriteValue(Float.valueOf(reactivePower));
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsError.OpenemsNamedException {
		Constraint[] constraints = {
				this.createPowerConstraint("PMS not running", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
				this.createPowerConstraint("PMS not running", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) };
		Status status = this.channel(EssSocomecPms.ChannelId.STATUS).getNextValue().asEnum();
		return switch (status) {
		case THROTTLED, RUNNING -> Power.NO_CONSTRAINTS;
		default -> constraints;
		};
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
