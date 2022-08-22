package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.prometheus.client.Histogram;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.common.error.TronDBException;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.prometheus.MetricKeys;
import org.tron.common.prometheus.Metrics;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.db.RevokingDatabase;
import org.tron.core.db2.ISession;
import org.tron.core.db2.common.DB;
import org.tron.core.db2.common.IRevokingDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;
import org.tron.core.db2.common.WrappedByteArray;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.exception.RevokingStoreIllegalStateException;
import org.tron.core.store.CheckPointV2Store;
import org.tron.core.store.CheckTmpStore;
import org.tron.protos.tron.CheckpointOuterClass;

@Slf4j(topic = "DB")
public class SnapshotManager implements RevokingDatabase {

  public static final int DEFAULT_MAX_FLUSH_COUNT = 200;
  public static final int DEFAULT_MIN_FLUSH_COUNT = 1;
  private static final int DEFAULT_STACK_MAX_SIZE = 256;
  private static final long ONE_MINUTE_MILLS = 60*1000L;
  @Getter
  private List<Chainbase> dbs = new ArrayList<>();
  @Getter
  private int size = 0;
  private AtomicInteger maxSize = new AtomicInteger(DEFAULT_STACK_MAX_SIZE);

  private boolean disabled = true;
  // for test
  @Getter
  private int activeSession = 0;
  // for test
  @Setter
  private boolean unChecked = true;

  private volatile int flushCount = 0;

  private Thread exitThread;
  private volatile boolean  hitDown;

  private Map<String, ListeningExecutorService> flushServices = new HashMap<>();

  private ScheduledExecutorService pruneCheckpointThread = Executors.newSingleThreadScheduledExecutor();;

  @Autowired
  @Setter
  @Getter
  private CheckTmpStore checkTmpStore;

  @Autowired
  @Setter
  @Getter
  private CheckPointV2Store checkPointV2Store;

  @Setter
  private volatile int maxFlushCount = DEFAULT_MIN_FLUSH_COUNT;

  private long currentBlockNum = -1;

  private int checkpointVersion = 2;   // default v2

  public SnapshotManager(String checkpointPath) {
  }

  @PostConstruct
  public void init() {
    checkpointVersion = CommonParameter.getInstance().getStorage().getCheckpointVersion();
    // prune checkpoint
    pruneCheckpointThread.scheduleWithFixedDelay(() -> {
      try {
        if (isV2Open() && !unChecked) {
          pruneCheckpoint();
        }
      } catch (Throwable t) {
        logger.error("Exception in prune checkpoint", t);
      }
    }, 300000, 3600, TimeUnit.MILLISECONDS);


    exitThread =  new Thread(() -> {
      LockSupport.park();
      // to Guarantee Some other thread invokes unpark with the current thread as the target
      if (hitDown) {
        System.exit(1);
      }
    });
    exitThread.setName("exit-thread");
    exitThread.start();
  }

  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

  public ISession buildSession() {
    return buildSession(false);
  }

  public synchronized ISession buildSession(boolean forceEnable) {
    if (disabled && !forceEnable) {
      return new Session(this);
    }

    boolean disableOnExit = disabled && forceEnable;
    if (forceEnable) {
      disabled = false;
    }

    if (size > maxSize.get() && !hitDown) {
      flushCount = flushCount + (size - maxSize.get());
      updateSolidity(size - maxSize.get());
      size = maxSize.get();
      flush();
    }

    advance();
    ++activeSession;
    return new Session(this, disableOnExit);
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor) {
    dbs.forEach(db -> db.setCursor(cursor));
  }

  @Override
  public void setCursor(Chainbase.Cursor cursor, long offset) {
    dbs.forEach(db -> db.setCursor(cursor, offset));
  }

  @Override
  public void add(IRevokingDB db) {
    Chainbase revokingDB = (Chainbase) db;
    dbs.add(revokingDB);
    flushServices.put(revokingDB.getDbName(),
        MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
  }

  private void advance() {
    dbs.forEach(db -> db.setHead(db.getHead().advance()));
    ++size;
  }

  private void retreat() {
    dbs.forEach(db -> db.setHead(db.getHead().retreat()));
    --size;
  }

  public void merge() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeDialog has to be greater than 0");
    }

