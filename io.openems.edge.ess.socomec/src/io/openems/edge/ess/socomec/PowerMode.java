package io.openems.edge.ess.socomec;


import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;

public enum PowerMode implements OptionsEnum {
    PERCENTAGE(0, "Percentage"),
    WATTS(1, "Watts"),
    VARS(2, "Vars"),
    UNDEFINED(-1, "Undefined");
    private final int value;
    private final String name;

    PowerMode(int value, String name) {
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

    public static final ElementToChannelConverter ACTIVE_POWER_MODE_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (PERCENTAGE.equals(value)) return DefaultSunSpecModel.S704_WSetMod.W_MAX_PCT;
                if (WATTS.equals(value)) return DefaultSunSpecModel.S704_WSetMod.WATTS;
                return DefaultSunSpecModel.S704_WSetMod.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S704_WSetMod.W_MAX_PCT.equals(value)) return PERCENTAGE;
                if (DefaultSunSpecModel.S704_WSetMod.WATTS.equals(value)) return WATTS;
                return UNDEFINED;
            });

    public static final ElementToChannelConverter REACTIVE_POWER_MODE_CONVERTER = new ElementToChannelConverter(//
            // element -> channel
            value -> {
                if (PERCENTAGE.equals(value)) return DefaultSunSpecModel.S704_VarSetMod.VAR_MAX_PCT;
                if (VARS.equals(value)) return DefaultSunSpecModel.S704_VarSetMod.VARS;
                return DefaultSunSpecModel.S704_VarSetMod.UNDEFINED;
            }, //
            // channel -> element
            value -> {
                if (DefaultSunSpecModel.S704_VarSetMod.VAR_MAX_PCT.equals(value)) return PERCENTAGE;
                if (DefaultSunSpecModel.S704_VarSetMod.VARS.equals(value)) return VARS;
                return UNDEFINED;
            });
}