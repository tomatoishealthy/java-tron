/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.actuator;

import static org.tron.core.config.Parameter.ChainSymbol.TRX_SYMBOL_BYTES;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.entity.Dec;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Commons;
import org.tron.common.utils.DecodeUtil;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.exception.BalanceInsufficientException;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ItemNotFoundException;
import org.tron.core.store.AccountStore;
import org.tron.core.store.AssetIssueStore;
import org.tron.core.store.AssetIssueV2Store;
import org.tron.core.store.DynamicPropertiesStore;
import org.tron.core.store.StableMarketStore;
import org.tron.core.utils.StableMarketUtil;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result.code;
import org.tron.protos.contract.StableMarketContract;
import org.tron.protos.contract.StableMarketContract.StableMarketExchangeContract;

@Slf4j(topic = "actuator")
public class StableMarketActuator extends AbstractActuator {

  private StableMarketUtil stableMarketUtil = new StableMarketUtil();

  public StableMarketActuator() {
    super(ContractType.StableMarketExchangeContract, StableMarketExchangeContract.class);
  }

  @Override
  public boolean execute(Object result) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) result;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AssetIssueStore assetIssueStore = chainBaseManager.getAssetIssueStore();
    AssetIssueV2Store assetIssueV2Store = chainBaseManager.getAssetIssueV2Store();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    try {
      StableMarketExchangeContract stableMarketContract = any.unpack(StableMarketExchangeContract.class);
      // get assetIssue
      byte[] fromAddress = stableMarketContract.getOwnerAddress().toByteArray();
      byte[] toAddress = stableMarketContract.getToAddress().toByteArray();
      byte[] sourceTokenId = stableMarketContract.getSourceAssetId().getBytes();
      byte[] destTokenId = stableMarketContract.getDestAssetId().getBytes();

      long offerAmount = stableMarketContract.getAmount();

      StableMarketContract.ExchangeResult exchangeResult = stableMarketUtil.computeSwap(sourceTokenId, destTokenId, offerAmount);
      long askAmount = exchangeResult.getAskAmount();
      long feeAmount = Dec.newDec(askAmount).mul(Dec.newDec(exchangeResult.getSpread())).roundLong();
      long askAmountSubFee = askAmount - feeAmount;
      // apply swap pool
      stableMarketUtil.applySwapPool(sourceTokenId, destTokenId, offerAmount, askAmountSubFee);

      // distribute reward
      chainBaseManager.getStableMarketStore().addOracleRewardPool(ByteArray.toStr(destTokenId), Dec.newDec(feeAmount));
      logger.info("stable: distribute exchange fee, token: {}, amount: {}", ByteArray.toStr(destTokenId), feeAmount);

      AccountCapsule ownerAccountCapsule = accountStore.get(fromAddress);
      AccountCapsule toAccountCapsule = accountStore.get(toAddress);
      if (toAccountCapsule == null) {
        boolean withDefaultPermission =
                dynamicStore.getAllowMultiSign() == 1;
        toAccountCapsule = new AccountCapsule(ByteString.copyFrom(toAddress), Protocol.AccountType.Normal,
                dynamicStore.getLatestBlockHeaderTimestamp(), withDefaultPermission, dynamicStore);
        accountStore.put(toAddress, toAccountCapsule);
        fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      }
      // adjust source balance
      if (Arrays.equals(sourceTokenId, TRX_SYMBOL_BYTES)) {
        Commons.adjustBalance(accountStore, ownerAccountCapsule, -offerAmount);
        stableMarketUtil.adjustTrxExchangeTotalAmount(-offerAmount);
      } else {
        if (!ownerAccountCapsule
                .reduceAssetAmountV2(sourceTokenId, offerAmount, dynamicStore, assetIssueStore)) {
          throw new ContractExeException("StableMarket reduceAssetAmount failed !");
        }
        accountStore.put(fromAddress, ownerAccountCapsule);
        // adjust total supply
        AssetIssueCapsule sourceAssetIssueCapsule = assetIssueV2Store.get(sourceTokenId);
        sourceAssetIssueCapsule.setTotalSupply(sourceAssetIssueCapsule.getTotalSupply() - offerAmount);
        assetIssueV2Store.put(sourceTokenId, sourceAssetIssueCapsule);
      }
      // adjust dest balance
      if (Arrays.equals(destTokenId, TRX_SYMBOL_BYTES)) {
        Commons.adjustBalance(accountStore, toAddress, askAmountSubFee);
        stableMarketUtil.adjustTrxExchangeTotalAmount(askAmount);
      } else {
        toAccountCapsule
                .addAssetAmountV2(destTokenId, askAmountSubFee, dynamicStore, assetIssueStore);
        accountStore.put(toAddress, toAccountCapsule);
        // adjust total supply
        AssetIssueCapsule destAssetIssueCapsule = assetIssueV2Store.get(destTokenId);
        destAssetIssueCapsule.setTotalSupply(destAssetIssueCapsule.getTotalSupply() + askAmount);
        assetIssueV2Store.put(destTokenId, destAssetIssueCapsule);
      }
      Commons.adjustBalance(accountStore, ownerAccountCapsule, -fee);
      if (dynamicStore.supportBlackHoleOptimization()) {
        dynamicStore.burnTrx(fee);
      } else {
        Commons.adjustBalance(accountStore, accountStore.getBlackhole(), fee);
      }
      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException | BalanceInsufficientException | ArithmeticException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }

    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException(ActuatorConstant.STORE_NOT_EXIST);
    }
    stableMarketUtil.init(chainBaseManager);
    DynamicPropertiesStore dynamicStore = chainBaseManager.getDynamicPropertiesStore();
    AccountStore accountStore = chainBaseManager.getAccountStore();
    StableMarketStore stableMarketStore = chainBaseManager.getStableMarketStore();
    if (!this.any.is(StableMarketExchangeContract.class)) {
      throw new ContractValidateException(
          "contract type error,expected type [StableMarketContract],real type[" + any
              .getClass() + "]");
    }
    // stable exchange should work after migrating to assetV2
    if (dynamicStore.getAllowSameTokenName() == 0) {
      throw new ContractValidateException("Stable coin exchange must be allowed after same token name opened");
    }

    if (dynamicStore.getAllowStableMarketOn() == 0) {
      throw new ContractValidateException("Stable Market not open.");
    }

    final StableMarketExchangeContract stableMarketExchangeContract;
    try {
      stableMarketExchangeContract = this.any.unpack(StableMarketExchangeContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }

    long fee = calcFee();
    byte[] ownerAddress = stableMarketExchangeContract.getOwnerAddress().toByteArray();
    byte[] toAddress = stableMarketExchangeContract.getToAddress().toByteArray();

    if (!DecodeUtil.addressValid(ownerAddress)) {
      throw new ContractValidateException("Invalid ownerAddress");
    }
    if (!DecodeUtil.addressValid(toAddress)) {
      throw new ContractValidateException("Invalid toAddress");
    }
    AccountCapsule ownerAccount = accountStore.get(ownerAddress);
    if (ownerAccount == null) {
      throw new ContractValidateException("No owner account!");
    }

    byte[] sourceAsset = stableMarketExchangeContract.getSourceAssetId().getBytes();
    byte[] destAsset = stableMarketExchangeContract.getDestAssetId().getBytes();
    long amount = stableMarketExchangeContract.getAmount();
    Dec sourceExchangeRate = stableMarketStore.getOracleExchangeRate(sourceAsset);
    Dec destExchangeRate = stableMarketStore.getOracleExchangeRate(destAsset);
    if (!Arrays.equals(TRX_SYMBOL_BYTES, sourceAsset) && sourceExchangeRate == null) {
      throw new ContractValidateException("source asset exchange rate not exist");
    }
    if (!Arrays.equals(TRX_SYMBOL_BYTES, destAsset) && destExchangeRate == null) {
      throw new ContractValidateException("dest asset exchange rate not exist");
    }
    long baseAmount = 0;
    try {
      baseAmount = stableMarketUtil.computeInternalSwap(
          sourceAsset, stableMarketUtil.getSDRTokenId(), amount).truncateLong();
    } catch (ItemNotFoundException e) {
      throw new ContractValidateException("baseAmount computeInternalSwap failed");
    }

    long maxExchangeAmount = stableMarketStore.getBasePool().quo(2).truncateLong();

    if (amount <= 0) {
      throw new ContractValidateException("Amount must be greater than 0.");
    }
    if (baseAmount >= maxExchangeAmount) {
      throw new ContractValidateException("Exchange amount is too large.");
    }

    if (Arrays.equals(sourceAsset, destAsset)) {
      throw new ContractValidateException("Source asset must not equal to dest asset!");
    }

    if (!stableMarketUtil.validateStable(sourceAsset)) {
      throw new ContractValidateException("No source asset!");
    }

    if (!stableMarketUtil.validateStable(destAsset)) {
      throw new ContractValidateException("No dest asset!");
    }

    // only worked on assetV2
    Long assetBalance;
    if (Arrays.equals(TRX_SYMBOL_BYTES, sourceAsset)) {
      assetBalance = ownerAccount.getBalance();
    } else {
      Map<String, Long> asset = ownerAccount.getAssetMapV2();
      if (asset.isEmpty()) {
        throw new ContractValidateException("Owner has no asset!");
      }
      assetBalance = asset.get(ByteArray.toStr(sourceAsset));
    }
    if (null == assetBalance || assetBalance <= 0) {
      throw new ContractValidateException("sourceAssetBalance must be greater than 0.");
    }
    if (amount > assetBalance) {
      throw new ContractValidateException("sourceAssetBalance is not sufficient.");
    }

    AccountCapsule toAccount = accountStore.get(toAddress);
    long offerAmount = 0;
    try {
      offerAmount = stableMarketUtil.computeInternalSwap(sourceAsset, destAsset, amount).truncateLong();
    } catch (ItemNotFoundException e) {
      throw new ContractValidateException("offerAmount computeInternalSwap failed.");
    }
    if (toAccount != null) {
      //after ForbidTransferToContract proposal, send trx to smartContract by actuator is not allowed.
      if (dynamicStore.getForbidTransferToContract() == 1
              && toAccount.getType() == Protocol.AccountType.Contract) {
        throw new ContractValidateException("Cannot exchange asset to smartContract.");
      }

      // todo: add unit test
      if(Arrays.equals(TRX_SYMBOL_BYTES, destAsset)) {
        assetBalance = toAccount.getBalance();
      } else {
        assetBalance = toAccount.getAssetMapV2().get(ByteArray.toStr(destAsset));
      }
      if (assetBalance != null && Long.MAX_VALUE - assetBalance < offerAmount) {
        throw new ContractValidateException("exchange amount overflow");
      }
    } else {
      fee = fee + dynamicStore.getCreateNewAccountFeeInSystemContract();
      if (ownerAccount.getBalance() < fee) {
        throw new ContractValidateException(
                "Validate StableMarketContract error, insufficient fee.");
      }
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(StableMarketExchangeContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}
