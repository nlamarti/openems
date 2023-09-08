package io.openems.edge.ess.socomec;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;

public enum Status implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	SLEEPING(1, "Sleeping"), //
	STARTING(2, "Starting"), //
	RUNNING(3, "Running"), //
	THROTTLED(4, "Throttled"), //
	SHUTTING_DOWN(5, "Shutting down"), //
	FAULT(6, "Fault"), //
	STANDBY(7, "Standby"); //

	private final int value;
	private final String name;

	Status(int value, String name) {
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

	public static final ElementToChannelConverter STATUS_CONVERTER = new ElementToChannelConverter(//
			// element -> channel
			value -> {
				int s = (int) value;
				if (DefaultSunSpecModel.S701_InvSt.OFF.getValue() == s) {
					return OFF;
				}
				if (DefaultSunSpecModel.S701_InvSt.SLEEPING.getValue() == s) {
					return SLEEPING;
				}
				if (DefaultSunSpecModel.S701_InvSt.STARTING.getValue() == s) {
					return STARTING;
				}
				if (DefaultSunSpecModel.S701_InvSt.RUNNING.getValue() == s) {
					return RUNNING;
				}
				if (DefaultSunSpecModel.S701_InvSt.THROTTLED.getValue() == s) {
					return THROTTLED;
				}
				if (DefaultSunSpecModel.S701_InvSt.SHUTTING_DOWN.getValue() == s) {
					return SHUTTING_DOWN;
				}
				if (DefaultSunSpecModel.S701_InvSt.FAULT.getValue() == s) {
					return FAULT;
				}
				if (DefaultSunSpecModel.S701_InvSt.STANDBY.getValue() == s) {
					return STANDBY;
				}
				return UNDEFINED;
			},
			// channel -> element
			value -> {
				Status s = (Status) value;
				if (OFF.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.OFF;
				}
				if (SLEEPING.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.SLEEPING;
				}
				if (STARTING.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.STARTING;
				}
				if (RUNNING.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.RUNNING;
				}
				if (THROTTLED.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.THROTTLED;
				}
				if (SHUTTING_DOWN.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.SHUTTING_DOWN;
				}
				if (FAULT.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.FAULT;
				}
				if (STANDBY.equals(s)) {
					return DefaultSunSpecModel.S701_InvSt.STANDBY;
				}
				return DefaultSunSpecModel.S701_InvSt.UNDEFINED;
			});
}
