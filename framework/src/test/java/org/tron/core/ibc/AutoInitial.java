package org.tron.core.ibc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class AutoInitial {

  private static String contractPath = Config.contractPath;
  private static String posPortalPath = Config.posPortalPath;
  private String fullNode = Config.fullNode;
  private String account1PriKey = Config.accountPriKey;
  private String contractAddress = Config.contractAddress;

  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private byte[] getAddress(String priKey) {
    return ECKey.fromPrivate(ByteArray.fromHexString(priKey)).getAddress();
  }

  /**
   * constructor.
   */
  @Before
  public void beforeClass() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    ManagedChannel fromChannelFull = ManagedChannelBuilder.forTarget(fullNode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(fromChannelFull);

    PublicMethed.printAddress(account1PriKey);
  }

  @Test
  public void initialize() throws IOException {
    String adminAddrss = StringUtil.encode58Check(getAddress(account1PriKey));
    // init stakeManager
    String methodStr = "initialize(address,address,address,address,address,address,address,address,address)";
    String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList(
            getAddr(AutoDeploy.Contract.Registry.getName()),
            getAddr(AutoDeploy.Contract.RootChainProxy.getName()),
            getAddr(AutoDeploy.Contract.DummyERC20.getName()),
            getAddr(AutoDeploy.Contract.StakingNFT.getName()),
            getAddr(AutoDeploy.Contract.StakingInfo.getName()),
            getAddr(AutoDeploy.Contract.ValidatorShareFactory.getName()),
            getAddr(AutoDeploy.Contract.GovernanceProxy.getName()),
            adminAddrss,
            getAddr(AutoDeploy.Contract.StakeManagerExtension.getName())
    ));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.StakeManagerProxy.getName())), methodStr, argsStr,
            true, 0, 300000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- init StakeManagerProxy finished ---");

    //** update registry map **//
    String stakeManagerKeccak = "56e86af72b94d3aa725a2e35243d6acbf3dc1ada7212033defd5140c5fcb6a9d";
    String validatorShareKeccak = "f32233bced9bbd82f0754425f51b5ffaf897dacec3c8ac3384a66e38ea701ec8";
    String eventsHubKeccak = "a1ed0e7a71ca197f0dfc1206d3fcb9c6b88b70f1c3a11268f9b6ed75e8cabd08";
    String stakeManagerAddr = getAddr(AutoDeploy.Contract.StakeManagerProxy.getName());
    String validatorShareAddr = getAddr(AutoDeploy.Contract.ValidatorShare.getName());
    String eventsHubAddr = getAddr(AutoDeploy.Contract.EventsHubProxy.getName());

    String updateMap = "updateContractMap(bytes32,address)";
    String updateStakeManager = AbiUtil.parseMethod(updateMap, Arrays.asList(stakeManagerKeccak, stakeManagerAddr));
    String updateValidatorShare = AbiUtil.parseMethod(updateMap, Arrays.asList(validatorShareKeccak, validatorShareAddr));
    String updateEventsHub = AbiUtil.parseMethod(updateMap, Arrays.asList(eventsHubKeccak, eventsHubAddr));

    // update stakemanager
    methodStr = "update(address,bytes)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.Registry.getName()), updateStakeManager));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.GovernanceProxy.getName())), methodStr, argsStr,
            true, 0, 100000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- update StakeManagerProxy in registry finished ---");

    // update validatorShare
    methodStr = "update(address,bytes)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.Registry.getName()), updateValidatorShare));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.GovernanceProxy.getName())), methodStr, argsStr,
            true, 0, 100000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- update validatorShare in registry finished ---");

    // update eventsHub
    methodStr = "update(address,bytes)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.Registry.getName()), updateEventsHub));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.GovernanceProxy.getName())), methodStr, argsStr,
            true, 0, 100000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- update eventsHub in registry finished ---");

    // transfer nft ownership
    methodStr = "transferOwnership(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.StakeManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.StakingNFT.getName())), methodStr, argsStr,
            true, 0, 100000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- transfer the nft Ownership to stakeManager finished ---");

    // initialize RootChainManagerProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(adminAddrss));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize RootChainManagerProxy finished ---");

    // initialize ERC20PredicateProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.ERC20PredicateProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize ERC20PredicateProxy finished ---");

    // initialize MinableERC20PredicateProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.MintableERC20PredicateProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize MinableERC20PredicateProxy finished ---");

    // initialize EtherPredicateProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.EtherPredicateProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize EtherPredicateProxy finished ---");

    // initialize ERC721PredicateProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.ERC721PredicateProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize ERC721PredicateProxy finished ---");

    // initialize MintableERC721PredicateProxy
    methodStr = "initialize(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.MintableERC721PredicateProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- initialize MintableERC721PredicateProxy finished ---");

    // set checkpoint
    methodStr = "setCheckpointManager(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.RootChainProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- setCheckpointManager in RootChainManagerProxy finished ---");

    // set stateSender
    methodStr = "setStateSender(address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(getAddr(AutoDeploy.Contract.StateSender.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- stateSender in RootChainManagerProxy finished ---");

    // register ether
    String EtherTokenType = "0xa234e09165f88967a714e2a476288e4c6d88b4b69fe7c300a03190b858990bfc";
    methodStr = "registerPredicate(bytes32,address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(EtherTokenType, getAddr(AutoDeploy.Contract.EtherPredicateProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- register etherPredicate in RootChainManagerProxy finished ---");

    // register erc20
    String ERC20TokenType = "0x8ae85d849167ff996c04040c44924fd364217285e4cad818292c7ac37c0a345b";
    methodStr = "registerPredicate(bytes32,address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(ERC20TokenType, getAddr(AutoDeploy.Contract.ERC20PredicateProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- register etherPredicate in RootChainManagerProxy finished ---");

    // register mintable erc20
    String MintableERC20TokenType = "0x5ffef61af1560b9aefc0e42aaa0f9464854ab113ab7b8bfab271be94cdb1d053";
    methodStr = "registerPredicate(bytes32,address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(MintableERC20TokenType, getAddr(AutoDeploy.Contract.MintableERC20PredicateProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- register etherPredicate in RootChainManagerProxy finished ---");

    // register erc721
    String ERC721TokenType = "0x73ad2146b3d3a286642c794379d750360a2d53a3459a11b3e5d6cc900f55f44a";
    methodStr = "registerPredicate(bytes32,address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(ERC721TokenType, getAddr(AutoDeploy.Contract.ERC721PredicateProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- register etherPredicate in RootChainManagerProxy finished ---");

    // register mintable erc721
    String MintableERC721TokenType = "0xd4392723c111fcb98b073fe55873efb447bcd23cd3e49ec9ea2581930cd01ddc";
    methodStr = "registerPredicate(bytes32,address)";
    argsStr =  AbiUtil.parseParameters(methodStr, Arrays.asList(MintableERC721TokenType, getAddr(AutoDeploy.Contract.MintableERC721PredicateProxy.getName())));
    PublicMethed.triggerContract(Commons.decodeFromBase58Check(getAddr(AutoDeploy.Contract.RootChainManagerProxy.getName())), methodStr, argsStr,
            true, 0, 200000000L, "0", 0,
            getAddress(account1PriKey), account1PriKey, blockingStubFull);
    System.out.println("--- register etherPredicate in RootChainManagerProxy finished ---");

    System.out.println("initialize finished, please go ahead to the txes of the admin to check whether the txes are successfully");
  }



  public String getAddr(String contract) throws IOException {
    // read the existed addr
    FileInputStream inputStream = new FileInputStream(contractAddress);
    int size = inputStream.available();
    byte[] buffer = new byte[size];
    inputStream.read(buffer);
    inputStream.close();
    String jsonString = new String(buffer, StandardCharsets.UTF_8);
    JSONObject jsonObject = JSON.parseObject(jsonString, JSONObject.class);
    return (String)jsonObject.get(contract);
  }

  public void writeAddr(String contract, String address) throws IOException {

    FileUtil.createFileIfNotExists(contractAddress);

    // read the existed addr
    FileInputStream inputStream = new FileInputStream(contractAddress);
    int size = inputStream.available();
    byte[] buffer = new byte[size];
    inputStream.read(buffer);
    inputStream.close();
    String jsonString = new String(buffer, StandardCharsets.UTF_8);
    JSONObject jsonObject = JSON.parseObject(jsonString, JSONObject.class);

    jsonObject.put(contract, address);

    // write the address
    OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(contractAddress), StandardCharsets.UTF_8);
    osw.write(jsonObject.toJSONString());
    osw.flush();
    osw.close();
  }
}