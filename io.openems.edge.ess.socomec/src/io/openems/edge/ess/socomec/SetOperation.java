package io.openems.edge.ess.socomec;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;

public enum SetOperation implements OptionsEnum {
    STOP(0, "Stop"),
    START(1, "Start"),
    EXIT_STANDBY(3, "Exit Standby"),
    ENTER_STANDBY(2, "Enter Standby"),
    UNDEFINED(-1, "Undefined");

    private final int value;
    private final String name;
    SetOperation(int value, String name) {
        this.value = value;
        this.name = name;
    }

    @Override
    public int getValue() {
        return this.value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public OptionsEnum getUndefined() {
        return UNDEFINED;
    }

    public static final ElementToChannelConverter SET_OPERATION_CONTROL_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (START.equals(value)) return DefaultSunSpecModel.S715_OpCtl.START;
                if (STOP.equals(value)) return DefaultSunSpecModel.S715_OpCtl.STOP;
                if (ENTER_STANDBY.equals(value)) return DefaultSunSpecModel.S715_OpCtl.ENTER_STANDBY;
                if (EXIT_STANDBY.equals(value)) return DefaultSunSpecModel.S715_OpCtl.EXIT_STANDBY;
                return DefaultSunSpecModel.S715_OpCtl.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S715_OpCtl.START.equals(value)) return START;
                if (DefaultSunSpecModel.S715_OpCtl.STOP.equals(value)) return STOP;
                if (DefaultSunSpecModel.S715_OpCtl.ENTER_STANDBY.equals(value)) return  ENTER_STANDBY;
                if (DefaultSunSpecModel.S715_OpCtl.EXIT_STANDBY.equals(value)) return EXIT_STANDBY;
                return UNDEFINED;
            });
}
