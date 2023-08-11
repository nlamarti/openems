package io.openems.edge.ess.socomec;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface EssSocomecPms extends ManagedSymmetricEss, ModbusComponent, ModbusSlave {
  enum ChannelId implements io.openems.edge.common.channel.ChannelId {
    SET_OPERATION(Doc.of(SetOperation.values())
        .accessMode(AccessMode.READ_WRITE)),
    STATUS(Doc.of(Status.values())
        .accessMode(AccessMode.READ_WRITE)),
    ACTIVE_POWER_CONTROL(Doc.of(Control.values())
        .accessMode(AccessMode.READ_WRITE)),
    SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)
        .accessMode(AccessMode.READ_WRITE)
        .unit(Unit.WATT)),
    ACTIVE_POWER_MODE(Doc.of(PowerMode.values())
        .accessMode(AccessMode.READ_WRITE)),
    REACTIVE_POWER_CONTROL(Doc.of(Control.values())
        .accessMode(AccessMode.READ_WRITE)),
    SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)
        .accessMode(AccessMode.READ_WRITE)
        .unit(Unit.VOLT_AMPERE)),
    REACTIVE_POWER_MODE(Doc.of(PowerMode.values())
        .accessMode(AccessMode.READ_WRITE)),
    BATTERY_CONTROL(Doc.of(Control.values())
        .accessMode(AccessMode.READ_WRITE)),
    HEARTBEAT(Doc.of(OpenemsType.INTEGER)
        .accessMode(AccessMode.READ_ONLY)),
    SET_HEARTBEAT(Doc.of(OpenemsType.INTEGER)
        .accessMode(AccessMode.READ_WRITE));
    private final Doc doc;

    ChannelId(Doc doc) {
      this.doc = doc;
    }

    @Override
    public Doc doc() {
      return this.doc;
    }
  }
}
