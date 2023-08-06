package io.openems.edge.ess.socomec;


import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;

public enum Control implements OptionsEnum {
    OFF(0, "Off"),
    ON(1, "On"),
    UNDEFINED(-1, "Undefined");
    private final int value;
    private final String name;

    Control(int value, String name) {
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

    public static final ElementToChannelConverter ACTIVE_POWER_CONTROL_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (ON.equals(value)) return DefaultSunSpecModel.S704_WSetEna.ENABLED;
                if (OFF.equals(value)) return DefaultSunSpecModel.S704_WSetEna.DISABLED;
                return DefaultSunSpecModel.S704_WSetEna.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S704_WSetEna.ENABLED.equals(value)) return ON;
                if (DefaultSunSpecModel.S704_WSetEna.DISABLED.equals(value)) return OFF;
                return UNDEFINED;
            });

    public static final ElementToChannelConverter REACTIVE_POWER_CONTROL_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (ON.equals(value)) return DefaultSunSpecModel.S704_VarSetEna.ENABLED;
                if (OFF.equals(value)) return DefaultSunSpecModel.S704_VarSetEna.DISABLED;
                return DefaultSunSpecModel.S704_VarSetEna.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S704_VarSetEna.ENABLED.equals(value)) return ON;
                if (DefaultSunSpecModel.S704_VarSetEna.DISABLED.equals(value)) return OFF;
                return UNDEFINED;
            });


    public static final ElementToChannelConverter BATTERY_CONTROL_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (ON.equals(value)) return DefaultSunSpecModel.S802_SetOp.CONNECT;
                if (OFF.equals(value)) return DefaultSunSpecModel.S802_SetOp.DISCONNECT;
                return DefaultSunSpecModel.S802_SetOp.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S802_SetOp.CONNECT.equals(value)) return ON;
                if (DefaultSunSpecModel.S802_SetOp.DISCONNECT.equals(value)) return OFF;
                return UNDEFINED;
            });
}
