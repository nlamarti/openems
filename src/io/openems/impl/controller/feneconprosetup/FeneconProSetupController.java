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
package io.openems.impl.controller.feneconprosetup;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class FeneconProSetupController extends Controller {

	@ConfigInfo(title = "Storages of type FeneconPro to run initial setup commands", type = Ess.class)
	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this);

	public FeneconProSetupController() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FeneconProSetupController(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				if (ess.pcsMode.labelOptional().isPresent() && !ess.pcsMode.labelOptional().get().equals("Remote")) {
					if (ess.setupMode.labelOptional().isPresent()
							&& ess.setupMode.labelOptional().get().equals(EssNature.ON)) {
						ess.setPcsMode.pushWriteFromLabel("Remote");
						log.info("Set " + ess.id() + " to Remote mode.");
					} else {
						log.info(ess.id() + " is not in Remote mode. Go to Setting Mode.");
						ess.setSetupMode.pushWriteFromLabel(EssNature.ON);
					}
				} else {
					if (ess.setupMode.labelOptional().isPresent()
							&& ess.setupMode.labelOptional().get().equals(EssNature.ON)) {
						ess.setSetupMode.pushWriteFromLabel(EssNature.OFF);
						log.info(ess.id() + " Switch setting mode Off");
					}
				}
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error("Failed to Finish Setup", e);
		}
	}

}
