package org.tron.plugins;

import java.io.IOException;
import org.junit.Test;

public class DbLiteRocksDbTest extends DbLiteTest {

  @Test
  public void testToolsWithRocksDB() throws InterruptedException, IOException {
    testTools("ROCKSDB", 1);
  }

  @Test
  public void testInitFlatCheckpointV2WithRocksDB() {
    testInitFlatCheckpointV2("ROCKSDB");
  }
}
