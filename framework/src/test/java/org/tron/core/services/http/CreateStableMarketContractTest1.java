package org.tron.core.services.http;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.collections.Maps;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.common.application.Application;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.AccountCapsule;
import org.tron.protos.Protocol;
import org.tron.protos.contract.StableMarketContract.StableMarketExchangeContract;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;

import static org.tron.common.utils.Commons.adjustBalance;
import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;

public class CreateStableMarketContractTest1 {

  private static String account1;
  private static String account2;
  private static String account3;
  private static String SOURCE_TOKEN_ID = "1000001";
  private static String DEST_TOKEN_ID = "1000002";
  private static long AMOUNT = 1_000L;

  private static TronApplicationContext context;
  private static String ip = "18.116.111.142";
  private static int fullHttpPort = 8090;
  private static Application appTest;
  private static CloseableHttpClient httpClient = HttpClients.createDefault();
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static final String pri1 = "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366";
  private static final String pri2 = "cba92a516ea09f620a16ff7ee95ce0df1d56550a8babe9964981a7144c8a784a";
  private static final String pri3 = "6781f44d9a2083b14fad1702b8e9ba82749162b795e2fc3f136192fc63f80de2";
  private static ECKey ecKey1 = ECKey.fromPrivate(Hex.decode(pri1));
  private static ECKey ecKey2 = ECKey.fromPrivate(Hex.decode(pri2));
  private static ECKey ecKey3 = ECKey.fromPrivate(Hex.decode(pri3));

  /**
   * init dependencies.
   */
  @BeforeClass
  public static void init() {
    account1 = ByteArray.toHexString(ecKey1.getAddress());
    account2 = ByteArray.toHexString(ecKey2.getAddress());
    account3 = ByteArray.toHexString(ecKey3.getAddress());

    String rpcNode = "18.116.111.142:50051";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(rpcNode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test
  public void testHttpCreateContract() throws IOException {
    String urlPath = "/wallet/createstablemarketexchange";
    String url = String.format("http://%s:%d%s", ip, fullHttpPort, urlPath);
    Map<String, Object> param = Maps.newHashMap();
    param.put("owner_address", account3);
    param.put("to_address", account2);
    param.put("source_asset_id", SOURCE_TOKEN_ID);
    param.put("dest_asset_id", DEST_TOKEN_ID);
    param.put("amount", 440562);
    String response = sendPostRequest(url, JsonUtil.obj2Json(param));
    System.out.println(account1);
    System.out.println(response);
    Map<String, Object> result = JsonUtil.json2Obj(response, Map.class);
    Assert.assertNotNull(result);
  }

  @Test
  public void testRpcCreateContract() {
    StableMarketExchangeContract stableMarketContract = StableMarketExchangeContract.newBuilder()
        // .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(TO_ADDRESS)))
        // .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
//        .setSourceTokenId(DEST_TOKEN_ID)
//        .setDestTokenId(TRX_SYMBOL)
        .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(account1)))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(account2)))
        .setSourceAssetId(DEST_TOKEN_ID)
        .setDestAssetId(TRX_SYMBOL)
        .setAmount(235134827)
        .build();
    GrpcAPI.TransactionExtention tx =
        blockingStubFull.createStableMarketExchange(stableMarketContract);
    Assert.assertNotNull(tx);

    Protocol.Transaction transaction = tx.getTransaction();
    transaction = signTransaction(ecKey1, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    System.out.println(response);
  }

  @Test
  public void performanceTest() {

    ECKey[] accountList = new ECKey[]{ecKey1, ecKey2, ecKey3};
    String[] tokenIdList = new String[]{TRX_SYMBOL, SOURCE_TOKEN_ID, DEST_TOKEN_ID};

    while (true) {
      int fromIndex = new Random().nextInt(3);
      int toIndex = new Random().nextInt(3);
      int fromAssetIndex = new Random().nextInt(3);
      int toAssetIndex = (new Random().nextInt(2) + fromAssetIndex + 1) % 3;


      ECKey from = accountList[fromIndex];
      ECKey to = accountList[toIndex];

      Protocol.Account fromAccountRequest = Protocol.Account.newBuilder()
          .setAddress(ByteString.copyFrom(from.getAddress())).build();
      Protocol.Account toAccountRequest = Protocol.Account.newBuilder()
          .setAddress(ByteString.copyFrom(to.getAddress())).build();
      Protocol.Account fromAccount = blockingStubFull.getAccount(fromAccountRequest);
      Protocol.Account toAccount = blockingStubFull.getAccount(toAccountRequest);

      String fromToken = tokenIdList[fromAssetIndex];
      String toToken = tokenIdList[toAssetIndex];
      Map<String, Long> fromAccountAsset = fromAccount.getAssetV2Map();
      Map<String, Long> toAccountAsset = toAccount.getAssetV2Map();
      long fromBalance = 0;
      if (TRX_SYMBOL.equals(fromToken)) {
        fromBalance = fromAccount.getBalance();
      } else {
        if (fromAccountAsset.get(fromToken) != null && fromAccountAsset.get(fromToken) > 0) {
          fromBalance = fromAccountAsset.get(fromToken);
        } else {
          continue;
        }
      }
      long amount = new Random().nextInt((int)Math.min(fromBalance, 1000000));
      StableMarketExchangeContract stableMarketContract = StableMarketExchangeContract.newBuilder()
          .setOwnerAddress(fromAccount.getAddress())
          .setToAddress(toAccount.getAddress())
          .setSourceAssetId(fromToken)
          .setDestAssetId(toToken)
          .setAmount(amount)
          .build();
      GrpcAPI.TransactionExtention tx =
          blockingStubFull.createStableMarketExchange(stableMarketContract);
      Assert.assertNotNull(tx);


      Protocol.Transaction transaction = tx.getTransaction();
      transaction = signTransaction(from, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      System.out.println("id: " + Sha256Hash.of(true, transaction.getRawData().toByteArray()));
      System.out.println("from: " + ByteArray.toHexString(from.getAddress()));
      System.out.println("to: " + ByteArray.toHexString(to.getAddress()));
      System.out.println("source: " + fromToken + " , dest: " + toToken + ", amount: " + amount);
      System.out.println(response);
//      try {
//        Thread.sleep(300);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
    }
  }

  private String sendPostRequest(String url, String body) throws IOException {
    HttpPost request = new HttpPost(url);
    request.setHeader("User-Agent", "Java client");
    StringEntity entity = new StringEntity(body);
    request.setEntity(entity);
    HttpResponse response = httpClient.execute(request);
    BufferedReader rd = new BufferedReader(
        new InputStreamReader(response.getEntity().getContent()));
    StringBuffer result = new StringBuffer();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return result.toString();
  }

  /**
   * Set public for future use.
   * @param ecKey ecKey of the private key
   * @param transaction transaction object
   */
  public static Protocol.Transaction signTransaction(ECKey ecKey,
                                                     Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * Set public for future use.
   * @param transaction transaction object
   * @param blockingStubFull Grpc interface
   */
  public static GrpcAPI.Return broadcastTransaction(
      Protocol.Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == GrpcAPI.Return.response_code.SERVER_BUSY
        && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
    }
    return response;
  }

}