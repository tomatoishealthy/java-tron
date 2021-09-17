package org.tron.core.ibc;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Strings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import org.tron.common.utils.StringUtil;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;


public class DeployUtil {
  // node conf
  //private String fromFullnode = "47.94.0.13:50054";
  private String fullnode = "47.252.19.181:50051";

  // account
  private String account1PriKey = "";   // nile

  private String Path = "/Users/quan/tron/solc/build/";

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

  public String deploy(Contract contract) throws IOException {
    Code code = parseSolidityFile(Path+contract.name+".txt", contract.name);
    //Code code = parseSolidityFile("/Users/quan/tron/solc/a.txt", contract.name);
    System.out.println(code);
    long maxFeeLimit = 5000000000l;
    long originEnergyLimit = 1000000000;

    String contractAddress;
    if (contract.method.isEmpty()) {
      byte[] bytes = PublicMethed.deployContract(contract.name, code.abi, code.code, "", maxFeeLimit,
              0L, 0, originEnergyLimit, "0",
              0, null, account1PriKey, getAddress(account1PriKey),
              blockingStubFull);
      contractAddress = StringUtil.encode58Check(bytes);
    } else {
      System.out.println("deploy with param");
      contractAddress = PublicMethed.deployContractWithConstantParame(contract.name, code.abi, code.code, contract.method, contract.param, "", maxFeeLimit,
              0L, 0, originEnergyLimit, "0",
              0, null, account1PriKey, getAddress(account1PriKey),
              blockingStubFull);
    }

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    return contractAddress;
  }

  public Code parseSolidityFile(String file, String identity) throws IOException {
    String abi = null;
    String bytecode = null;

    BufferedReader in = new BufferedReader(new FileReader(file));
    String str;
    Code code = new Code();
    while ((str = in.readLine()) != null) {
      if (str.contains(":"+identity+" =======")) {
        str = in.readLine();
        if ("Binary:".equals(str)) {
          bytecode = in.readLine();
          if (Strings.isNullOrEmpty(bytecode)) {
            System.out.println("bytecode is null");
            return null;
          }
          code.setCode(bytecode);
        }
        // read abi
        str = in.readLine();
        if ("Contract JSON ABI".equals(str)) {
          abi = in.readLine();
          if (Strings.isNullOrEmpty(abi)) {
            System.out.println("abi is null");
            return null;
          }
          code.setAbi(abi);
        }
      }
    }
    return code;
  }

  /**
   * 执行本方法部署对应的合约
   * 部署时检查一下对应的枚举对象中是否需要替换参数
   * 替换规则参看方法介绍
   */
  @Test
  public void deployUtil() throws IOException {
    //ERC20Predicate = deploy(Contract.ERC20Predicate);
    //deploy(Contract.ERC20Predicate);
    //deploy(Contract.ERC20PredicateProxy);
    deploy(Contract.EtherPredicate);
    //deploy(Contract.EtherPredicateProxy);
    //deploy(Contract.Governance);
    //deploy(Contract.GovernanceProxy);
    //deploy(Contract.Registry);
    //deploy(Contract.RootChain);
    //deploy(Contract.RootChainProxy);
    //deploy(Contract.RootChainManager);
    //deploy(Contract.TronRootChainManager);
    //deploy(Contract.RootChainManagerProxy);
    //deploy(Contract.StakeManager);
    //deploy(Contract.StakeManagerProxy);
    //deploy(Contract.StakingInfo);
    //deploy(Contract.StakingNFT);
    //deploy(Contract.StateSender);
    //deploy(Contract.ValidatorShare);
    //deploy(Contract.ValidatorShareFactory);
    //deploy(Contract.StakeManagerExtension);
    //deploy(Contract.MintableERC20Predicate);
    //deploy(Contract.MintableERC20PredicateProxy);
  }

  @Data
  class Code {
    String abi;
    String code;
  }

