package org.tron.program;

import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.StorageUtils;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.db2.core.Chainbase;
import org.tron.core.exception.BadItemException;
import org.tron.core.store.CheckTmpStore;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class Check {

  public static void main(String[] args) throws BadItemException {
    Args.setParam(args, Constant.TESTNET_CONF);
    CommonParameter parameter = Args.getInstance();
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);
    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);

    context.refresh();

    BlockStore blockStore = context.getBean(BlockStore.class);
    System.out.println(blockStore.getBlockByLatestNum(1));

    System.out.println("-----------------------");
    CheckTmpStore tmpStore = context.getBean(CheckTmpStore.class);
    printTmp(tmpStore);

    System.out.println("end-----------------------");


  }


  public static void printTmp(CheckTmpStore checkTmpStore) throws BadItemException {
    long bigestNum = 0;
    if (!checkTmpStore.getDbSource().allKeys().isEmpty()) {
      for (Map.Entry<byte[], byte[]> e : checkTmpStore.getDbSource()) {
        byte[] key = e.getKey();
        byte[] value = e.getValue();
        String db = simpleDecode(key);
        byte[] realKey = Arrays.copyOfRange(key, db.getBytes().length + 4, key.length);

        byte[] realValue = value.length == 1 ? null : Arrays.copyOfRange(value, 1, value.length);

        if (db.equals("block")) {
          long nowNum = new BlockCapsule(realValue).getNum();
          if (bigestNum < nowNum) {
            bigestNum = nowNum;
          }
        } else if (db.equals("account")) {
//          System.out.println("--account--");
//          AccountCapsule accountCapsule = new AccountCapsule(realValue);
//          System.out.println(ByteArray.toHexString(accountCapsule.getAddress().toByteArray()));
//          System.out.println(accountCapsule);
        }
      }
    }
    System.out.println("--block--");
    System.out.println(bigestNum);
  }


  public static String simpleDecode(byte[] bytes) {
    byte[] lengthBytes = Arrays.copyOf(bytes, 4);
    int length = Ints.fromByteArray(lengthBytes);
    byte[] value = Arrays.copyOfRange(bytes, 4, 4 + length);
    return new String(value);
  }

}
