package org.tron.core.ibc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Before;
import org.junit.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.core.Wallet;
import stest.tron.wallet.common.client.Parameter;
import stest.tron.wallet.common.client.utils.AbiUtil;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.Arrays;


public class TriggerUtil {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = "47.252.19.181:50051";
  private String contractAddress = "TMQ3Nfmmn7uFMDnjmT98sAPfdtaPsjFFDD";  // addr: validatorShare
  //private String contractAddress = "TCPgDUVZoamSbY9YXJ3dnAyfNC1M6Mqrzt";

  private String account1PriKey = "4df12b6b37734c521eadc4ce5811f27f40e8bae8d43d32804dbf580d40aebcd7";
  private ECKey ecKey1 = ECKey.fromPrivate(ByteArray.fromHexString(account1PriKey));
  private byte[] account1Address = ecKey1.getAddress();

  /**
   * constructor.
   */
  @Before
  public void beforeClass() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(Parameter.CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    PublicMethed.printAddress(account1PriKey);
  }

    @Test
    public void exchangeRate() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "exchangeRate()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 获取指定用户的质押量
     */
    @Test
    public void getTotalStake() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "getTotalStake(address)";
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList("TAHogs4YMkfeumq82g9GTW7cwGcuYpXSCe"));
        //String argsStr = "0x7AF2802CC3F75F91EBA975D28857A0D55245F0AE";   // 0x 开头的地址
        //String argsStr = "TAHogs4YMkfeumq82g9GTW7cwGcuYpXSCe";   // 0x 开头的地址
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 查询质押量和质押凭证比例
     */
    @Test
    public void withdrawExchangeRate() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "withdrawExchangeRate()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 查询用户奖励
     */
    @Test
    public void getLiquidRewards() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "getLiquidRewards(address)";
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList("TAHogs4YMkfeumq82g9GTW7cwGcuYpXSCe"));
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 查询每个凭证目前的reward值
     */
    @Test
    public void getRewardPerShare() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "getRewardPerShare()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 质押，第一个是质押的btt数量， 第二个是希望获取的最少质押凭证个数。 第二个值应小于等于第一个值
     */
    @Test
    public void buyVoucher() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "buyVoucher(uint256,uint256)";  // 第一个是质押的btt数量， 第二个是希望获取的最少质押凭证个数。 第二个值应小于等于第一个值
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList(100, 50));
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    /**
     * 提取奖励，默认发起人
     */
    @Test
    public void withdrawRewards() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "withdrawRewards()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    @Test
    public void unstakeClaimTokens() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "unstakeClaimTokens()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    @Test
    public void restake() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "restake()";
        String argsStr = "";
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        false, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }


    @Test
    public void sellVoucher_new() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "sellVoucher_new(uint256,uint256)";  // 第一个是收回质押的btt数量， 第二个是希望燃烧的最大质押凭证个数。 第二个值应大于等于第一个值
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList(50, 100));
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    @Test
    public void unstakeClaimTokens_new() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
        String methodStr = "unstakeClaimTokens_new(uint256)";  // 调用sellVoucher_new交易日志中的unbondNonce值
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList(50));
        System.out.println(
                PublicMethed.triggerContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }

    @Test
    public void balanceOf() {
        channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
        blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

        String methodStr = "balanceOf(address)";
        String argsStr = AbiUtil.parseParameters(methodStr, Arrays.asList("TAHogs4YMkfeumq82g9GTW7cwGcuYpXSCe"));
        System.out.println(
                PublicMethed.triggerConstantContract(Commons.decodeFromBase58Check(contractAddress), methodStr, argsStr,
                        true, 0, 100000000L, "0", 0,
                        account1Address, account1PriKey, blockingStubFull)
        );
    }
}
