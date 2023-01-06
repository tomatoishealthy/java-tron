package org.tron.core.tire;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.protobuf.CodedOutputStream;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.state.StateType;
import org.tron.core.state.WorldStateTrieStore;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.state.trie.TrieImpl;
import stest.tron.wallet.common.client.utils.ByteArray;

import java.util.List;

@Ignore
public class WorldStateTrieTest {
  private TronApplicationContext context;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private Application appTest;
  private WorldStateTrieStore worldStateTrieStore;
  private BlockStore blockStore;

  /**
   * init logic.
   */
  //@Before
  public void startApp() {
    Args.setParam(new String[]{"-d", "../output-directory"}, "config-localtest.conf");
    // allow account root
    Args.getInstance().setAllowAccountStateRoot(1);
    // init dbBackupConfig to avoid NPE
    Args.getInstance().dbBackupConfig = DbBackupConfig.getInstance();
    context = new TronApplicationContext(DefaultConfig.class);
    appTest = ApplicationFactory.create(context);
    appTest.addService(context.getBean(RpcApiService.class));
    appTest.addService(context.getBean(RpcApiServiceOnSolidity.class));
    appTest.initServices(Args.getInstance());
    appTest.startServices();
    appTest.startup();
    worldStateTrieStore = context.getBean(WorldStateTrieStore.class);
    blockStore = context.getBean(BlockStore.class);

    String fullnode = String.format("%s:%d", "127.0.0.1",
        Args.getInstance().getRpcPort());
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test
  public void test1() {
    System.out.println(Hash.EMPTY_TRIE_HASH);
    System.out.println(Hex.toHexString(Hash.EMPTY_TRIE_HASH));

//    CodedOutputStream.computeMessageSize(1, );
    System.out.println(ByteArray.toHexString(Ints.toByteArray(-128)));
  }


  @Test
  public void get() {
    List<BlockCapsule> blockCapsuleList = blockStore.getBlockByLatestNum(1);
    BlockCapsule blockCapsule = blockCapsuleList.get(0);
    System.out.println(blockCapsule.getArchiveStateRoot());
    System.out.println(blockCapsule.getNum());
//    TrieImpl trie = new TrieImpl(worldStateTrieStore, blockCapsule.getStateRoot().getBytes());
    TrieImpl trie = new TrieImpl(worldStateTrieStore, ByteArray.fromHexString("c3d0e55383fec167784b5d0646a402fc2117a1c7d137e9a4569e457071b66b3b"));
    byte[] key = "latest_block_header_hash".getBytes();
   // key = ByteArray.fromHexString("4171B0AF54E0A1182A5E0947D6A64F3B22740EF318");
    byte[] prefix = new byte[]{StateType.Properties.value()};
//    byte[] prefix = new byte[]{StateType.Account.value()};
    byte[] realKey = ByteUtil.merge(prefix, key);
//    AccountCapsule account = new AccountCapsule(trie.get(Hash.encodeElement(realKey)));
//    System.out.println(account);
    System.out.println(ByteArray.toHexString(trie.get(Hash.encodeElement(realKey))));
//  realKey = ByteUtil.merge(prefix, "latest_block_header_number".getBytes());
    realKey = ByteUtil.merge(prefix, "ALLOW_MULTI_SIGN".getBytes());
    System.out.println(Longs.fromByteArray(trie.get(Hash.encodeElement(realKey))));
  }
}