  /**
   * 每个对象中第二个参数method为空的合约，第三个param参数也为空。
   * 合约中的param为参考示例，根据部署时生成的对应的地址进行替换
   */
  enum Contract {
    Merkle("Merkle", "", ""),
    ECVerify("ECVerify", "", ""),

    ERC20Predicate("ERC20Predicate", "", ""),
    // param 为 ERC20Predicate地址
    ERC20PredicateProxy("ERC20PredicateProxy", "constructor(address)","\"TGHqMZ1ReRBVpi6Zk9oPgZWCp47KzMm89c\""),

    EtherPredicate("EtherPredicate", "", ""),
    // param 为 EtherPredicate地址
    EtherPredicateProxy("EtherPredicateProxy", "constructor(address)", "\"TGgxh8uVFQaDPdXwhpLdBbPPbLpPAYd3jM\""),

    Governance("Governance", "", ""),
    // param 为 Governance 地址
    GovernanceProxy("GovernanceProxy", "constructor(address)", "\"TXRaSNutPTASMRzUhjefUa39jDYBR2s5GV\""),

    // param 为 GovernanceProxy 地址
    Registry("Registry", "constructor(address)", "\"TSmxEWwSZAznjFKdfNyKTk2HnkJLNtPN8g\""),

    RootChain("RootChain", "", ""),
    // param1 为 RootChain 地址， param2 为 Registry 地址， param3 暂定 1即可
    RootChainProxy("RootChainProxy", "constructor(address,address,string)", "\"TRunp592No7wyUVhzfMhhrTU89zrKhE2mc\",\"TZ4RDZH34i4SyQBsBs66psUtWSjgQh2M9X\",\"1\""),

    TronRootChainManager("RootChainManager", "", ""),
    RootChainManager("RootChainManager", "", ""),
    // 在tron上部署时，param为 TronRootChainManager 地址
    RootChainManagerProxy("RootChainManagerProxy", "constructor(address)", "\"TTe54WVmT2SjN4tqNR2mEh1LVE1NYV7gns\""),

    StakeManager("StakeManager", "", ""),
    // param 为 StakeManager 地址
    StakeManagerProxy("StakeManagerProxy", "constructor(address)", "\"TCMY5UMQSwBUBgMPXNzScEHzekF2joG3em\""),

    // StakingInfo 即为stakeManagerProxy初始化中的logger， param 为 Registry 地址
    StakingInfo("StakingInfo", "constructor(address)", "\"TDpw9ex15i3AAou6oKLCPiKVvM56uzhaE8\""),

    // 参数为token name 和symbol，随意起
    StakingNFT("StakingNFT", "constructor(string,string)", "\"" + "nft-test" + "\"," + "\"" + "NFT-test" + "\""),
    StateSender("StateSender", "", ""),
    ValidatorShare("ValidatorShare", "", ""),
    ValidatorShareFactory("ValidatorShareFactory", "", ""),
    StakeManagerExtension("StakeManagerExtension", "", ""),

    MintableERC20Predicate("MintableERC20Predicate", "", ""),
    // 参数为 MintableERC20Predicate 地址
    MintableERC20PredicateProxy("MintableERC20PredicateProxy", "constructor(address)", "\"TPYpcP1o6Pa57GvKhGqrSd2kQV4bG2q4hQ\"");

    private String name;
    private String method;
    private String param;

    private Contract(String name, String method, String param) {
      this.name = name;
      this.method = method;
      this.param = param;
    }

  }


  @Test
  public void parseAddr() {
    String Merkle = "TWL6XF4vhk83vUofv49p9BBqRWhy6zDYhQ";
    String ECVerify = "TASzF1Bg4k7G9Xy2v7uVcCpq1TRXUFML4q";
    System.out.println(ByteArray.toHexString(Commons.decodeFromBase58Check(Merkle)));
    System.out.println(ByteArray.toHexString(Commons.decodeFromBase58Check(ECVerify)));
  }
}