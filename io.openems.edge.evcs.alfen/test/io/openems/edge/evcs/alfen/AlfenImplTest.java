package io.openems.edge.evcs.alfen;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class AlfenImplTest {

	private static final String COMPONENT_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new AlfenImpl()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setMaxHwPower(70000).setMinHwPower(0).build());
	}

}
