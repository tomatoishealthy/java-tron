package org.tron.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.VotesCapsule;
import org.tron.core.db.TronStoreWithRevoking;
import org.tron.core.state.StateType;
import org.tron.core.state.WorldStateCallBackUtils;

@Component
public class VotesStore extends TronStoreWithRevoking<VotesCapsule> {

  @Autowired
  private WorldStateCallBackUtils worldStateCallBackUtils;

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
  }

  @Override
  public void put(byte[] key, VotesCapsule item) {
    super.put(key, item);
    worldStateCallBackUtils.callBack(StateType.Votes, key, item);
  }
}