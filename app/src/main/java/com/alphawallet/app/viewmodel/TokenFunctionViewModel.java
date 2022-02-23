package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;
import com.alphawallet.app.ui.Erc20DetailActivity;
import com.alphawallet.app.ui.FunctionActivity;
import com.alphawallet.app.ui.MyAddressActivity;
import com.alphawallet.app.ui.RedeemAssetSelectActivity;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.ui.TransferNFTActivity;
import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.ui.widget.entity.TicketRangeParcel;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.ContractAddress;
import com.alphawallet.token.entity.FunctionDefinition;
import com.alphawallet.token.entity.MethodArg;
import com.alphawallet.token.entity.SigReturnType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.TokenscriptElement;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.TokenDefinition;

import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.core.methods.response.EthEstimateGas;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

import javax.inject.Inject;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
@HiltViewModel
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private final OpenSeaService openseaService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AnalyticsServiceType analyticsService;
    private Wallet wallet;

    @Nullable
    private Disposable calcGasCost;

    private final MutableLiveData<Token> insufficientFunds = new MutableLiveData<>();
    private final MutableLiveData<String> invalidAddress = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();
    private final MutableLiveData<Wallet> walletUpdate = new MutableLiveData<>();
    private final MutableLiveData<TransactionData> transactionFinalised = new MutableLiveData<>();
    private final MutableLiveData<Throwable> transactionError = new MutableLiveData<>();
    private final MutableLiveData<Web3Transaction> gasEstimateComplete = new MutableLiveData<>();

    @Inject
    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract,
            OpenSeaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract,
            AnalyticsServiceType analyticsService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.keyService = keyService;
        this.genericWalletInteract = genericWalletInteract;
        this.openseaService = openseaService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.analyticsService = analyticsService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
    public LiveData<Token> insufficientFunds() { return insufficientFunds; }
    public LiveData<String> invalidAddress() { return invalidAddress; }
    public LiveData<XMLDsigDescriptor> sig() { return sig; }
    public LiveData<Wallet> walletUpdate() { return walletUpdate; }
    public LiveData<Boolean> newScriptFound() { return newScriptFound; }
    public MutableLiveData<TransactionData> transactionFinalised() { return transactionFinalised; }
    public MutableLiveData<Throwable> transactionError() { return transactionError; }
    public MutableLiveData<Web3Transaction> gasEstimateComplete() { return gasEstimateComplete; }

    public void prepare()
    {
        getCurrentWallet();
    }

    public void openUniversalLink(Context context, Token token, List<BigInteger> selection) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(selection, false));
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(C.EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void startGasPriceUpdate(long chainId)
    {
        gasService.startGasPriceCycle(chainId);
    }

    public void showTransferToken(Context ctx, Token token, List<BigInteger> selection)
    {
        Intent intent = new Intent(ctx, TransferTicketDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());

        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(selection, false));

        if (token.isERC721()) //skip numerical selection - ERC721 has no multiple token transfer
        {
            intent.putExtra(C.EXTRA_STATE, TRANSFER_TO_ADDRESS.ordinal());
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void showFunction(Context ctx, Token token, String method, List<BigInteger> tokenIds)
    {
        Intent intent = new Intent(ctx, FunctionActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_STATE, method);
        if (tokenIds == null) tokenIds = new ArrayList<>(Collections.singletonList(BigInteger.ZERO));
        intent.putExtra(C.EXTRA_TOKEN_ID, Utils.bigIntListToString(tokenIds, true));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void checkTokenScriptValidity(Token token)
    {
        disposable = assetDefinitionService.getSignatureData(token.tokenInfo.chainId, token.tokenInfo.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig::postValue, this::onSigCheckError);
    }

    private void onSigCheckError(Throwable throwable)
    {
        XMLDsigDescriptor failSig = new XMLDsigDescriptor();
        failSig.result = "fail";
        failSig.type = SigReturnType.NO_TOKENSCRIPT;
        failSig.subject = throwable.getMessage();
        sig.postValue(failSig);
    }

    public void signMessage(Signable message, DAppFunction dAppFunction, long chainId) {
        disposable = createTransactionInteract.sign(wallet, message, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig.signature, message),
                        error -> dAppFunction.DAppError(error, message));
    }

    public String getTransactionBytes(Token token, BigInteger tokenId, FunctionDefinition def)
    {
        return assetDefinitionService.generateTransactionPayload(token, tokenId, def);
    }

    public TokenScriptResult getTokenScriptResult(Token token, BigInteger tokenId)
    {
        return assetDefinitionService.getTokenScriptResult(token, tokenId);
    }

    public BigInteger calculateMinGasPrice(BigInteger oldGasPrice)
    {
        //get 0.1GWEI in wei
        BigInteger zeroPointOneWei = BalanceUtils.gweiToWei(BigDecimal.valueOf(0.1));
        return new BigDecimal(oldGasPrice).multiply(BigDecimal.valueOf(1.1)).setScale(18, RoundingMode.UP).toBigInteger()
                .add(zeroPointOneWei);
    }

    public Token getToken(long chainId, String contractAddress)
    {
        return tokensService.getToken(chainId, contractAddress);
    }

    public void selectRedeemToken(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.Key.TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void stopGasSettingsFetch()
    {
        gasService.stopGasPriceCycle();
    }

    public void getAuthorisation(Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public Token getCurrency(long chainId)
    {
        return tokensService.getToken(chainId, wallet.address);
    }

    public void getCurrentWallet()
    {
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet w) {
        progress.postValue(false);
        wallet = w;
        walletUpdate.postValue(w);
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }

    public void completeAuthentication(Operation signData)
    {
        keyService.completeAuthentication(signData);
    }

    public void failedAuthentication(Operation signData)
    {
        keyService.failedAuthentication(signData);
    }

    public void showContractInfo(Context ctx, Token token)
    {
        Intent intent = new Intent(ctx, MyAddressActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void selectRedeemTokens(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.Key.TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void sellTicketRouter(Context context, Token token, List<BigInteger> idList) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(idList, false));
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(C.EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public Web3Transaction handleFunction(TSAction action, BigInteger tokenId, Token token, Context context)
    {
        String functionEffect = action.function.method;
        if (action.function.tx != null && (action.function.method == null || action.function.method.length() == 0)
                && (action.function.parameters == null || action.function.parameters.size() == 0))
        {
            //no params, this is a native style transaction
            return NativeSend(action, token, context);
        }
        else
        {
            //what's selected?
            ContractAddress cAddr = new ContractAddress(action.function);
            String functionData = getTransactionBytes(token, tokenId, action.function);
            if (functionData == null) return null;
            //function call may include some value
            String value = "0";
            if (action.function.tx != null && action.function.tx.args.containsKey("value"))
            {
                TokenscriptElement arg = action.function.tx.args.get("value");
                //resolve reference
                value = assetDefinitionService.resolveReference(token, action, arg, tokenId);
                Token currency = getCurrency(token.tokenInfo.chainId);
                functionEffect = BalanceUtils.getScaledValue(value, 18, Token.TOKEN_BALANCE_PRECISION)
                        + " " + currency.getSymbol() + " to " + action.function.method;
            }
            else
            {
                //Form full method representation
                functionEffect += "(";
                boolean firstArg = true;
                for (MethodArg arg : action.function.parameters)
                {
                    if (!firstArg) functionEffect += ", ";
                    firstArg = false;
                    if (arg.element.ref.equals("tokenId"))
                    {
                        functionEffect += "TokenId";
                    }
                    else
                    {
                        functionEffect += arg.element.value;
                    }
                }

                functionEffect += ")";
            }

            //finished resolving attributes, blank definition cache so definition is re-loaded when next needed
            getAssetDefinitionService().clearCache();

            return buildWeb3Transaction(functionData, cAddr.address, functionEffect, value);
        }
    }

    private Web3Transaction NativeSend(TSAction action, Token token, Context context)
    {
        boolean isValid = true;

        //calculate native amount
        BigDecimal value = new BigDecimal(action.function.tx.args.get("value").value);
        //this is a native send, so check the native currency
        Token currency = getCurrency(token.tokenInfo.chainId);

        if (currency.balance.subtract(value).compareTo(BigDecimal.ZERO) < 0)
        {
            //flash up dialog box insufficent funds
            insufficientFunds.postValue(currency);
            isValid = false;
        }

        //is 'to' overridden?
        String to = null;
        if (action.function.tx.args.get("to") != null)
        {
            to = action.function.tx.args.get("to").value;
        }
        else if (action.function.contract.addresses.get(token.tokenInfo.chainId) != null)
        {
            to = action.function.contract.addresses.get(token.tokenInfo.chainId).get(0);
        }

        if (to == null || !Utils.isAddressValid(to))
        {
            invalidAddress.postValue(to);
            isValid = false;
        }

        String valCorrected = BalanceUtils.getScaledValue(value, token.tokenInfo.decimals, Token.TOKEN_BALANCE_PRECISION);

        //eg Send 2(*1) ETH(*2) to Alex's Amazing Coffee House(*3) (0xdeadacec0ffee(*4))
        String extraInfo = String.format(context.getString(R.string.tokenscript_send_native), valCorrected, token.getSymbol(), action.function.method, to);

        //Clear the cache to refresh any resolved values
        getAssetDefinitionService().clearCache();

        if (isValid)
        {
            return new Web3Transaction(
                    new Address(to),
                    new Address(token.getAddress()),
                    value.toBigInteger(),
                    BigInteger.ZERO,
                    BigInteger.valueOf(C.GAS_LIMIT_MIN),
                    -1,
                    "0x",
                    extraInfo
            );
        }
        else
        {
            return null;
        }
    }

    private Web3Transaction buildWeb3Transaction(String functionData,
                                    String contractAddress, String additionalDetails, String value)
    {
        return new Web3Transaction(
                new Address(contractAddress),
                new Address(contractAddress),
                new BigInteger(value),
                BigInteger.ZERO,
                BigInteger.ZERO, // notify that we need to calculate gaslimit
                -1,
                functionData,
                additionalDetails
        );
    }

    public void onDestroy()
    {
        if (calcGasCost != null && !calcGasCost.isDisposed()) { calcGasCost.dispose(); }
    }

    public OpenSeaService getOpenseaService()
    {
        return openseaService;
    }

    public void updateTokenScriptViewSize(Token token, int itemViewHeight)
    {
        assetDefinitionService.storeTokenViewHeight(token.tokenInfo.chainId, token.getAddress(), itemViewHeight);
    }

    public void checkForNewScript(Token token)
    {
        //check server for new tokenscript
        assetDefinitionService.checkServerForScript(token.tokenInfo.chainId, token.getAddress())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.single())
                .subscribe(this::handleDefinition, this::onError)
                .isDisposed();
    }

    private void handleDefinition(TokenDefinition td)
    {
        if (!TextUtils.isEmpty(td.holdingToken)) newScriptFound.postValue(true);
    }

    public boolean isAuthorizeToFunction()
    {
        return wallet.type != WalletType.WATCH;
    }

    public Wallet getWallet()
    {
        return wallet;
    }

    public Realm getRealmInstance(Wallet w)
    {
        return tokensService.getRealmInstance(w);
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public FetchTransactionsInteract getTransactionsInteract()
    {
        return fetchTransactionsInteract;
    }

    public Transaction fetchTransaction(String txHash)
    {
        return fetchTransactionsInteract.fetchCached(wallet.address, txHash);
    }

    public long fetchExpectedTxTime(String txHash)
    {
        return fetchTransactionsInteract.fetchTxCompletionTime(wallet.address, txHash);
    }

    public void estimateGasLimit(Web3Transaction w3tx, long chainId)
    {
        calcGasCost = gasService.calculateGasEstimate(Numeric.hexStringToByteArray(w3tx.payload), chainId, w3tx.contract.toString(), w3tx.value, wallet, BigInteger.ZERO)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(estimate -> buildNewConfirmation(estimate, w3tx),
                        error -> buildNewConfirmation(BigInteger.ZERO, w3tx)); //node didn't like this tx
    }

    private void buildNewConfirmation(BigInteger estimate, Web3Transaction w3tx)
    {
        gasEstimateComplete.postValue(new Web3Transaction(
                w3tx.recipient, w3tx.contract, w3tx.value, w3tx.gasPrice, estimate, w3tx.nonce, w3tx.payload, w3tx.description));
    }
    
    @Override
    public void showErc20TokenDetail(Activity context, @NotNull String address, String symbol, int decimals, @NotNull Token token)
    {
        boolean hasDefinition = assetDefinitionService.hasDefinition(token.tokenInfo.chainId, address);
        Intent intent = new Intent(context, Erc20DetailActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, !token.isEthereum());
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_ADDRESS, address);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(C.EXTRA_HAS_DEFINITION, hasDefinition);
        context.startActivity(intent);
    }

    @Override
    public void showTokenList(Activity activity, Token token)
    {
        Intent intent = new Intent(activity, AssetDisplayActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.Key.WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivityForResult(intent, C.TERMINATE_ACTIVITY);
    }

    public void restartServices()
    {
        fetchTransactionsInteract.restartTransactionService();
    }

    public void updateTokensCheck(Token token)
    {
        tokensService.setFocusToken(token);
    }

    public void clearFocusToken()
    {
        tokensService.clearFocusToken();
    }

    public TokensService getTokenService()
    {
        return tokensService;
    }

    public void getAuthentication(Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public void sendTransaction(Web3Transaction finalTx, long chainId, String overridenTxHash)
    {
        disposable = createTransactionInteract
                .createWithSig(wallet, finalTx, chainId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(txData -> processTransaction(txData, wallet, overridenTxHash),
                        transactionError::postValue);
    }

    private void processTransaction(TransactionData txData, Wallet wallet, String overridenTxHash)
    {
        //remove old tx from database
        if (!TextUtils.isEmpty(overridenTxHash)) fetchTransactionsInteract.removeOverridenTransaction(wallet, overridenTxHash);
        //update Activity
        transactionFinalised.postValue(txData);
    }

    public void actionSheetConfirm(String mode)
    {
        AnalyticsProperties analyticsProperties = new AnalyticsProperties();
        analyticsProperties.setData(mode);

        analyticsService.track(C.AN_CALL_ACTIONSHEET, analyticsProperties);
    }

    public Single<Intent> showTransferSelectCount(Context ctx, Token token, BigInteger tokenId)
    {
        return genericWalletInteract.find()
                .map(wallet -> completeTransferSelect(ctx, token, tokenId, wallet));
    }

    private Intent completeTransferSelect(Context ctx, Token token, BigInteger tokenId, Wallet wallet)
    {
        Intent intent = new Intent(ctx, Erc1155AssetSelectActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKEN_ID, tokenId.toString(16));
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public Single<Intent> getTransferIntent(Context ctx, Token token, List<BigInteger> tokenIds, ArrayList<NFTAsset> selection)
    {
        return genericWalletInteract.find()
                .map(wallet -> completeTransferIntent(ctx, token, tokenIds, selection, wallet));
    }

    private Intent completeTransferIntent(Context ctx, Token token, List<BigInteger> tokenIds, ArrayList<NFTAsset> selection, Wallet wallet)
    {
        Intent intent = new Intent(ctx, TransferNFTActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(tokenIds, false));
        intent.putParcelableArrayListExtra(C.EXTRA_NFTASSET_LIST, selection);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
