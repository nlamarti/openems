/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.thermalpowerstation;

import java.util.List;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

public class ThermalPowerStationController extends Controller {

	@ConfigInfo(title = "The ess where the soc should be read from.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "The meters of power producers for excample PV.", type = Meter.class)
	public ConfigChannel<List<Meter>> meters = new ConfigChannel<>("meters", this);

	@ConfigInfo(title = "if the soc falls below this value and power production is below the productionLimit thermalpowerstation will start", type = Long.class)
	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this);
	@ConfigInfo(title = "if the soc rise above maxSoc the thermalpowerstation will stop", type = Long.class)
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);
	@ConfigInfo(title = "true if the digital output should be inverted.", type = Boolean.class)
	public ConfigChannel<Boolean> invertOutput = new ConfigChannel<>("invertOutput", this);
	@ConfigInfo(title = "if the soc falls below this value and power production is below the productionLimit thermalpowerstation will start", type = Long.class)
	public ConfigChannel<Long> productionLimit = new ConfigChannel<>("productionLimit", this);
	@ConfigInfo(title = "indicates how long the production power must be below the productionLimit to start the powerstation. Time in minutes.", type = Long.class)
	public ConfigChannel<Long> limitTimeRange = new ConfigChannel<>("limitTimeRange", this);

	private ThingRepository repo = ThingRepository.getInstance();
	private Long lastTimeBelowProductionlimit = System.currentTimeMillis();

	@SuppressWarnings("unchecked")
	@ConfigInfo(title = "the address of the Digital Output where the generator is connected to.", type = String.class)
	public ConfigChannel<String> outputChannelAddress = new ConfigChannel<String>("outputChannelAddress", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						outputChannel = (WriteChannel<Boolean>) ch.get();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'outputChannelAddress' is not configured!");
				}
			});

	private WriteChannel<Boolean> outputChannel;

	private boolean outputOn = true;

	public ThermalPowerStationController() {
		super();
	}

	public ThermalPowerStationController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			if (getProductionPower() <= productionLimit.value()) {
				lastTimeBelowProductionlimit = System.currentTimeMillis();
			}
			if (!outputOn && ess.value().soc.value() <= minSoc.value()
					&& getProductionPower() <= productionLimit.value()) {
				// switch generator on
				startGenerator();
				outputOn = true;
			} else if (outputOn && (ess.value().soc.value() >= maxSoc.value() || lastTimeBelowProductionlimit
					+ limitTimeRange.value() * 60 * 1000 <= System.currentTimeMillis())) {
				// switch generator off
				stopGenerator();
				outputOn = false;
			} else if (outputOn) {
				startGenerator();
			} else if (!outputOn) {
				stopGenerator();
			}
		} catch (InvalidValueException e) {
			log.error("Failed to read value!", e);
		} catch (WriteChannelException e) {
			log.error("Error due write to output [" + outputChannelAddress.valueOptional().orElse("<none>") + "]", e);
		}
	}

	private void startGenerator() throws WriteChannelException, InvalidValueException {
		if (outputChannel.value() != true ^ invertOutput.value()) {
			outputChannel.pushWrite(true ^ invertOutput.value());
		}
	}

	private void stopGenerator() throws InvalidValueException, WriteChannelException {
		if (outputChannel.value() != false ^ invertOutput.value()) {
			outputChannel.pushWrite(false ^ invertOutput.value());
		}
	}

	private Long getProductionPower() throws InvalidValueException {
		Long power = 0L;
		for (Meter m : meters.value()) {
			power += m.power.value();
		}
		return power;
	}

}
