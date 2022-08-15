package org.tron.core.store;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.tron.common.storage.WriteOptionsWrapper;
import org.tron.core.db.TronDatabase;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.ItemNotFoundException;

import java.util.Spliterator;
import java.util.function.Consumer;

@Component
public class CheckPointV2Store extends TronDatabase<byte[]> {

  @Autowired
  public CheckPointV2Store(ApplicationContext ctx) {
    super("checkpoint");
  }

//  @Override
//  protected Options getOptionsByDbNameForLevelDB(String dbName) {
//    Options options = StorageUtils.getOptionsByDbName(dbName);
//    options.comparator(new CheckpointComparatorForLevelDB());
//    return options;
//  }
//
//  //todo: to test later
//  @Override
//  protected DirectComparator getDirectComparator() {
//    ComparatorOptions comparatorOptions = new ComparatorOptions();
//    return new CheckpointComparatorForRockDB(comparatorOptions);
////    return null;
//  }

  @Override
  public void put(byte[] key, byte[] item) {
  }

  @Override
  public void delete(byte[] key) {
    getDbSource().deleteData(key);
  }

  @Override
  public byte[] get(byte[] key)
      throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException {
    return null;
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  @Override
  public void forEach(Consumer action) {

  }

  @Override
  public Spliterator spliterator() {
    return null;
  }

  public void put(byte[] key, byte[] item, WriteOptionsWrapper optionsWrapper) {
    getDbSource().putWithOption(key, item, optionsWrapper);
  }
}