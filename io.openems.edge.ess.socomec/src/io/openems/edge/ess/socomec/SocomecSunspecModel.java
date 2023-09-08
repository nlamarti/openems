package io.openems.edge.ess.socomec;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModelType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;

public enum SocomecSunspecModel implements SunSpecModel {
  S_64901(
      "Socomec PMS ",
      "Vendor model providing PMS states, warnings and alarms details ",
      "",
      66,
      SocomecSunspecModel.S64901.values(),
      SunSpecModelType.VENDOR_SPECIFIC //
  );

  public enum S64901 implements SunSpecPoint {
    SW1(new PointImpl(
        "S64901_SW1",
        "States Word 1",
        "States Word 1",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        S64901_SW1.values())),
    SW2(new PointImpl(
        "S64901_SW2",
        "States Word 2",
        "States Word 2",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        S64901_SW2.values())),
    SW3(new PointImpl(
        "S64901_SW3",
        "States Word 3",
        "States Word 3",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    SW4(new PointImpl(
        "S64901_SW4",
        "States Word 4",
        "States Word 4",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    SW5(new PointImpl(
        "S64901_SW5",
        "States Word 5",
        "States Word 5",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    SW6(new PointImpl(
        "S64901_SW6",
        "States Word 6",
        "States Word 6",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    SW7(new PointImpl(
        "S64901_SW7",
        "States Word 7",
        "States Word 7",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW8(new PointImpl(
        "S64901_SW8",
        "States Word 8",
        "States Word 8",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW9(new PointImpl(
        "S64901_SW9",
        "States Word 9",
        "States Word 9",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW10(new PointImpl(
        "S64901_SW10",
        "States Word 10",
        "States Word 10",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW11(new PointImpl(
        "S64901_SW2",
        "States Word 11",
        "States Word 11",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW12(new PointImpl(
        "S64901_SW12",
        "States Word 12",
        "States Word 12",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW13(new PointImpl(
        "S64901_SW13",
        "States Word 13",
        "States Word 13",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),

    // Empty
    SW14(new PointImpl(
        "S64901_SW14",
        "States Word 14",
        "States Word 14",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW15(new PointImpl(
        "S64901_SW15",
        "States Word 15",
        "States Word 15",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    SW16(new PointImpl(
        "S64901_SW16",
        "States Word 16",
        "States Word 16",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW1(new PointImpl(
        "S64901_WW1",
        "Warnings Word 1",
        "Warnings Word 1",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW2(new PointImpl(
        "S64901_WW2",
        "Warnings Word 2",
        "Warnings Word 2",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW3(new PointImpl(
        "S64901_WW3",
        "Warnings Word 3",
        "Warnings Word 3",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW4(new PointImpl(
        "S64901_WW4",
        "Warnings Word 4",
        "Warnings Word 4",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW5(new PointImpl(
        "S64901_WW5",
        "Warnings Word 5",
        "Warnings Word 5",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW6(new PointImpl(
        "S64901_WW6",
        "Warnings Word 6",
        "Warnings Word 6",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW7(new PointImpl(
        "S64901_WW7",
        "Warnings Word 7",
        "Warnings Word 7",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    WW8(new PointImpl(
        "S64901_WW8",
        "Warnings Word 8",
        "Warnings Word 8",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW9(new PointImpl(
        "S64901_WW9",
        "Warnings Word 9",
        "Warnings Word 9",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW10(new PointImpl(
        "S64901_WW10",
        "Warnings Word 10",
        "Warnings Word 10",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW11(new PointImpl(
        "S64901_WW2",
        "Warnings Word 11",
        "Warnings Word 11",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW12(new PointImpl(
        "S64901_WW12",
        "Warnings Word 12",
        "Warnings Word 12",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW13(new PointImpl(
        "S64901_WW13",
        "Warnings Word 13",
        "Warnings Word 13",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),

    // Empty
    WW14(new PointImpl(
        "S64901_WW14",
        "Warnings Word 14",
        "Warnings Word 14",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW15(new PointImpl(
        "S64901_WW15",
        "Warnings Word 15",
        "Warnings Word 15",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    WW16(new PointImpl(
        "S64901_WW16",
        "Warnings Word 16",
        "Warnings Word 16",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW1(new PointImpl(
        "S64901_AW1",
        "Alarms Word 1",
        "Alarms Word 1",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW2(new PointImpl(
        "S64901_AW2",
        "Alarms Word 2",
        "Alarms Word 2",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW3(new PointImpl(
        "S64901_AW3",
        "Alarms Word 3",
        "Alarms Word 3",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW4(new PointImpl(
        "S64901_AW4",
        "Alarms Word 4",
        "Alarms Word 4",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW5(new PointImpl(
        "S64901_AW5",
        "Alarms Word 5",
        "Alarms Word 5",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW6(new PointImpl(
        "S64901_AW6",
        "Alarms Word 6",
        "Alarms Word 6",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW7(new PointImpl(
        "S64901_AW7",
        "Alarms Word 7",
        "Alarms Word 7",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    AW8(new PointImpl(
        "S64901_AW8",
        "Alarms Word 8",
        "Alarms Word 8",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW9(new PointImpl(
        "S64901_AW9",
        "Alarms Word 9",
        "Alarms Word 9",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW10(new PointImpl(
        "S64901_AW10",
        "Alarms Word 10",
        "Alarms Word 10",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW11(new PointImpl(
        "S64901_AW2",
        "Alarms Word 11",
        "Alarms Word 11",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW12(new PointImpl(
        "S64901_AW12",
        "Alarms Word 12",
        "Alarms Word 12",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW13(new PointImpl(
        "S64901_AW13",
        "Alarms Word 13",
        "Alarms Word 13",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),

    // Empty
    AW14(new PointImpl(
        "S64901_AW14",
        "Alarms Word 14",
        "Alarms Word 14",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW15(new PointImpl(
        "S64901_AW15",
        "Alarms Word 15",
        "Alarms Word 15",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0])),
    // Empty
    AW16(new PointImpl(
        "S64901_AW16",
        "Alarms Word 16",
        "Alarms Word 16",
        "",
        PointType.BITFIELD16,
        false,
        AccessMode.READ_ONLY,
        Unit.NONE,
		null,
        new OptionsEnum[0]));

    private final PointImpl impl;

    S64901(PointImpl impl) {
      this.impl = impl;
    }

    @Override
    public PointImpl get() {
      return this.impl;
    }
  }

  //CHECKSTYLE:OFF
  public enum S64901_SW1 implements OptionsEnum {
    //CHECKSTYLE:ON
    UNDEFINED(-1, "Undefined"),
    BATTERY_ON_COMMAND(0, "BATTERY_ON_COMMAND"),
    INVERTER_ON_COMMAND(1, "INVERTER_ON_COMMAND"),
    BATTERY_READY(4, "BATTERY_READY"),
    INVERTER_READY(5, "INVERTER_READY"),
    SLEEP_ACTIVE(6, "SLEEP_ACTIVE"),
    ALARM_STATE(7, "ALARM_STATE"),
    IDLE_STATE(8, "IDLE_STATE"),
    CHARGING(9, "CHARGING"),
    DISCHARGING(10, "DISCHARGING"),
    GENERAL_ALARM(12, "GENERAL_ALARM"),
    GENERAL_WARNING(13, "GENERAL_WARNING"),
    BATTERY_CAN_BE_CHARGED(14, "BATTERY_CAN_BE_CHARGED"),
    BATTERY_CAN_BE_DISCHARGED(15, "BATTERY_CAN_BE_DISCHARGED");

    private final int value;
    private final String name;

    S64901_SW1(int value, String name) {
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
  }

  //CHECKSTYLE:OFF
  public enum S64901_SW2 implements OptionsEnum {
    //CHECKSTYLE:ON
    UNDEFINED(-1, "Undefined"),
    ON_GRID_MODE(1, "ON_GRID_MODE"),
    OFF_GRID_MODE(2, "OFF_GRID_MODE"),
    FORCE_ISLANDING_COMMAND(3, "FORCE_ISLANDING_COMMAND"),
    UN_PROGRAMMED_ISLANDING_COMMAND(4, "UN_PROGRAMMED_ISLANDING_COMMAND"),
    BLACK_START_ENABLED_COMMAND(5, "BLACK_START_ENABLED_COMMAND"),
    BLACK_START_ENDED(6, "BLACK_START_ENDED"),
    ZERO_POWER_IN_PROGRESS(8, "ZERO_POWER_IN_PROGRESS"),
    PROGRAMMED_ISLANDING(9, "PROGRAMMED_ISLANDING"),
    UN_PROGRAMMED_ISLANDING(10, "UN_PROGRAMMED_ISLANDING"),
    BLACK_START_IN_PROGRESS(11, "BLACK_START_IN_PROGRESS"),
    SYNCHRONIZATION_IN_PROGRESS(12, "SYNCHRONIZATION_IN_PROGRESS"),
    PCS_DRYING_IN_PROGRESS(14, "PCS_DRYING_IN_PROGRESS"),
    LOCAL_MODE_ACTIVE(15, "LOCAL_MODE_ACTIVE");

    private final int value;
    private final String name;

    S64901_SW2(int value, String name) {
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
  }

  public final String label;
  public final String description;
  public final String notes;
  public final int length;
  public final SunSpecPoint[] points;
  public final SunSpecModelType modelType;

  SocomecSunspecModel(String label, String description, String notes, int length, SunSpecPoint[] points,
                      SunSpecModelType modelType) {
    this.label = label;
    this.description = description;
    this.notes = notes;
    this.length = length;
    this.points = points;
    this.modelType = modelType;
  }

  @Override
  public SunSpecPoint[] points() {
    return this.points;
  }

  @Override
  public String label() {
    return this.label;
  }
}
