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
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Power;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EssSocomesPMSImpl extends AbstractOpenemsSunSpecComponent implements EssSocomecPMS {

    public static final ElementToChannelConverter GRID_MODE_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (GridMode.ON_GRID.equals(value)) return DefaultSunSpecModel.S701_ConnSt.CONNECTED;
                if (GridMode.OFF_GRID.equals(value)) return DefaultSunSpecModel.S701_ConnSt.DISCONNECTED;
                return DefaultSunSpecModel.S701_ConnSt.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S701_ConnSt.CONNECTED.equals(value)) return GridMode.ON_GRID;
                if (DefaultSunSpecModel.S701_ConnSt.DISCONNECTED.equals(value)) return GridMode.OFF_GRID;
                return GridMode.UNDEFINED;
            });
    private final Logger log = LoggerFactory.getLogger(EssSocomesPMSImpl.class);
    private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
            .put(DefaultSunSpecModel.S_1, Priority.LOW)
            .put(DefaultSunSpecModel.S_701, Priority.LOW)
            .put(DefaultSunSpecModel.S_702, Priority.LOW)
            .put(DefaultSunSpecModel.S_704, Priority.HIGH)
            .put(DefaultSunSpecModel.S_713, Priority.HIGH)
            .put(DefaultSunSpecModel.S_802, Priority.LOW)
            .put(DefaultSunSpecModel.S_715, Priority.HIGH)
            .build();
    // Further available SunSpec blocks provided by Socomec PMS are:
    // .put(DefaultSunSpecModel.S_703, Priority.LOW)
    // .put(DefaultSunSpecModel.S_705, Priority.LOW)
    // .put(DefaultSunSpecModel.S_706, Priority.LOW)
    // .put(DefaultSunSpecModel.S_802, Priority.LOW)
    // .put(DefaultSunSpecModel.S_803, Priority.LOW)
    // 64901 Socomec specific
    // 64902 Socomec specific

    /**
     * Constructs a AbstractOpenemsSunSpecComponent.
     *
     * @throws OpenemsException on error
     */
    public EssSocomesPMSImpl() throws OpenemsException {
        super(ACTIVE_MODELS, //
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                ManagedSymmetricEss.ChannelId.values(), //
                SymmetricEss.ChannelId.values(),
                EssSocomecPMS.ChannelId.values());
    }


    @Override
    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId, ConfigurationAdmin cm, String modbusReference, String modbusId) {
        return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);
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

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.ACTIVE_POWER_CONTROL,
                Control.ACTIVE_POWER_CONTROL_CONVERTER,
                DefaultSunSpecModel.S704.W_SET_ENA);

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.SET_ACTIVE_POWER,
                ElementToChannelConverter.DIRECT_1_TO_1,
                DefaultSunSpecModel.S704.W_SET);

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.ACTIVE_POWER_MODE,
                PowerMode.ACTIVE_POWER_MODE_CONVERTER,
                DefaultSunSpecModel.S704.W_SET_MOD);

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.REACTIVE_POWER_CONTROL,
                Control.REACTIVE_POWER_CONTROL_CONVERTER,
                DefaultSunSpecModel.S704.VAR_SET_ENA);

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.SET_REACTIVE_POWER,
                ElementToChannelConverter.DIRECT_1_TO_1,
                DefaultSunSpecModel.S704.VAR_SET);

        this.mapFirstPointToChannel(EssSocomecPMS.ChannelId.REACTIVE_POWER_MODE,
                PowerMode.REACTIVE_POWER_MODE_CONVERTER,
                DefaultSunSpecModel.S704.VAR_SET_MOD);

    }

    // TODO
    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
        return null;
    }

    // TODO
    @Override
    public Power getPower() {
        return null;
    }

    @Override
    public void applyPower(int activePower, int reactivePower) throws OpenemsError.OpenemsNamedException {
        IntegerWriteChannel setActivePowerChannel = this.channel(EssSocomecPMS.ChannelId.SET_ACTIVE_POWER);
        IntegerWriteChannel setReactivePowerChannel = this.channel(EssSocomecPMS.ChannelId.SET_REACTIVE_POWER);

        setActivePowerChannel.setNextWriteValue(activePower);
        setReactivePowerChannel.setNextWriteValue(reactivePower);
    }

    @Override
    public int getPowerPrecision() {
        return 1;
    }

    // TODO: Add constraint by checking state of the ESS
    @Override
    public Constraint[] getStaticConstraints() throws OpenemsError.OpenemsNamedException {
        return EssSocomecPMS.super.getStaticConstraints();
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
