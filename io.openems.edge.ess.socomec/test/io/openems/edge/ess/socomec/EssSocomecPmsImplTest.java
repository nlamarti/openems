package io.openems.edge.ess.socomec;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import org.junit.Test;

public class EssSocomecPmsImplTest {
  private static final String ESS_ID = "ess0";
  private static final String MODBUS_ID = "modbus0";

  @Test
  public void test() throws Exception {
    new ManagedSymmetricEssTest(new EssSocomecPmsImpl()).addReference("cm", new DummyConfigurationAdmin())
        .addReference("setModbus", new DummyModbusBridge(MODBUS_ID))
        .activate(MyConfig.create().setId(ESS_ID).setModbusId(MODBUS_ID).build());
  }
}
