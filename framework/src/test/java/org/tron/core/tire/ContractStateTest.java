package org.tron.core.tire;

import com.google.common.primitives.Longs;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.config.DbBackupConfig;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteUtil;
import org.tron.common.utils.Commons;
import org.tron.core.Wallet;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.interfaceOnSolidity.RpcApiServiceOnSolidity;
import org.tron.core.state.StateType;
import org.tron.core.state.WorldStateTrieStore;
import org.tron.core.state.trie.TrieImpl;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.Arrays;
import java.util.List;

@Ignore
public class ContractStateTest {

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String priKey = "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366";
  private String bytecode = "60806040526000805534801561001457600080fd5b50610181806100246000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c806342cbb15c146100465780636d4ce63c14610064578063d09de08a14610082575b600080fd5b61004e61008c565b60405161005b91906100cd565b60405180910390f35b61006c610094565b60405161007991906100cd565b60405180910390f35b61008a61009d565b005b600043905090565b60008054905090565b60016000546100ac9190610117565b600081905550565b6000819050919050565b6100c7816100b4565b82525050565b60006020820190506100e260008301846100be565b92915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610122826100b4565b915061012d836100b4565b9250828201905080821115610145576101446100e8565b5b9291505056fea26469706673582212207c5e242c88722ac1f7f5f1ea670cf1a784cad42b058651ceaf6fe0fc10ebff8264736f6c63430008110033";
  private String abi = "[\n" +
      "\t{\n" +
      "\t\t\"inputs\": [],\n" +
      "\t\t\"name\": \"get\",\n" +
      "\t\t\"outputs\": [\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"internalType\": \"uint256\",\n" +
      "\t\t\t\t\"name\": \"\",\n" +
      "\t\t\t\t\"type\": \"uint256\"\n" +
      "\t\t\t}\n" +
      "\t\t],\n" +
      "\t\t\"stateMutability\": \"view\",\n" +
      "\t\t\"type\": \"function\"\n" +
      "\t},\n" +
      "\t{\n" +
      "\t\t\"inputs\": [],\n" +
      "\t\t\"name\": \"getBlockNumber\",\n" +
      "\t\t\"outputs\": [\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"internalType\": \"uint256\",\n" +
      "\t\t\t\t\"name\": \"\",\n" +
      "\t\t\t\t\"type\": \"uint256\"\n" +
      "\t\t\t}\n" +
      "\t\t],\n" +
      "\t\t\"stateMutability\": \"view\",\n" +
      "\t\t\"type\": \"function\"\n" +
      "\t},\n" +
      "\t{\n" +
      "\t\t\"inputs\": [],\n" +
      "\t\t\"name\": \"increment\",\n" +
      "\t\t\"outputs\": [],\n" +
      "\t\t\"stateMutability\": \"nonpayable\",\n" +
      "\t\t\"type\": \"function\"\n" +
      "\t}\n" +
      "]";
  private ECKey ecKey1 = ECKey.fromPrivate(ByteArray.fromHexString(priKey));
  private byte[] address = ecKey1.getAddress();

  ManagedChannel channelFull;

  String contractAddress = "TYwNNQ73kqtDW7mbhaRPSE9cFqjBhK2w1P";

  @Before
  public void beforeClass() {
    String fullnode = String.format("%s:%d", "127.0.0.1", 50051);
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    PublicMethed.printAddress(priKey);
  }

  @Test
  public void test() {
    System.out.println(StateType.get("properties"));
  }

  @Test
  public void deploy() {
    long maxFeeLimit = 1000000000;
    long originEnergyLimit = 1000000000;
    byte[] contractAddr =
        PublicMethed.deployContract(
            "stateQuery",
            abi,
            bytecode,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            priKey,
            address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract =
        PublicMethed.getContract(contractAddr, blockingStubFull);
    System.out.println(ByteArray.toHexString(contractAddr));
  }

  @Test
  public void triggerConstantContract() {
    String methodStr = "get()";
  //  String methodStr = "getBlockNumber()";
    String argsStr = "";
    System.out.println(
        PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
            false, 0, 100000000L, "0", 0,
            address, priKey, blockingStubFull)
    );
    System.out.println(AbiUtil.parseMethod(methodStr, argsStr, true));
  }

  @Test
  public void triggerContract() {
    String methodStr = "increment()";
    String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList());
    System.out.println(
        PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
            true, 0, 100000000L, "0", 0,
            address, priKey, blockingStubFull)
    );
  }
}
