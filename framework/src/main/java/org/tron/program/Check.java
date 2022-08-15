package org.tron.program;

import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import org.bouncycastle.util.Strings;
import org.iq80.leveldb.*;
import org.tron.common.utils.ByteArray;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.exception.BadItemException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static org.tron.program.DBConvert.newDefaultLevelDbOptions;


public class Check {


  public static void main(String[] args) throws IOException, InterruptedException {
//    boolean sync = Boolean.parseBoolean(args[0]);
//    int loop = Integer.parseInt(args[1]);
//    int valueSize = Integer.parseInt(args[2]);
    Options dbOptions = newDefaultLevelDbOptions();
    //CheckpointComparatorForLevelDB comparator = new CheckpointComparatorForLevelDB();
    //dbOptions.comparator(comparator);
    DB db = initDB("/Users/quan/tron/java-tron/output-directory/database/checkpoint", dbOptions);
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

    read(db);

//    checkDBEqual("/Users/quan/tron/java-tron/output-directory/database/IncrementalMerkleTree",
//        "/Users/quan/tron/java-tron/output-directory_bak/database/IncrementalMerkleTree", dbOptions);

  }

  public static void checkAllDBEqual(String sourcePath, String destPath) throws IOException {
    List<String> dbs = Lists.newArrayList();
    File[] sourceFiles = new File(sourcePath).listFiles();
    for(File file: sourceFiles) {
      if (file.isDirectory()) {
        dbs.add(file.getName());
      }
    }
    List<String> diffDbs = Lists.newArrayList();
    int totalDB = 0;
    for(String db: dbs) {
      if(db.equals("market_pair_price_to_order")) {
        continue;
      }
      System.out.println("db: " + db);
      String sourceDBPath = Paths.get(sourcePath, db).toString();
      String destDBPath = Paths.get(destPath, db).toString();
      Options dbOptions = newDefaultLevelDbOptions();
      boolean flag = checkDBEqual(sourceDBPath, destDBPath, dbOptions);
      if (!flag) {
        diffDbs.add(db);
      }
      totalDB++;
      System.out.println();
      System.out.println();
    }
    System.out.println("total compared: " + totalDB);
    System.out.println("diff dbs: " + diffDbs);

//    Options dbOptions = newDefaultLevelDbOptions();
//    checkDBEqual("/Users/quan/tron/java-tron/output-directory/database/account",
//        "/Users/quan/tron/java-tron/output-directory_bak/database/account", dbOptions);
  }

  public static boolean checkDBEqual(String sourcePath, String destPath, Options options) throws IOException {
    boolean flag = true;
    DB source = initDB(sourcePath, options);
    DB dest = initDB(destPath, options);
    try (DBIterator sourceIterator = source.iterator();
         DBIterator destIterator = dest.iterator();) {
      System.out.println("--------");
      System.out.println("source compare");
      System.out.println("--------");
      long total = 0;
      long diffCount = 0;
      for (sourceIterator.seekToFirst(); sourceIterator.hasNext(); sourceIterator.next()) {
        total++;
        byte[] key = sourceIterator.peekNext().getKey();
        byte[] value = sourceIterator.peekNext().getValue();
        byte[] destValue = dest.get(key);
        if (destValue == null || !Arrays.equals(value, destValue)) {
          System.out.println("db not consistent");
          System.out.println("key: " + ByteArray.toHexString(key));
          //System.out.println("source value: " + value);
          //System.out.println("dest value: " + destValue);
          diffCount++;
        }
      }
      flag = diffCount == 0;
      System.out.println("--------");
      System.out.println("total num: " + total);
      System.out.println("There are " + diffCount + " diff between source and dest");
      System.out.println("--------");

      System.out.println("--------");
      System.out.println("dest compare");
      System.out.println("--------");
      diffCount = 0;
      total = 0;
      for (destIterator.seekToFirst(); destIterator.hasNext(); destIterator.next()) {
        total++;
        byte[] key = destIterator.peekNext().getKey();
        byte[] value = destIterator.peekNext().getValue();
        byte[] sourceValue = source.get(key);
        if (sourceValue == null || !Arrays.equals(value, sourceValue)) {
          System.out.println("db not consistent");
          System.out.println("key: " + ByteArray.toHexString(key));
          System.out.println("dest value: " + value.length);
          System.out.println("source value: " + sourceValue);
          diffCount++;
        }
      }
      flag = diffCount == 0;
      System.out.println("--------");
      System.out.println("total num: " + total);
      System.out.println("There are " + diffCount + " diff between dest and source");
      System.out.println("--------");
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
