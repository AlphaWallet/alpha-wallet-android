package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ConfirmationType;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.DAppFunction;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenFactory;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.repository.entity.RealmTokenTicker;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.ui.FunctionActivity;
import com.alphawallet.app.ui.MyAddressActivity;
import com.alphawallet.app.ui.RedeemAssetSelectActivity;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.ui.widget.entity.TicketRangeParcel;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static com.alphawallet.app.entity.DisplayState.TRANSFER_TO_ADDRESS;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private final OpenseaService openseaService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private Wallet wallet;

    private final MutableLiveData<Token> insufficientFunds = new MutableLiveData<>();
    private final MutableLiveData<String> invalidAddress = new MutableLiveData<>();
    private final MutableLiveData<XMLDsigDescriptor> sig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> newScriptFound = new MutableLiveData<>();
    private final MutableLiveData<Wallet> walletUpdate = new MutableLiveData<>();

    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract,
            OpenseaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract)
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

    public void prepare()
    {
        getCurrentWallet();
    }

    public void openUniversalLink(Context context, Token token, List<BigInteger> selection) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.EXTRA_TOKENID_LIST, token.bigIntListToString(selection, false));
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(C.EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void startGasPriceUpdate(int chainId)
    {
        gasService.startGasListener(chainId);
    }

    public void showTransferToken(Context ctx, Token token, List<BigInteger> selection)
    {
        Intent intent = new Intent(ctx, TransferTicketDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.Key.TICKET, token);

        intent.putExtra(C.EXTRA_TOKENID_LIST, token.bigIntListToString(selection, false));

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
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_STATE, method);
        if (tokenIds == null) tokenIds = new ArrayList<>(Collections.singletonList(BigInteger.ZERO));
        intent.putExtra(C.EXTRA_TOKEN_ID, token.bigIntListToString(tokenIds, true));
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

    public void signMessage(Signable message, DAppFunction dAppFunction, int chainId) {
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

    public void confirmTransaction(Context ctx, int networkId, String functionData, String toAddress,
                                   String contractAddress, String additionalDetails, String functionName, String value)
    {
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TRANSACTION_DATA, functionData);
        intent.putExtra(C.EXTRA_NETWORKID, networkId);
        intent.putExtra(C.EXTRA_NETWORK_NAME, ethereumNetworkRepository.getNetworkByChain(networkId).getShortName());
        intent.putExtra(C.EXTRA_AMOUNT, value);
        if (toAddress != null) intent.putExtra(C.EXTRA_TO_ADDRESS, toAddress);
        if (contractAddress != null) intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_ACTION_NAME, additionalDetails);
        intent.putExtra(C.EXTRA_FUNCTION_NAME, functionName);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.TOKENSCRIPT.ordinal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public void confirmNativeTransaction(Context ctx, String toAddress, BigDecimal value, Token nativeEth, String info)
    {
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, toAddress);
        intent.putExtra(C.EXTRA_AMOUNT, value.toString());
        intent.putExtra(C.EXTRA_DECIMALS, nativeEth.tokenInfo.decimals);
        intent.putExtra(C.EXTRA_SYMBOL, nativeEth.getSymbol());
        intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
        intent.putExtra(C.EXTRA_ENS_DETAILS, info);
        intent.putExtra(C.EXTRA_NETWORKID, nativeEth.tokenInfo.chainId);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.ETH.ordinal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public Token getToken(int chainId, String contractAddress)
    {
        return tokensService.getToken(chainId, contractAddress);
    }

    public void selectRedeemToken(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.Key.TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void stopGasSettingsFetch()
    {
        gasService.stopGasListener();
    }

    public void getAuthorisation(Activity activity, SignAuthenticationCallback callback)
    {
        keyService.getAuthenticationForSignature(wallet, activity, callback);
    }

    public Token getCurrency(int chainId)
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
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void selectRedeemTokens(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.Key.TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void sellTicketRouter(Context context, Token token, List<BigInteger> idList) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.Key.TICKET, token);
        intent.putExtra(C.EXTRA_TOKENID_LIST, token.bigIntListToString(idList, false));
        intent.putExtra(C.EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(C.EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public boolean handleFunction(TSAction action, BigInteger tokenId, Token token, Context context)
    {
        String functionEffect = action.function.method;
        if (action.function.tx != null && (action.function.method == null || action.function.method.length() == 0)
                && (action.function.parameters == null || action.function.parameters.size() == 0))
        {
            //no params, this is a native style transaction
            NativeSend(action, token, context);
        }
        else
        {
            //what's selected?
            ContractAddress cAddr = new ContractAddress(action.function);
            String functionData = getTransactionBytes(token, tokenId, action.function);
            if (functionData == null) return false;
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

            //Form full method representation
            String fullMethod = action.function.method + "(";
            boolean firstArg = true;
            for (MethodArg arg : action.function.parameters)
            {
                if (!firstArg) fullMethod += ", ";
                firstArg = false;
                fullMethod += arg.parameterType;
                fullMethod += " ";
                fullMethod += arg.element.value;
            }

            fullMethod += ")";

            //finished resolving attributes, blank definition cache so definition is re-loaded when next needed
            getAssetDefinitionService().clearCache();

            confirmTransaction(context, cAddr.chainId, functionData, null, cAddr.address, fullMethod, functionEffect, value);
        }

        return true;
    }

    private void NativeSend(TSAction action, Token token, Context context)
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

        if (isValid) {
            confirmNativeTransaction(context, to, value, token, extraInfo);
        }
    }

    public OpenseaService getOpenseaService()
    {
        return openseaService;
    }

    public void updateTokenScriptViewSize(Token token, int itemViewHeight)
    {
        assetDefinitionService.storeTokenViewHeight(token.tokenInfo.chainId, token.getAddress(), itemViewHeight)
                    .isDisposed();
    }

    public void checkForNewScript(Token token)
    {
        //check server for new tokenscript
        assetDefinitionService.checkServerForScript(token.tokenInfo.chainId, token.getAddress())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.single())
                .subscribe(this::handleFilename, this::onError)
                .isDisposed();
    }

    private void handleFilename(String newFile)
    {
        if (!TextUtils.isEmpty(newFile)) newScriptFound.postValue(true);
    }

    public boolean isAuthorizeToFunction()
    {
        return wallet.type != WalletType.WATCH;
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
}
