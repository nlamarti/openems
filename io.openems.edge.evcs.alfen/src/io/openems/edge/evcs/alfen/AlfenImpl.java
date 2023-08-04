package io.openems.edge.evcs.alfen;

import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Alfen", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class AlfenImpl extends AbstractOpenemsModbusComponent
		implements Alfen, Evcs, ManagedEvcs, OpenemsComponent, ModbusComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AlfenImpl.class);
	private static final int DETECT_PHASE_ACTIVITY = 100; // W

	protected Config config;

	private Long energyAtSessionStart = 0L;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	private final WriteHandler writeHandler = new WriteHandler(this);

	public AlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Alfen.ChannelId.values());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);
		this.<Channel<Status>>channel(Evcs.ChannelId.STATUS).onChange((current, prev) -> {
			if ((current.orElse(Status.UNDEFINED).equals(Status.READY_FOR_CHARGING) || current.orElse(Status.UNDEFINED).equals(Status.CHARGING))
					&& !(prev.orElse(Status.UNDEFINED).equals(Status.READY_FOR_CHARGING) || prev.orElse(Status.UNDEFINED).equals(Status.CHARGING))) {
				this.energyAtSessionStart = this.getActiveConsumptionEnergy().get();
			}
		});

		this.applyConfig(context, config);
		this.getModbusCommunicationFailedChannel()
				.onSetNextValue(t -> this._setChargingstationCommunicationFailed(t.orElse(false)));
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	private void applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this._setFixedMinimumHardwarePower(config.minHwPower());
		this._setFixedMaximumHardwarePower(config.maxHwPower());
		this._setPowerPrecision(1);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateSessionEnergy();
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		ModbusProtocol modbusProtocol = new ModbusProtocol(this,

				new FC3ReadRegistersTask(300, Priority.HIGH,
						m(Alfen.ChannelId.METER_STATE, new UnsignedWordElement(300)),
						m(Alfen.ChannelId.METER_LAST_VALUE_TIMESTAMP, new UnsignedQuadruplewordElement(301)),
						m(Alfen.ChannelId.METER_TYPE, new UnsignedWordElement(305)),
						m(Alfen.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(306)),
						m(Alfen.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(308)),
						m(Alfen.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(310)),
						m(Alfen.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(312)),
						m(Alfen.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(314)),
						m(Alfen.ChannelId.VOLTAGE_L3_L1, new FloatDoublewordElement(316)),
						m(Alfen.ChannelId.CURRENT_N, new FloatDoublewordElement(318)),
						m(Alfen.ChannelId.CURRENT_L1, new FloatDoublewordElement(320)),
						m(Alfen.ChannelId.CURRENT_L2, new FloatDoublewordElement(322)),
						m(Alfen.ChannelId.CURRENT_L3, new FloatDoublewordElement(324)),
						m(Alfen.ChannelId.CURRENT, new FloatDoublewordElement(326)),
						m(Alfen.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(328)),
						m(Alfen.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(330)),
						m(Alfen.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(332)),
						m(Alfen.ChannelId.POWER_FACTOR_SUM, new FloatDoublewordElement(334)),
						m(Alfen.ChannelId.FREQUENCY, new FloatDoublewordElement(336)),
						m(Alfen.ChannelId.CHARGE_POWER_L1, new FloatDoublewordElement(338)),
						m(Alfen.ChannelId.CHARGE_POWER_L2, new FloatDoublewordElement(340)),
						m(Alfen.ChannelId.CHARGE_POWER_L3, new FloatDoublewordElement(342)),
						m(Evcs.ChannelId.CHARGE_POWER, new FloatDoublewordElement(344)),
						m(Alfen.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(346)),
						m(Alfen.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(348)),
						m(Alfen.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(350)),
						m(Alfen.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(352)),
						m(Alfen.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(354)),
						m(Alfen.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(356)),
						m(Alfen.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(358)),
						m(Alfen.ChannelId.REACTIVE_POWER_SUM, new FloatDoublewordElement(360))),

				new FC3ReadRegistersTask(374, Priority.LOW,
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(374))),

				new FC3ReadRegistersTask(1200, Priority.HIGH,
						m(Alfen.ChannelId.AVAILABILITY, new UnsignedWordElement(1200)),
						m(Alfen.ChannelId.MODE_3_STATE, new StringWordElement(1201, 5)),
						m(Alfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new FloatDoublewordElement(1206)),
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME, new UnsignedDoublewordElement(1208)),
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210)),
						m(Alfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT, new FloatDoublewordElement(1212)),
						m(Alfen.ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR, new UnsignedWordElement(1214)),
						m(Alfen.ChannelId.CHARGE_USING_1_OR_3_PHASES, new UnsignedWordElement(1215))),

				new FC16WriteRegistersTask(1210,
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210)))

		);
		this.addStatusListener();
		this.addPhasesListener();
		return modbusProtocol;

	}

	private void calculateSessionEnergy() {
		var sessionEnergy = (int) (this.getActiveConsumptionEnergy().orElse(0L) - this.energyAtSessionStart);
		this._setEnergySession(sessionEnergy);
	}

	private void addStatusListener() {
		this.<Channel<String>>channel(Alfen.ChannelId.MODE_3_STATE).onUpdate((val) -> {
			String mode3State = val.orElse(new String(""));
			switch (mode3State) {
			case "A":
			case "E":
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case "B1":
			case "B2":
				this._setStatus(Status.READY_FOR_CHARGING);
				break;
			case "C1":
			case "D1":
				this._setStatus(Status.CHARGING_FINISHED);
				break;
			case "C2":
			case "D2":
				this._setStatus(Status.CHARGING);
				break;
			case "F":
				this._setStatus(Status.ERROR);
				this.channel(Alfen.ChannelId.ERROR).setNextValue(Level.FAULT);
				break;
			default:
				this._setStatus(Status.UNDEFINED);
			}
		});
	}

	private void addPhasesListener() {
		final Consumer<Value<Float>> setPhases = ignore -> {
			var phases = 0;
			if (this.getPowerL1().orElse(0.0f) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (this.getPowerL2().orElse(0.0f) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (this.getPowerL3().orElse(0.0f) > DETECT_PHASE_ACTIVITY) {
				phases++;
			}
			if (phases == 0) {
				phases = 3;
			}
			this._setPhases(phases);
		};
		this.getPowerL1Channel().onUpdate(setPhases);
		this.getPowerL2Channel().onUpdate(setPhases);
		this.getPowerL3Channel().onUpdate(setPhases);
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		float current = this._toCurrent(power);
		this.logDebug("Setting current " + current + " from power " + power);
		this._setSlaveMaxCurrent(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

}