package org.tron.plugins;

import static org.tron.plugins.utils.DBUtils.CHECKPOINT_DB_V2;
import static org.tron.plugins.utils.DBUtils.FILE_ENGINE;
import static org.tron.plugins.utils.DBUtils.KEY_ENGINE;
import static org.tron.plugins.utils.DBUtils.Operator;
import static org.tron.plugins.utils.DBUtils.ROCKSDB;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Strings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rocksdb.RocksDBException;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Pair;
import org.tron.common.utils.PublicMethod;
import org.tron.common.utils.Utils;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.plugins.utils.FileUtils;
import org.tron.plugins.utils.WrappedByteArray;
import org.tron.plugins.utils.db.DBInterface;
import org.tron.plugins.utils.db.DbTool;
import picocli.CommandLine;

@Slf4j
public class DbLiteTest {

  private TronApplicationContext context;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private ManagedChannel channelFull;
  private Application appTest;
  private String databaseDir;

  private static final String MOCK_DB_NAME = "account";
  // checkpoint 1
  private static final List<Pair<String, String>> entry1 = Lists.newArrayList(
      new Pair<>("key1", "value1"),
      new Pair<>("common_key2", "value2"),
      new Pair<>("common_key3", "value3")
  );
  // checkpoint 2
  private static final List<Pair<String, String>> entry2 = Lists.newArrayList(
      new Pair<>("key0", "value0"),
      new Pair<>("common_key2", "value2 in entry2"),
      new Pair<>("common_key3", "value3")
  );
  // checkpoint 3
  private static final List<Pair<String, String>> entry3 = Lists.newArrayList(
      new Pair<>("common_key2", "value2"),
      new Pair<>("common_key3", "value2 in entry3"),
      new Pair<>("common_key4", "value4")
  );
  // checkpoint 4
  private static final List<Pair<String, String>> entry4 = Lists.newArrayList(
      new Pair<>("key7", "value7"),
      new Pair<>("key123", "value123"),
      new Pair<>("common_key3", "value3 in entry4"),
      new Pair<>("common_key4", "value4 in entry4"),
      new Pair<>("key5", "value5")
  );
  // flat checkpoint
  private static final List<Pair<String, String>> flatEntrys = Lists.newArrayList(
      new Pair<>("key0", "value0"),
      new Pair<>("key1", "value1"),
      new Pair<>("common_key2", "value2"),
      new Pair<>("common_key3", "value3 in entry4"),
      new Pair<>("common_key4", "value4 in entry4"),
      new Pair<>("key5", "value5"),
      new Pair<>("key7", "value7"),
      new Pair<>("key123", "value123")
  );
  // account
  private static final Map<WrappedByteArray, WrappedByteArray> accountEntrys =
      new HashMap<WrappedByteArray, WrappedByteArray>() {
    {
      put(WrappedByteArray.of("key0".getBytes()),
          WrappedByteArray.of("value0 in account".getBytes()));
      put(WrappedByteArray.of("key5_account".getBytes()),
          WrappedByteArray.of("value5 in account".getBytes()));
      put(WrappedByteArray.of("key_ingnore".getBytes()),
          WrappedByteArray.of("value ingnore".getBytes()));
    }
  };

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private String dbPath;
  CommandLine cli = new CommandLine(new DbLite());

  /**
   * init logic.
   */
  public void startApp() {
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(RpcApiService.class));
    appTest.addService(context.getBean(RpcApiServiceOnSolidity.class));
    appTest.startup();

    String fullNode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    channelFull = ManagedChannelBuilder.forTarget(fullNode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  /**
   * shutdown the fullNode.
   */
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    context.close();
  }