    if (size < 2) {
      return;
    }

    dbs.forEach(db -> db.getHead().getPrevious().merge(db.getHead()));
    retreat();
    --activeSession;
  }

  public synchronized void revoke() {
    if (disabled) {
      return;
    }

    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be greater than 0");
    }

    if (size <= 0) {
      return;
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
    --activeSession;
  }

  public synchronized void commit() {
    if (activeSession <= 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be greater than 0");
    }

    --activeSession;
  }

  public synchronized void pop() {
    if (activeSession != 0) {
      throw new RevokingStoreIllegalStateException("activeSession has to be equal 0");
    }

    if (size <= 0) {
      throw new RevokingStoreIllegalStateException("there is not snapshot to be popped");
    }

    disabled = true;

    try {
      retreat();
    } finally {
      disabled = false;
    }
  }

  @Override
  public void fastPop() {
    pop();
  }

  public synchronized void enable() {
    disabled = false;
  }

  @Override
  public int size() {
    return size;
  }

  public int getMaxSize() {
    return maxSize.get();
  }

  @Override
  public void setMaxSize(int maxSize) {
    this.maxSize.set(maxSize);
  }

  public synchronized void disable() {
    disabled = true;
  }

  @Override
  public void shutdown() {
    System.err.println("******** begin to pop revokingDb ********");
    System.err.println("******** before revokingDb size:" + size);
    checkTmpStore.close();
    System.err.println("******** end to pop revokingDb ********");
  }

  public void updateSolidity(int hops) {
    for (int i = 0; i < hops; i++) {
      for (Chainbase db : dbs) {
        db.getHead().updateSolidity();
      }
    }
  }

  private boolean shouldBeRefreshed() {
    return flushCount >= maxFlushCount;
  }

  private void refresh() {
    List<ListenableFuture<?>> futures = new ArrayList<>(dbs.size());
    for (Chainbase db : dbs) {
      futures.add(flushServices.get(db.getDbName()).submit(() -> refreshOne(db)));
    }
    Future<?> future = Futures.allAsList(futures);
    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TronDBException(e);
    } catch (ExecutionException e) {
      throw new TronDBException(e);
    }
  }

  private void refreshOne(Chainbase db) {
    if (Snapshot.isRoot(db.getHead())) {
      return;
    }

    List<Snapshot> snapshots = new ArrayList<>();

    SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
    Snapshot next = root;
    for (int i = 0; i < flushCount; ++i) {
      next = next.getNext();
      snapshots.add(next);
    }
    // set current block number flag
//    if (!db.getDbName().equals("market_pair_price_to_order")
//        && !db.getDbName().equals("witness")
//        && !db.getDbName().equals("votes")
//        && !db.getDbName().equals("witness_schedule")) {
 //     next.put(CURRENT_FLUSHED_BLOCK_NUM_KEY, Longs.toByteArray(currentBlockNum));
    //}

    root.merge(snapshots);

    root.resetSolidity();
    if (db.getHead() == next) {
      db.setHead(root);
    } else {
      next.getNext().setPrevious(root);
      root.setNext(next.getNext());
    }
  }

  public void flush() {
    if (unChecked) {
      return;
    }

    if (shouldBeRefreshed()) {
      try {
        long start = System.currentTimeMillis();
        if (!isV2Open()) {
          Histogram.Timer requestTimer = Metrics.histogramStartTimer(
              MetricKeys.Histogram.DB_FLUSH, "delete");
          deleteCheckpoint();
          Metrics.histogramObserve(requestTimer);
        }
        Histogram.Timer createTimer = Metrics.histogramStartTimer(
            MetricKeys.Histogram.DB_FLUSH, "create");
        createCheckpoint();
        Metrics.histogramObserve(createTimer);
//        } else {
//          Histogram.Timer createV2 = Metrics.histogramStartTimer(
//              MetricKeys.Histogram.DB_FLUSH, "create2");
//          createCheckpointV2();
//          Metrics.histogramObserve(createV2);
//        }
        long checkPointEnd = System.currentTimeMillis();
        refresh();
        flushCount = 0;
        logger.info("flush cost:{}, create checkpoint cost:{}, refresh cost:{}",
            System.currentTimeMillis() - start,
            checkPointEnd - start,
            System.currentTimeMillis() - checkPointEnd
        );
      } catch (TronDBException e) {
        logger.error(" Find fatal error , program will be exited soon", e);
        hitDown = true;
        LockSupport.unpark(exitThread);
      }
    }
  }

  private void createCheckpoint() {
    try {
      Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
      for (Chainbase db : dbs) {
        Snapshot head = db.getHead();
        if (Snapshot.isRoot(head)) {
          return;
        }

        String dbName = db.getDbName();
        Snapshot next = head.getRoot();
        for (int i = 0; i < flushCount; ++i) {
          next = next.getNext();
          SnapshotImpl snapshot = (SnapshotImpl) next;
          DB<Key, Value> keyValueDB = snapshot.getDb();
          for (Map.Entry<Key, Value> e : keyValueDB) {
            Key k = e.getKey();
            Value v = e.getValue();
            batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())),
                WrappedByteArray.of(v.encode()));
            if (isV2Open() && db.getDbName().equals("block")) {
              currentBlockNum = new BlockCapsule(v.getBytes()).getNum();
            }
          }
        }
      }

      if (isV2Open()) {
        if (currentBlockNum < 0) {
          logger.error("create checkpoint failed, currentBlockNum: {}", currentBlockNum);
          System.exit(-1);
        }
        byte[] now  = Longs.toByteArray(System.currentTimeMillis());
        checkPointV2Store.getDbSource().updateByBatch(batch.entrySet().stream()
                .map(e -> Maps.immutableEntry(
                    Bytes.concat(Longs.toByteArray(currentBlockNum), now, e.getKey().getBytes()),
                    e.getValue().getBytes()))
                .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll),
            WriteOptionsWrapper.getInstance().sync(CommonParameter
                .getInstance().getStorage().isDbSync()));
      } else {
        checkTmpStore.getDbSource().updateByBatch(batch.entrySet().stream()
                .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
                .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll),
            WriteOptionsWrapper.getInstance().sync(CommonParameter
                .getInstance().getStorage().isDbSync()));
      }
    } catch ( Exception e) {
      throw new TronDBException(e);
    }
  }

  private void createCheckpointV2() {
    try {
      Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
      for (Chainbase db : dbs) {
        Snapshot head = db.getHead();
        if (Snapshot.isRoot(head)) {
          return;
        }

        String dbName = db.getDbName();
        Snapshot next = head.getRoot();
        for (int i = 0; i < flushCount; ++i) {
          next = next.getNext();
          SnapshotImpl snapshot = (SnapshotImpl) next;
          DB<Key, Value> keyValueDB = snapshot.getDb();
          for (Map.Entry<Key, Value> e : keyValueDB) {
            Key k = e.getKey();
            Value v = e.getValue();
            batch.put(WrappedByteArray.of(Bytes.concat(simpleEncode(dbName), k.getBytes())),
                WrappedByteArray.of(v.encode()));
            if (db.getDbName().equals("block")) {
              currentBlockNum = new BlockCapsule(v.getBytes()).getNum();
            }
          }
        }
      }

      checkTmpStore.getDbSource().updateByBatch(batch.entrySet().stream()
              .map(e -> Maps.immutableEntry(
                  Bytes.concat(Longs.toByteArray(currentBlockNum), e.getKey().getBytes()),
                  e.getValue().getBytes()))
              .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll),
          WriteOptionsWrapper.getInstance().sync(CommonParameter
              .getInstance().getStorage().isDbSync()));

    } catch ( Exception e) {
      throw new TronDBException(e);
    }

