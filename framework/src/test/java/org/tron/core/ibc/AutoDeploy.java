package org.tron.core.ibc;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


public class AutoDeploy {

  private static String contractPath = "/Users/quan/tron/contracts/build/contracts";
  private static String posPortalPath = "/Users/quan/tron/pos-portal/build/contracts";
  private String contractAddress = "src/test/java/org/tron/core/ibc/contractAddresses.json";
  // node conf
  private String fullnode = "47.252.19.181:50051";
  // account
  private String account1PriKey = "";   // nile

  // server
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
    ManagedChannel fromChannelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(fromChannelFull);

    PublicMethed.printAddress(account1PriKey);
  }

  @Test
  public void deploy() throws IOException {
    deployMerkle();
    deployECVerify();
    deployGovernance();
    deployGovernanceProxy();
    deployRegistry();
    deployRootChain();
    deployRootChainProxy();
   // deployStakeManager();
    deployStakeManagerProxy();
    deployStakingInfo();
    deployStakingNFT();
    deployStateSender();
    deployValidatorShare();
    deployValidatorShareFactory();
    deployStakeManagerExtension();
    deployEventsHub();
    deployEventsHubProxy();
    deployTronRootChainManager();
    deployRootChainManagerProxy();
    deployERC20Predicate();
    deployERC20PredicateProxy();
    deployEtherPredicate();
    deployEtherPredicateProxy();
    deployMintableERC20Predicate();
    deployMintableERC20PredicateProxy();
    deployERC721Predicate();
    deployERC721PredicateProxy();
    deployMintableERC721Predicate();
    deployMintableERC721PredicateProxy();
    deployDummyERC20();
    deployDummyMintableERC20();
    deployDummyERC721();
    deployDummyMintableERC721();
  }

  @Test
  public void deployMerkle() throws IOException {
    Contract.Merkle.setAbi(DeployBase.getAbi(Contract.Merkle));
    Contract.Merkle.setCode(DeployBase.getCode(Contract.Merkle));
    String MerkleAddr = DeployBase.deploy(Contract.Merkle, blockingStubFull, account1PriKey);
    writeAddr(Contract.Merkle.getName(), MerkleAddr);
    System.out.println("MerkleAddr: " + MerkleAddr);
  }

  @Test
  public void deployECVerify() throws IOException {
    Contract.ECVerify.setAbi(DeployBase.getAbi(Contract.ECVerify));
    Contract.ECVerify.setCode(DeployBase.getCode(Contract.ECVerify));
    String ECVerifyAddr = DeployBase.deploy(Contract.ECVerify, blockingStubFull, account1PriKey);
    writeAddr(Contract.ECVerify.getName(), ECVerifyAddr);
    System.out.println("ECVerifyAddr: " + ECVerifyAddr);
  }

  @Test
  public void deployGovernance() throws IOException {
    Contract.Governance.setAbi(DeployBase.getAbi(Contract.Governance));
    Contract.Governance.setCode(DeployBase.getCode(Contract.Governance));
    String GovernanceAddr = DeployBase.deploy(Contract.Governance, blockingStubFull, account1PriKey);
    writeAddr(Contract.Governance.getName(), GovernanceAddr);
    System.out.println("GovernanceAddr: " + GovernanceAddr);
  }

  @Test
  public void deployGovernanceProxy() throws IOException {
    Contract.GovernanceProxy.setAbi(DeployBase.getAbi(Contract.GovernanceProxy));
    Contract.GovernanceProxy.setCode(DeployBase.getCode(Contract.GovernanceProxy));
    Contract.GovernanceProxy.setMethod("constructor(address)");
    Contract.GovernanceProxy.setParam("\"" + getAddr(Contract.Governance.getName()) + "\"");
    String GovernanceProxyAddr = DeployBase.deploy(Contract.GovernanceProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.GovernanceProxy.getName(), GovernanceProxyAddr);
    System.out.println("GovernanceProxyAddr: " + GovernanceProxyAddr);
  }

  @Test
  public void deployRegistry() throws IOException {
    Contract.Registry.setAbi(DeployBase.getAbi(Contract.Registry));
    Contract.Registry.setCode(DeployBase.getCode(Contract.Registry));
    Contract.Registry.setMethod("constructor(address)");
    Contract.Registry.setParam("\"" + getAddr(Contract.GovernanceProxy.getName()) + "\"");
    String RegistryAddr = DeployBase.deploy(Contract.Registry, blockingStubFull, account1PriKey);
    writeAddr(Contract.Registry.getName(), RegistryAddr);
    System.out.println("Registry: " + RegistryAddr);
  }

  @Test
  public void deployRootChain() throws IOException {
    Contract.RootChain.setAbi(DeployBase.getAbi(Contract.RootChain));
    Contract.RootChain.setCode(DeployBase.getCode(Contract.RootChain));
    String RootChainAddr = DeployBase.deploy(Contract.RootChain, blockingStubFull, account1PriKey);
    writeAddr(Contract.RootChain.getName(), RootChainAddr);
    System.out.println("RootChainAddr: " + RootChainAddr);
  }

  @Test
  public void deployRootChainProxy() throws IOException {
    Contract.RootChainProxy.setAbi(DeployBase.getAbi(Contract.RootChainProxy));
    Contract.RootChainProxy.setCode(DeployBase.getCode(Contract.RootChainProxy));
    Contract.RootChainProxy.setMethod("constructor(address,address,string)");
    Contract.RootChainProxy.setParam("\"" + getAddr(Contract.RootChain.getName()) + "\"" +
            ",\"" + getAddr(Contract.Registry.getName()) + "\",\"just-sun-001\"");
    String RootChainProxyAddr = DeployBase.deploy(Contract.RootChainProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.RootChainProxy.getName(), RootChainProxyAddr);
    System.out.println("RootChainProxyAddr: " + RootChainProxyAddr);
  }

  @Test
  public void deployStakeManager() throws IOException {
    Contract.StakeManager.setAbi(DeployBase.getAbi(Contract.StakeManager));
    Contract.StakeManager.setCode(DeployBase.getCode(Contract.StakeManager));
    String StakeManagerAddr = DeployBase.deploy(Contract.StakeManager, blockingStubFull, account1PriKey);
    writeAddr(Contract.StakeManager.getName(), StakeManagerAddr);
    System.out.println("StakeManagerAddr: " + StakeManagerAddr);
    System.out.println("code: " + Contract.StakeManager.getCode());
  }

  @Test
  public void deployStakeManagerProxy() throws IOException {
    Contract.StakeManagerProxy.setAbi(DeployBase.getAbi(Contract.StakeManagerProxy));
    Contract.StakeManagerProxy.setCode(DeployBase.getCode(Contract.StakeManagerProxy));
    Contract.StakeManagerProxy.setMethod("constructor(address)");
    Contract.StakeManagerProxy.setParam("\"" + getAddr(Contract.StakeManager.getName()) + "\"");
    String StakeManagerProxyAddr = DeployBase.deploy(Contract.StakeManagerProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.StakeManagerProxy.getName(), StakeManagerProxyAddr);
    System.out.println("StakeManagerProxyAddr: " + StakeManagerProxyAddr);
  }

  @Test
  public void deployStakingInfo() throws IOException {
    Contract.StakingInfo.setAbi(DeployBase.getAbi(Contract.StakingInfo));
    Contract.StakingInfo.setCode(DeployBase.getCode(Contract.StakingInfo));
    Contract.StakingInfo.setMethod("constructor(address)");
    Contract.StakingInfo.setParam("\"" + getAddr(Contract.Registry.getName()) + "\"");
    String StakingInfoAddr = DeployBase.deploy(Contract.StakingInfo, blockingStubFull, account1PriKey);
    writeAddr(Contract.StakingInfo.getName(), StakingInfoAddr);
    System.out.println("StakingInfo: " + StakingInfoAddr);
  }

  @Test
  public void deployStakingNFT() throws IOException {
    String tokenName = "BTT-NFT";
    String tokenSymbol = "BTT-NFT";
    Contract.StakingNFT.setAbi(DeployBase.getAbi(Contract.StakingNFT));
    Contract.StakingNFT.setCode(DeployBase.getCode(Contract.StakingNFT));
    Contract.StakingNFT.setMethod("constructor(string,string)");
    Contract.StakingNFT.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String StakingAddr = DeployBase.deploy(Contract.StakingNFT, blockingStubFull, account1PriKey);
    writeAddr(Contract.StakingNFT.getName(), StakingAddr);
    System.out.println("StakingNFT: " + StakingAddr);
  }

  @Test
  public void deployStateSender() throws IOException {
    Contract.StateSender.setAbi(DeployBase.getAbi(Contract.StateSender));
    Contract.StateSender.setCode(DeployBase.getCode(Contract.StateSender));
    String StateSenderAddr = DeployBase.deploy(Contract.StateSender, blockingStubFull, account1PriKey);
    writeAddr(Contract.StateSender.getName(), StateSenderAddr);
    System.out.println("StateSenderAddr: " + StateSenderAddr);
  }

  @Test
  public void deployValidatorShare() throws IOException {
    Contract.ValidatorShare.setAbi(DeployBase.getAbi(Contract.ValidatorShare));
    Contract.ValidatorShare.setCode(DeployBase.getCode(Contract.ValidatorShare));
    String ValidatorShareAddr = DeployBase.deploy(Contract.ValidatorShare, blockingStubFull, account1PriKey);
    writeAddr(Contract.ValidatorShare.getName(), ValidatorShareAddr);
    System.out.println("ValidatorShareAddr: " + ValidatorShareAddr);
  }

  @Test
  public void deployValidatorShareFactory() throws IOException {
    Contract.ValidatorShareFactory.setAbi(DeployBase.getAbi(Contract.ValidatorShareFactory));
    Contract.ValidatorShareFactory.setCode(DeployBase.getCode(Contract.ValidatorShareFactory));
    String ValidatorShareFactoryAddr = DeployBase.deploy(Contract.ValidatorShareFactory, blockingStubFull, account1PriKey);
    writeAddr(Contract.ValidatorShareFactory.getName(), ValidatorShareFactoryAddr);
    System.out.println("ValidatorShareFactoryAddr: " + ValidatorShareFactoryAddr);
  }

  @Test
  public void deployStakeManagerExtension() throws IOException {
    Contract.StakeManagerExtension.setAbi(DeployBase.getAbi(Contract.StakeManagerExtension));
    Contract.StakeManagerExtension.setCode(DeployBase.getCode(Contract.StakeManagerExtension));
    String StakeManagerExtensionAddr = DeployBase.deploy(Contract.StakeManagerExtension, blockingStubFull, account1PriKey);
    writeAddr(Contract.StakeManagerExtension.getName(), StakeManagerExtensionAddr);
    System.out.println("ValidatorShareFactoryAddr: " + StakeManagerExtensionAddr);
  }

  @Test
  public void deployEventsHub() throws IOException {
    Contract.EventsHub.setAbi(DeployBase.getAbi(Contract.EventsHub));
    Contract.EventsHub.setCode(DeployBase.getCode(Contract.EventsHub));
    String EventsHubAddr = DeployBase.deploy(Contract.EventsHub, blockingStubFull, account1PriKey);
    writeAddr(Contract.EventsHub.getName(), EventsHubAddr);
    System.out.println("EventsHubAddr: " + EventsHubAddr);
  }

  @Test
  public void deployEventsHubProxy() throws IOException {
    Contract.EventsHubProxy.setAbi(DeployBase.getAbi(Contract.EventsHubProxy));
    Contract.EventsHubProxy.setCode(DeployBase.getCode(Contract.EventsHubProxy));
    Contract.EventsHubProxy.setMethod("constructor(address)");
    Contract.EventsHubProxy.setParam("\"" + getAddr(Contract.EventsHub.getName()) + "\"");
    String EventsHubProxyAddr = DeployBase.deploy(Contract.EventsHubProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.EventsHubProxy.getName(), EventsHubProxyAddr);
    System.out.println("EventsHubProxyAddr: " + EventsHubProxyAddr);
  }

  @Test
  public void deployBtt() throws IOException {
    String tokenName = "BTT";
    String tokenSymbol = "BTT";
    Contract.Btt.setAbi(DeployBase.getAbi(Contract.Btt));
    Contract.Btt.setCode(DeployBase.getCode(Contract.Btt));
    Contract.Btt.setMethod("constructor(string,string)");
    Contract.Btt.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String BttAddr = DeployBase.deploy(Contract.Btt, blockingStubFull, account1PriKey);
    writeAddr(Contract.Btt.getName(), BttAddr);
    System.out.println("Btt: " + BttAddr);
  }

  @Test
  public void deployTronRootChainManager() throws IOException {
    Contract.TronRootChainManager.setAbi(DeployBase.getAbi(Contract.TronRootChainManager));
    Contract.TronRootChainManager.setCode(DeployBase.getCode(Contract.TronRootChainManager));
    String TronRootChainManagerAddr = DeployBase.deploy(Contract.TronRootChainManager, blockingStubFull, account1PriKey);
    writeAddr(Contract.TronRootChainManager.getName(), TronRootChainManagerAddr);
    System.out.println("TronRootChainManager: " + TronRootChainManagerAddr);
  }

  @Test
  public void deployRootChainManagerProxy() throws IOException {
    Contract.RootChainManagerProxy.setAbi(DeployBase.getAbi(Contract.RootChainManagerProxy));
    Contract.RootChainManagerProxy.setCode(DeployBase.getCode(Contract.RootChainManagerProxy));
    Contract.RootChainManagerProxy.setMethod("constructor(address)");
    Contract.RootChainManagerProxy.setParam("\"" + getAddr(Contract.TronRootChainManager.getName()) + "\"");
    String RootChainManagerProxyAddr = DeployBase.deploy(Contract.RootChainManagerProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.RootChainManagerProxy.getName(), RootChainManagerProxyAddr);
    System.out.println("RootChainManagerProxy: " + RootChainManagerProxyAddr);
  }

  @Test
  public void deployERC20Predicate() throws IOException {
    Contract.ERC20Predicate.setAbi(DeployBase.getAbi(Contract.ERC20Predicate));
    Contract.ERC20Predicate.setCode(DeployBase.getCode(Contract.ERC20Predicate));
    String ERC20PredicateAddr = DeployBase.deploy(Contract.ERC20Predicate, blockingStubFull, account1PriKey);
    writeAddr(Contract.ERC20Predicate.getName(), ERC20PredicateAddr);
    System.out.println("ERC20Predicate: " + ERC20PredicateAddr);
  }

  @Test
  public void deployERC20PredicateProxy() throws IOException {
    Contract.ERC20PredicateProxy.setAbi(DeployBase.getAbi(Contract.ERC20PredicateProxy));
    Contract.ERC20PredicateProxy.setCode(DeployBase.getCode(Contract.ERC20PredicateProxy));
    Contract.ERC20PredicateProxy.setMethod("constructor(address)");
    Contract.ERC20PredicateProxy.setParam("\"" + getAddr(Contract.ERC20Predicate.getName()) + "\"");
    String ERC20PredicateProxyAddr = DeployBase.deploy(Contract.ERC20PredicateProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.ERC20PredicateProxy.getName(), ERC20PredicateProxyAddr);
    System.out.println("ERC20PredicateProxy: " + ERC20PredicateProxyAddr);
  }

  @Test
  public void deployEtherPredicate() throws IOException {
    Contract.EtherPredicate.setAbi(DeployBase.getAbi(Contract.EtherPredicate));
    Contract.EtherPredicate.setCode(DeployBase.getCode(Contract.EtherPredicate));
    String EtherPredicateAddr = DeployBase.deploy(Contract.EtherPredicate, blockingStubFull, account1PriKey);
    writeAddr(Contract.EtherPredicate.getName(), EtherPredicateAddr);
    System.out.println("EtherPredicate: " + EtherPredicateAddr);
  }

  @Test
  public void deployEtherPredicateProxy() throws IOException {
    Contract.EtherPredicateProxy.setAbi(DeployBase.getAbi(Contract.EtherPredicateProxy));
    Contract.EtherPredicateProxy.setCode(DeployBase.getCode(Contract.EtherPredicateProxy));
    Contract.EtherPredicateProxy.setMethod("constructor(address)");
    Contract.EtherPredicateProxy.setParam("\"" + getAddr(Contract.EtherPredicate.getName()) + "\"");
    String EtherPredicateProxyAddr = DeployBase.deploy(Contract.EtherPredicateProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.EtherPredicateProxy.getName(), EtherPredicateProxyAddr);
    System.out.println("EtherPredicateProxy: " + EtherPredicateProxyAddr);
  }

  @Test
  public void deployMintableERC20Predicate() throws IOException {
    Contract.MintableERC20Predicate.setAbi(DeployBase.getAbi(Contract.MintableERC20Predicate));
    Contract.MintableERC20Predicate.setCode(DeployBase.getCode(Contract.MintableERC20Predicate));
    String MintableERC20PredicateAddr = DeployBase.deploy(Contract.MintableERC20Predicate, blockingStubFull, account1PriKey);
    writeAddr(Contract.MintableERC20Predicate.getName(), MintableERC20PredicateAddr);
    System.out.println("MintableERC20Predicate: " + MintableERC20PredicateAddr);
  }

  @Test
  public void deployMintableERC20PredicateProxy() throws IOException {
    Contract.MintableERC20PredicateProxy.setAbi(DeployBase.getAbi(Contract.MintableERC20PredicateProxy));
    Contract.MintableERC20PredicateProxy.setCode(DeployBase.getCode(Contract.MintableERC20PredicateProxy));
    Contract.MintableERC20PredicateProxy.setMethod("constructor(address)");
    Contract.MintableERC20PredicateProxy.setParam("\"" + getAddr(Contract.MintableERC20Predicate.getName()) + "\"");
    String MintableERC20PredicateProxyAddr = DeployBase.deploy(Contract.MintableERC20PredicateProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.MintableERC20PredicateProxy.getName(), MintableERC20PredicateProxyAddr);
    System.out.println("MintableERC20PredicateProxy: " + MintableERC20PredicateProxyAddr);
  }

  @Test
  public void deployERC721Predicate() throws IOException {
    Contract.ERC721Predicate.setAbi(DeployBase.getAbi(Contract.ERC721Predicate));
    Contract.ERC721Predicate.setCode(DeployBase.getCode(Contract.ERC721Predicate));
    String ERC721PredicateAddr = DeployBase.deploy(Contract.ERC721Predicate, blockingStubFull, account1PriKey);
    writeAddr(Contract.ERC721Predicate.getName(), ERC721PredicateAddr);
    System.out.println("ERC721Predicate: " + ERC721PredicateAddr);
  }

  @Test
  public void deployERC721PredicateProxy() throws IOException {
    Contract.ERC721PredicateProxy.setAbi(DeployBase.getAbi(Contract.ERC721PredicateProxy));
    Contract.ERC721PredicateProxy.setCode(DeployBase.getCode(Contract.ERC721PredicateProxy));
    Contract.ERC721PredicateProxy.setMethod("constructor(address)");
    Contract.ERC721PredicateProxy.setParam("\"" + getAddr(Contract.ERC721Predicate.getName()) + "\"");
    String ERC721PredicateProxyAddr = DeployBase.deploy(Contract.ERC721PredicateProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.ERC721PredicateProxy.getName(), ERC721PredicateProxyAddr);
    System.out.println("ERC721PredicateProxyAddr: " + ERC721PredicateProxyAddr);
  }

  @Test
  public void deployMintableERC721Predicate() throws IOException {
    Contract.MintableERC721Predicate.setAbi(DeployBase.getAbi(Contract.MintableERC721Predicate));
    Contract.MintableERC721Predicate.setCode(DeployBase.getCode(Contract.MintableERC721Predicate));
    String MintableERC721PredicateAddr = DeployBase.deploy(Contract.MintableERC721Predicate, blockingStubFull, account1PriKey);
    writeAddr(Contract.MintableERC721Predicate.getName(), MintableERC721PredicateAddr);
    System.out.println("MintableERC721Predicate: " + MintableERC721PredicateAddr);
  }

  @Test
  public void deployMintableERC721PredicateProxy() throws IOException {
    Contract.MintableERC721PredicateProxy.setAbi(DeployBase.getAbi(Contract.MintableERC721PredicateProxy));
    Contract.MintableERC721PredicateProxy.setCode(DeployBase.getCode(Contract.MintableERC721PredicateProxy));
    Contract.MintableERC721PredicateProxy.setMethod("constructor(address)");
    Contract.MintableERC721PredicateProxy.setParam("\"" + getAddr(Contract.MintableERC721Predicate.getName()) + "\"");
    String MintableERC721PredicateProxyAddr = DeployBase.deploy(Contract.MintableERC721PredicateProxy, blockingStubFull, account1PriKey);
    writeAddr(Contract.MintableERC721PredicateProxy.getName(), MintableERC721PredicateProxyAddr);
    System.out.println("MintableERC721PredicateProxyAddr: " + MintableERC721PredicateProxyAddr);
  }

  @Test
  public void deployDummyERC20() throws IOException {
    String tokenName = "DummyERC20";
    String tokenSymbol = "DummyERC20";
    Contract.DummyERC20.setAbi(DeployBase.getAbi(Contract.DummyERC20));
    Contract.DummyERC20.setCode(DeployBase.getCode(Contract.DummyERC20));
    Contract.DummyERC20.setMethod("constructor(string,string)");
    Contract.DummyERC20.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String DummyERC20Addr = DeployBase.deploy(Contract.DummyERC20, blockingStubFull, account1PriKey);
    writeAddr(Contract.DummyERC20.getName(), DummyERC20Addr);
    System.out.println("DummyERC20: " + DummyERC20Addr);
  }

  @Test
  public void deployDummyMintableERC20() throws IOException {
    String tokenName = "DummyMintable";
    String tokenSymbol = "DummyMintable";
    Contract.DummyMintableERC20.setAbi(DeployBase.getAbi(Contract.DummyMintableERC20));
    Contract.DummyMintableERC20.setCode(DeployBase.getCode(Contract.DummyMintableERC20));
    Contract.DummyMintableERC20.setMethod("constructor(string,string)");
    Contract.DummyMintableERC20.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String DummyMintableERC20Addr = DeployBase.deploy(Contract.DummyMintableERC20, blockingStubFull, account1PriKey);
    writeAddr(Contract.DummyMintableERC20.getName(), DummyMintableERC20Addr);
    System.out.println("DummyMintableERC20: " + DummyMintableERC20Addr);
  }

  @Test
  public void deployDummyERC721() throws IOException {
    String tokenName = "DummyERC721";
    String tokenSymbol = "DummyERC721";
    Contract.DummyERC721.setAbi(DeployBase.getAbi(Contract.DummyERC721));
    Contract.DummyERC721.setCode(DeployBase.getCode(Contract.DummyERC721));
    Contract.DummyERC721.setMethod("constructor(string,string)");
    Contract.DummyERC721.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String DummyERC721Addr = DeployBase.deploy(Contract.DummyERC721, blockingStubFull, account1PriKey);
    writeAddr(Contract.DummyERC721.getName(), DummyERC721Addr);
    System.out.println("DummyERC721Addr: " + DummyERC721Addr);
  }

  @Test
  public void deployDummyMintableERC721() throws IOException {
    String tokenName = "DummyMintable721";
    String tokenSymbol = "DummyMintable721";
    Contract.DummyMintableERC721.setAbi(DeployBase.getAbi(Contract.DummyMintableERC721));
    Contract.DummyMintableERC721.setCode(DeployBase.getCode(Contract.DummyMintableERC721));
    Contract.DummyMintableERC721.setMethod("constructor(string,string)");
    Contract.DummyMintableERC721.setParam("\"" + tokenName + "\"," + "\"" + tokenSymbol + "\"");
    String DummyMintableERC721Addr = DeployBase.deploy(Contract.DummyMintableERC721, blockingStubFull, account1PriKey);
    writeAddr(Contract.DummyMintableERC721.getName(), DummyMintableERC721Addr);
    System.out.println("DummyMintableERC721Addr: " + DummyMintableERC721Addr);
  }


  @Test
  public void parseAddr() {
    String Merkle = "TWL6XF4vhk83vUofv49p9BBqRWhy6zDYhQ";
    String ECVerify = "TASzF1Bg4k7G9Xy2v7uVcCpq1TRXUFML4q";
    System.out.println(ByteArray.toHexString(Commons.decodeFromBase58Check(Merkle)));
    System.out.println(ByteArray.toHexString(Commons.decodeFromBase58Check(ECVerify)));
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

  enum Contract {
    Merkle("Merkle", "", "", contractPath),
    ECVerify("ECVerify", "", "", contractPath),

    Governance("Governance", "", "", contractPath),
    // param 为 Governance 地址
    GovernanceProxy("GovernanceProxy", "constructor(address)", "\"TXRaSNutPTASMRzUhjefUa39jDYBR2s5GV\"", contractPath),

    // param 为 GovernanceProxy 地址
    Registry("Registry", "constructor(address)", "\"TSmxEWwSZAznjFKdfNyKTk2HnkJLNtPN8g\"", contractPath),

    RootChain("RootChain", "", "", contractPath),
    // param1 为 RootChain 地址， param2 为 Registry 地址， param3 暂定 1即可
    RootChainProxy("RootChainProxy", "constructor(address,address,string)", "\"TRunp592No7wyUVhzfMhhrTU89zrKhE2mc\",\"TZ4RDZH34i4SyQBsBs66psUtWSjgQh2M9X\",\"1\"", contractPath),

    StakeManager("StakeManager", "", "", contractPath),
    // param 为 StakeManager 地址
    StakeManagerProxy("StakeManagerProxy", "constructor(address)", "\"TCMY5UMQSwBUBgMPXNzScEHzekF2joG3em\"", contractPath),

    // StakingInfo 即为stakeManagerProxy初始化中的logger， param 为 Registry 地址
    StakingInfo("StakingInfo", "constructor(address)", "\"TDpw9ex15i3AAou6oKLCPiKVvM56uzhaE8\"", contractPath),

    // 参数为token name 和symbol，随意起
    StakingNFT("StakingNFT", "constructor(string,string)", "\"" + "nft-test" + "\"," + "\"" + "NFT-test" + "\"", contractPath),
    StateSender("StateSender", "", "", contractPath),
    ValidatorShare("ValidatorShare", "", "", contractPath),
    ValidatorShareFactory("ValidatorShareFactory", "", "", contractPath),
    StakeManagerExtension("StakeManagerExtension", "", "", contractPath),
    EventsHub("EventsHub", "", "", contractPath),
    EventsHubProxy("EventsHubProxy", "", "", contractPath),
    Btt("TestToken", "constructor(string,string)", "", contractPath),


    //   posPortalPath  //
    TronRootChainManager("TronRootChainManager", "", "", posPortalPath),
    RootChainManager("RootChainManager", "", "", posPortalPath),
    // 在tron上部署时，param为 TronRootChainManager 地址
    RootChainManagerProxy("RootChainManagerProxy", "constructor(address)", "\"TTe54WVmT2SjN4tqNR2mEh1LVE1NYV7gns\"", posPortalPath),

    ERC20Predicate("ERC20Predicate", "", "", posPortalPath),
    // param 为 ERC20Predicate地址
    ERC20PredicateProxy("ERC20PredicateProxy", "constructor(address)","\"TGHqMZ1ReRBVpi6Zk9oPgZWCp47KzMm89c\"", posPortalPath),

    EtherPredicate("EtherPredicate", "", "", posPortalPath),
    // param 为 EtherPredicate地址
    EtherPredicateProxy("EtherPredicateProxy", "constructor(address)", "\"TGgxh8uVFQaDPdXwhpLdBbPPbLpPAYd3jM\"", posPortalPath),

    MintableERC20Predicate("MintableERC20Predicate", "", "", posPortalPath),
    // 参数为 MintableERC20Predicate 地址
    MintableERC20PredicateProxy("MintableERC20PredicateProxy", "constructor(address)", "\"TPYpcP1o6Pa57GvKhGqrSd2kQV4bG2q4hQ\"", posPortalPath),

    ERC721Predicate("ERC721Predicate", "", "", posPortalPath),
    // 参数为 MintableERC20Predicate 地址
    ERC721PredicateProxy("ERC721PredicateProxy", "constructor(address)", "\"TPYpcP1o6Pa57GvKhGqrSd2kQV4bG2q4hQ\"", posPortalPath),

    MintableERC721Predicate("MintableERC721Predicate", "", "", posPortalPath),
    // 参数为 MintableERC20Predicate 地址
    MintableERC721PredicateProxy("MintableERC721PredicateProxy", "constructor(address)", "\"TPYpcP1o6Pa57GvKhGqrSd2kQV4bG2q4hQ\"", posPortalPath),


    // token
    DummyERC20("DummyERC20", "constructor(string,string)", "\"" + "test" + "\"," + "\"" + "test" + "\"", posPortalPath),
    DummyMintableERC20("DummyMintableERC20", "constructor(string,string)", "\"" + "test" + "\"," + "\"" + "test" + "\"", posPortalPath),

    DummyERC721("DummyERC721", "constructor(string,string)", "\"" + "test" + "\"," + "\"" + "test" + "\"", posPortalPath),
    DummyMintableERC721("DummyMintableERC721", "constructor(string,string)", "\"" + "test" + "\"," + "\"" + "test" + "\"", posPortalPath);


    private String name;
    private String method;
    private String param;
    private String abi;
    private String code;
    private String path;

    private Contract(String name, String method, String param, String path) {
      this.name = name;
      this.method = method;
      this.param = param;
      this.path = path + "/" + name + ".json";
    }

    public void setAbi(String abi) {
      this.abi = abi;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public void setMethod(String method) {
      this.method = method;
    }

    public void setParam(String param) {
      this.param = param;
    }

    public String getName() {
      return name;
    }

    public String getAbi() {
      return abi;
    }

    public String getCode() {
      return code;
    }

    public String getMethod() {
      return method;
    }

    public String getParam() {
      return param;
    }

    public String getPath() {
      return path;
    }

  }
}