  public void init() throws IOException {
    dbPath = folder.newFolder().toString();
    Args.setParam(new String[]{"-d", dbPath, "-w", "--p2p-disable", "true"},
        "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    Args.getInstance().setRpcPort(PublicMethod.chooseRandomPort());
    databaseDir = Args.getInstance().getStorage().getDbDirectory();
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
  }

  @After
  public void clear() {
    Args.clearParam();
  }

  void testTools(String dbType, int checkpointVersion)
      throws InterruptedException, IOException {
    logger.info("dbType {}, checkpointVersion {}", dbType, checkpointVersion);
    dbPath = String.format("%s_%s_%d", dbPath, dbType, System.currentTimeMillis());
    init();
    final String[] argsForSnapshot =
        new String[]{"-o", "split", "-t", "snapshot", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path",
            dbPath};
    final String[] argsForHistory =
        new String[]{"-o", "split", "-t", "history", "--fn-data-path",
            dbPath + File.separator + databaseDir, "--dataset-path",
            dbPath};
    final String[] argsForMerge =
        new String[]{"-o", "merge", "--fn-data-path", dbPath + File.separator + databaseDir,
            "--dataset-path", dbPath + File.separator + "history"};
    Args.getInstance().getStorage().setDbEngine(dbType);
    Args.getInstance().getStorage().setCheckpointVersion(checkpointVersion);
    DbLite.setRecentBlks(3);
    // start fullNode
    startApp();
    // produce transactions for 18 seconds
    generateSomeTransactions(18);
    // stop the node
    shutdown();
    // delete tran-cache
    FileUtil.deleteDir(Paths.get(dbPath, databaseDir, "trans-cache").toFile());
    // generate snapshot
    cli.execute(argsForSnapshot);
    // start fullNode
    startApp();
    // produce transactions
    generateSomeTransactions(checkpointVersion == 1 ? 6 : 18);
    // stop the node
    shutdown();
    // generate history
    cli.execute(argsForHistory);
    // backup original database to database_bak
    File database = new File(Paths.get(dbPath, databaseDir).toString());
    if (!database.renameTo(new File(Paths.get(dbPath, databaseDir + "_bak").toString()))) {
      throw new RuntimeException(
              String.format("rename %s to %s failed", database.getPath(),
                  Paths.get(dbPath, databaseDir)));
    }
    // change snapshot to the new database
    File snapshot = new File(Paths.get(dbPath, "snapshot").toString());
    if (!snapshot.renameTo(new File(Paths.get(dbPath, databaseDir).toString()))) {
      throw new RuntimeException(
              String.format("rename snapshot to %s failed",
                  Paths.get(dbPath, databaseDir)));
    }
    // start and validate the snapshot
    startApp();
    generateSomeTransactions(checkpointVersion == 1 ? 18 : 6);
    // stop the node
    shutdown();
    // merge history
    cli.execute(argsForMerge);
    // start and validate
    startApp();
    generateSomeTransactions(6);
    shutdown();
    DbLite.reSetRecentBlks();
  }

  private void generateSomeTransactions(int during) {
    during *= 1000; // ms
    int runTime = 0;
    int sleepOnce = 100;
    while (true) {
      ECKey ecKey2 = new ECKey(Utils.getRandom());
      byte[] address = ecKey2.getAddress();

      String sunPri = "cba92a516ea09f620a16ff7ee95ce0df1d56550a8babe9964981a7144c8a784a";
      byte[] sunAddress = PublicMethod.getFinalAddress(sunPri);
      PublicMethod.sendcoin(address, 1L,
              sunAddress, sunPri, blockingStubFull);
      try {
        Thread.sleep(sleepOnce);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if ((runTime += sleepOnce) > during) {
        return;
      }
    }
  }

  private String mockDBWithCheckpointV2(String dbType)
      throws IOException, RocksDBException {
    String parentPath = folder.newFolder().toString();
    String cpPath = Paths.get(parentPath, CHECKPOINT_DB_V2).toString();
    FileUtils.createDirIfNotExists(cpPath);

    List<List<Pair<String, String>>> allEntrys = Lists.newArrayList(
        entry1, entry2, entry3, entry4);
    long now = System.currentTimeMillis();
    for (List<Pair<String, String>> entry: allEntrys) {
      if ("ROCKSDB".equals(dbType)) {
        String dbPath = Paths.get(cpPath, String.valueOf(now)).toString();
        String propertyPath = Paths.get(dbPath, FILE_ENGINE).toString();
        FileUtils.createDirIfNotExists(dbPath);
        FileUtils.createFileIfNotExists(propertyPath);
        FileUtils.writeProperty(propertyPath, KEY_ENGINE, ROCKSDB);
      }
      DBInterface dbInterface = DbTool.getDB(cpPath, String.valueOf(now));
      entry.forEach((item) -> {
        dbInterface.put(
            Bytes.concat(DbLite.simpleEncode(MOCK_DB_NAME), item.getKey().getBytes()),
            Bytes.concat(new byte[]{Operator.PUT.getValue()}, item.getValue().getBytes()));
      });
      now += 3000;
    }
    DBInterface dbInterface = DbTool.getDB(parentPath, MOCK_DB_NAME);
    accountEntrys.forEach(
        (key, value) -> dbInterface.put(key.getBytes(), value.getBytes()));
    DbTool.close();
    return parentPath;
  }

  public void testInitFlatCheckpointV2(String dbType) {
    DbLite dbLite = new DbLite();
    String path;
    try {
      path = mockDBWithCheckpointV2(dbType);
      dbLite.initFlatCheckpointV2(path);
    } catch (IOException | RocksDBException e) {
      Assert.fail();
    }
    Assert.assertEquals(flatEntrys.size(), DbLite.checkpointV2FlatMap.size());
    for (Pair<String, String> entry: flatEntrys) {
      Assert.assertEquals(
          Strings.fromByteArray(
              Bytes.concat(new byte[]{Operator.PUT.getValue()},
                  entry.getValue().getBytes())),
          Strings.fromByteArray(
              DbLite.checkpointV2FlatMap.get(
                  WrappedByteArray.of(
                      Bytes.concat(DbLite.simpleEncode(MOCK_DB_NAME),
                          entry.getKey().getBytes())))));
    }
    DbTool.close();
  }

  @Test
  public void testGetDataFromSourceDB() {
    WrappedByteArray key0 = WrappedByteArray.of("key0".getBytes());
    WrappedByteArray key5 = WrappedByteArray.of("key5".getBytes());
    WrappedByteArray key5a = WrappedByteArray.of("key5_account".getBytes());

    DbLite dbLite = new DbLite();
    String path;
    try {
      path = mockDBWithCheckpointV2("leveldb");
      dbLite.initFlatCheckpointV2(path);
      // data exists in database and checkpoint
      Assert.assertEquals(WrappedByteArray.of("value0".getBytes()),
          WrappedByteArray.of(
              dbLite.getDataFromSourceDB(path, MOCK_DB_NAME, key0.getBytes())));
      // data only exists in checkpoint
      Assert.assertEquals(WrappedByteArray.of("value5".getBytes()),
          WrappedByteArray.of(
              dbLite.getDataFromSourceDB(path, MOCK_DB_NAME, key5.getBytes())));
      // data only exists in database
      Assert.assertEquals(WrappedByteArray.of("value5 in account".getBytes()),
          WrappedByteArray.of(
              dbLite.getDataFromSourceDB(path, MOCK_DB_NAME, key5a.getBytes())));
    } catch (IOException | RocksDBException e) {
      Assert.fail();
    }
    DbTool.close();
  }
}
