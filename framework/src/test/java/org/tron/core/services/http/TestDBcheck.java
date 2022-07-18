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
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.StableMarketContract.StableMarketExchangeContract;
import stest.tron.wallet.common.client.utils.TransactionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL;

public class TestDBcheck {

  private static String account1;
  private static String account2;
  private static String account3;
  private static String SOURCE_TOKEN_ID = "1000001";
  private static String DEST_TOKEN_ID = "1000002";
  private static long AMOUNT = 1_000L;

  private static TronApplicationContext context;
  private static String ip = "127.0.0.1";
  private static int fullHttpPort = 8090;
  private static Application appTest;
  private static CloseableHttpClient httpClient = HttpClients.createDefault();
  private static WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private static final String pri1 = "D95611A9AF2A2A45359106222ED1AFED48853D9A44DEFF8DC7913F5CBA727366";
  private static ECKey ecKey1 = ECKey.fromPrivate(Hex.decode(pri1));

  /**
   * init dependencies.
   */
  @BeforeClass
  public static void init() {
    account1 = ByteArray.toHexString(ecKey1.getAddress());

    String rpcNode = "127.0.0.1:50051";
    ManagedChannel channelFull = ManagedChannelBuilder.forTarget(rpcNode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }


  @Test
  public void testRpcCreateContract() {
    long amount = 1;
    while (true) {
      ECKey to = new ECKey(Utils.getRandom());
      BalanceContract.TransferContract transferContract = BalanceContract.TransferContract.newBuilder()
          .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(account1)))
          .setToAddress(ByteString.copyFrom(to.getAddress()))
          .setAmount(amount++)
          .build();
      GrpcAPI.TransactionExtention tx =
          blockingStubFull.createTransaction2(transferContract);
      Assert.assertNotNull(tx);

      Protocol.Transaction transaction = tx.getTransaction();
      transaction = signTransaction(ecKey1, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      System.out.println(response);
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