//    try {
//      CheckpointOuterClass.Checkpoint.Builder cp = CheckpointOuterClass.Checkpoint.newBuilder();
//      for (Chainbase db : dbs) {
//        Snapshot head = db.getHead();
//        if (Snapshot.isRoot(head)) {
//          return;
//        }
//
//        String dbName = db.getDbName();
//        Snapshot next = head.getRoot();
//        for (int i = 0; i < flushCount; ++i) {
//          next = next.getNext();
//          SnapshotImpl snapshot = (SnapshotImpl) next;
//          DB<Key, Value> keyValueDB = snapshot.getDb();
//          for (Map.Entry<Key, Value> e : keyValueDB) {
//            Key k = e.getKey();
//            Value v = e.getValue();
//            cp.putEntry(
//                ByteArray.toHexString(Bytes.concat(simpleEncode(dbName), k.getBytes())),
//                ByteString.copyFrom(v.encode()));
//            if (db.getDbName().equals("block")) {
//              currentBlockNum = new BlockCapsule(v.getBytes()).getNum();
//            }
//          }
//        }
//      }
//      if (currentBlockNum == -1) {
//        throw new TronDBException("create checkpoint failed, block num should not be -1");
//      }
//      checkPointV2Store.getDbSource().putWithOption(
//          Bytes.concat(Longs.toByteArray(currentBlockNum),
//              Longs.toByteArray(System.currentTimeMillis())),
//          cp.build().toByteArray(),
//          WriteOptionsWrapper.getInstance().sync(true));
//
//      logger.info("create checkpoint success, number: {}", currentBlockNum);
//    } catch ( Exception e) {
//      throw new TronDBException(e);
//    }
  }

  private void deleteCheckpoint() {
    checkTmpStore.reset();
//    try {
//      Map<byte[], byte[]> hmap = new HashMap<>();
//      if (!checkTmpStore.getDbSource().allKeys().isEmpty()) {
//        for (Map.Entry<byte[], byte[]> e: checkTmpStore.getDbSource()) {
//          hmap.put(e.getKey(), null);
//        }
//      }
//
//      checkTmpStore.getDbSource().updateByBatch(hmap);
//    } catch (Exception e) {
//      throw new TronDBException(e);
//    }
  }

  private void pruneCheckpoint() {
    if (unChecked) {
      return;
    }
    long prevBlockNumber = -1;
    boolean first = true;
    for (Map.Entry<byte[], byte[]> entry: checkPointV2Store.getDbSource()) {
      byte[] key = entry.getKey();
      long blockNumber = Longs.fromByteArray(Arrays.copyOf(key, 8));
      if (first) {
        prevBlockNumber = blockNumber;
        first = false;
      }
      long timestamp = Longs.fromByteArray(Arrays.copyOfRange(key, 8, 16));
      if (System.currentTimeMillis() - timestamp < ONE_MINUTE_MILLS * 2) {
        break;
      }
      checkPointV2Store.delete(key);
      if (prevBlockNumber != blockNumber) {
        logger.info("checkpoint prune, number: {}", prevBlockNumber);
      }
      prevBlockNumber = blockNumber;
    }
  }

  // ensure run this method first after process start.
  @Override
  public void check() {
    if (!isV2Open()) {
      if (checkPointV2Store.getDbSource().allKeys().size() > 0) {
        logger.error("db check failed, can't convert checkpoint from v2 to v1");
        System.exit(-1);
      }
      checkV1();
    } else {
      checkV2();
    }
  }

  private void checkV1() {
    for (Chainbase db: dbs) {
      if (!Snapshot.isRoot(db.getHead())) {
        throw new IllegalStateException("first check.");
      }
    }

    if (!checkTmpStore.getDbSource().allKeys().isEmpty()) {
      Map<String, Chainbase> dbMap = dbs.stream()
          .map(db -> Maps.immutableEntry(db.getDbName(), db))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      advance();
      for (Map.Entry<byte[], byte[]> e: checkTmpStore.getDbSource()) {
        byte[] key = e.getKey();
        byte[] value = e.getValue();
        String db = simpleDecode(key);
        if (dbMap.get(db) == null) {
          continue;
        }
        byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);

        byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
        if (realValue != null) {
          dbMap.get(db).getHead().put(realKey, realValue);
        } else {
          dbMap.get(db).getHead().remove(realKey);
        }

      }

      dbs.forEach(db -> db.getHead().getRoot().merge(db.getHead()));
      retreat();
    }

    unChecked = false;
  }

  private void checkV2() {
    logger.info("checkpoint version: {}", CommonParameter.getInstance().getStorage().getCheckpointVersion());
    logger.info("checkpoint sync: {}", CommonParameter.getInstance().getStorage().isCheckpointSync());
    Set<byte[]> allKeys = checkPointV2Store.getDbSource().allKeys();
    // todo: review this logic
    if (allKeys.size() == 0) {
      logger.info("checkpoint size is 0, using v1 recover");
      checkV1();
      deleteCheckpoint();
      return;
    }

    Map<String, Chainbase> dbMap = dbs.stream()
        .map(db -> Maps.immutableEntry(db.getDbName(), db))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    advance();

    long beginBlockNumber = -1, endBlockNumber = -1;
    boolean first = true;
    for (Map.Entry<byte[], byte[]> e: checkPointV2Store.getDbSource()) {
      byte[] k = e.getKey();
      byte[] key = Arrays.copyOfRange(k, 16, k.length);
      byte[] value = e.getValue();
      if (first) {
        beginBlockNumber = Longs.fromByteArray(Arrays.copyOfRange(k, 0, 8));
        first = false;
      }
      endBlockNumber =Longs.fromByteArray(Arrays.copyOfRange(k, 0, 8));
      if (endBlockNumber == -1) {
        logger.error("checkpoint recover failed, checkpoint illegal, blocknumber can't be -1");
        System.exit(-1);
      }
      String db = simpleDecode(key);
      if (dbMap.get(db) == null) {
        continue;
      }
      byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);
      byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);
      if (realValue != null) {
        dbMap.get(db).getHead().put(realKey, realValue);
      } else {
        dbMap.get(db).getHead().remove(realKey);
      }

    }

    dbs.forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    retreat();

    unChecked = false;
    logger.info("checkpoint recover success, block range from:{}, to: {}", beginBlockNumber, endBlockNumber);

    //System.exit(-1);
  }

  private boolean isV2Open() {
    return checkpointVersion == 2;
  }

  private byte[] simpleEncode(String s) {
    byte[] bytes = s.getBytes();
    byte[] length = Ints.toByteArray(bytes.length);
    byte[] r = new byte[4 + bytes.length];
    System.arraycopy(length, 0, r, 0, 4);
    System.arraycopy(bytes, 0, r, 4, bytes.length);
    return r;
  }

  @Slf4j(topic = "DB")
  @Getter // only for unit test
  public static class Session implements ISession {

    private SnapshotManager snapshotManager;
    private boolean applySnapshot = true;
    private boolean disableOnExit = false;

    public Session(SnapshotManager snapshotManager) {
      this(snapshotManager, false);
    }

    public Session(SnapshotManager snapshotManager, boolean disableOnExit) {
      this.snapshotManager = snapshotManager;
      this.disableOnExit = disableOnExit;
    }

    @Override
    public void commit() {
      applySnapshot = false;
      snapshotManager.commit();
    }

    @Override
    public void revoke() {
      if (applySnapshot) {
        snapshotManager.revoke();
      }

      applySnapshot = false;
    }

    @Override
    public void merge() {
      if (applySnapshot) {
        snapshotManager.merge();
      }

      applySnapshot = false;
    }

    @Override
    public void destroy() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }

    @Override
    public void close() {
      try {
        if (applySnapshot) {
          snapshotManager.revoke();
        }
      } catch (Exception e) {
        logger.error("revoke database error.", e);
        throw new RevokingStoreIllegalStateException(e);
      }
      if (disableOnExit) {
        snapshotManager.disable();
      }
    }
  }

}
