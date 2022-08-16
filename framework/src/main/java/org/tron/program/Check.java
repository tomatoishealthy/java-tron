package org.tron.program;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.bouncycastle.util.Strings;
import org.iq80.leveldb.*;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.BadItemException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static org.tron.program.DBConvert.newDefaultLevelDbOptions;


public class Check {


  public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
    boolean type = Boolean.parseBoolean(args[0]);
    String sourcePath = args[1];
    String destPath = args[2];
    boolean details = false;
    if (args.length >= 4) {
      details = Boolean.parseBoolean(args[3]);
    }
    //Options dbOptions = newDefaultLevelDbOptions();
    //CheckpointComparatorForLevelDB comparator = new CheckpointComparatorForLevelDB();
    //dbOptions.comparator(comparator);
    //DB db = initDB("/Users/quan/tron/java-tron/output-directory/database/checkpoint", dbOptions);
//    WriteOptions writeOptions = new WriteOptions();
//    writeOptions.sync(sync);
    // db.put(Longs.toByteArray(1000000), "test".getBytes());
    //db.put(Longs.toByteArray(), "test".getBytes());

//    for(int j=0; j < loop; j++) {
//      Map<byte[], byte[]> batch = new HashMap<>();
//      for (int i =0; i < 1000; i++) {
//        String key = RandomStringUtils.randomAlphanumeric(20);
//        String value = RandomStringUtils.randomAlphanumeric(valueSize);
//        batch.put(key.getBytes(), value.getBytes());
//      }
//      byte[] bt = null;
//      ByteArrayOutputStream os = new ByteArrayOutputStream();
//      ObjectOutputStream oos = new ObjectOutputStream(os);
//      oos.writeObject(batch);
//      bt = os.toByteArray();
//      oos.close();
//      os.close();
//      long start = System.currentTimeMillis();
//      db.put(
//          Longs.toByteArray(j),
//          bt,
//          writeOptions);
//      long end = System.currentTimeMillis();
//
//      System.out.println("cost: " + (end - start));
//      Thread.sleep(1000);
//    }

    //read(db);

//    checkDBEqual("/Users/quan/tron/java-tron/output-directory/database/IncrementalMerkleTree",
//        "/Users/quan/tron/java-tron/output-directory_bak/database/IncrementalMerkleTree", dbOptions);
    if (type) {
      checkAllDBEqual(sourcePath, destPath, details);
    } else {
      Options dbOptions = newDefaultLevelDbOptions();
      checkDBEqual(sourcePath, sourcePath, destPath, dbOptions, details);
    }

  }

  public static void checkAllDBEqual(String sourcePath, String destPath, boolean details) throws IOException, ExecutionException, InterruptedException {
    List<String> dbs = Lists.newArrayList();
    File[] sourceFiles = new File(sourcePath).listFiles();
    for(File file: sourceFiles) {
      if (file.isDirectory()) {
        dbs.add(file.getName());
      }
    }
    List<String> diffDbs = Lists.newArrayList();
    List<ListenableFuture<?>> futures = new ArrayList<>(dbs.size());
    for(String db: dbs) {
      if(db.equals("market_pair_price_to_order") ||
          db.equals("checkpoint") ||
          db.equals("tmp")
      ) {
        continue;
      }
      futures.add(MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()).submit(() -> {
        System.out.println("start check: " + db);
        String sourceDBPath = Paths.get(sourcePath, db).toString();
        String destDBPath = Paths.get(destPath, db).toString();
        Options dbOptions = newDefaultLevelDbOptions();
        boolean flag = false;
        try {
          flag = checkDBEqual(db, sourceDBPath, destDBPath, dbOptions, details);
        } catch (IOException e) {
          e.printStackTrace();
        }
        if (!flag) {
          diffDbs.add(db);
        }
      }));
    }

    Future<?> future = Futures.allAsList(futures);
    future.get();
    System.out.println("total dbs: +" + futures.size());
    System.out.println("diff dbs: " + diffDbs);

  }

  public static boolean checkDBEqual(String name, String sourcePath, String destPath, Options options, boolean details) throws IOException {
    boolean flag = true;
    DB source = initDB(sourcePath, options);
    DB dest = initDB(destPath, options);
    try (DBIterator sourceIterator = source.iterator();
         DBIterator destIterator = dest.iterator();) {
//      System.out.println("--------");
//      System.out.println("source compare");
//      System.out.println("--------");
        System.out.println("start");
        long start = System.currentTimeMillis();
        long total = 0;
        long diffCount = 0;
        for (sourceIterator.seekToFirst(), destIterator.seekToFirst();
             sourceIterator.hasNext();
             sourceIterator.next(), destIterator.next()) {
          total++;
          byte[] key = sourceIterator.peekNext().getKey();
          byte[] value = sourceIterator.peekNext().getValue();

          byte[] destkey = destIterator.peekNext().getKey();
          byte[] destvalue = destIterator.peekNext().getValue();
          if (!Arrays.equals(key, destkey) ||
              !Arrays.equals(value, destvalue)) {
            if (details) {
              System.out.println("db not consistent");
              System.out.println("key: " + ByteArray.toHexString(key));
              System.out.println("source value: " + value);
              System.out.println("dest value: " + destvalue);
            }
            diffCount++;
          }
        }
        flag = diffCount == 0;
        System.out.println("--------");
        if (destIterator.hasNext()) {
          System.out.println("db not consistent, dest still has data");
          flag = false;
        }
        System.out.println("db: " + name);
        System.out.println("total num: " + total);
        System.out.println("There are " + diffCount + " diff between source and dest");
        System.out.println("cost: " + (System.currentTimeMillis() - start));
      System.out.println("--------");
      System.out.println();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    source.close();
    dest.close();
    return flag;
  }

  public static void read(DB db) {
    try (DBIterator iterator = db.iterator()) {
      Map<byte[], byte[]> result = new HashMap<>();
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        byte[] key = iterator.peekNext().getKey();
        byte[] value = iterator.peekNext().getValue();
        result.put(key, value);
        System.out.println(Longs.fromByteArray(key));
        //System.out.println("key");
        //System.out.println(Strings.fromByteArray(key));
//        BlockCapsule blockCapsule = new BlockCapsule(value);
//        System.out.println("block number: " + blockCapsule.getNum());
        //ystem.out.println(value.length);
//        WitnessCapsule witnessCapsule = new WitnessCapsule(value);
//        System.out.println(witnessCapsule.getAddress());
      }
      System.out.println(result.size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static DB initDB(String path, Options dbOptions) throws IOException {
    File file = new File(path);
    DB database;
    try {
      database = factory.open(file, dbOptions);
    } catch (IOException e) {
      if (e.getMessage().contains("Corruption:")) {
        factory.repair(file, dbOptions);
        database = factory.open(file, dbOptions);
        System.out.println(e);
      } else {
        throw e;
      }
    }
    return database;
  }

}
