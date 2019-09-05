package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.token.entity.ContractAddress;
import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.entity.TokenScriptResult;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.*;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.wallet.web3.entity.Message;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.stormbird.wallet.C.*;
import static io.stormbird.wallet.C.Key.*;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final SellTicketRouter sellTicketRouter;
    private final TransferTicketDetailRouter transferTicketRouter;
    private final CreateTransactionInteract createTransactionInteract;
    private final GasService gasService;
    private final TokensService tokensService;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final KeyService keyService;
    private final GenericWalletInteract genericWalletInteract;
    private Wallet wallet;

    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService,
            SellTicketRouter sellTicketRouter,
            TransferTicketDetailRouter transferTicketRouter,
            CreateTransactionInteract createTransactionInteract,
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract) {
        this.assetDefinitionService = assetDefinitionService;
        this.sellTicketRouter = sellTicketRouter;
        this.transferTicketRouter = transferTicketRouter;
        this.createTransactionInteract = createTransactionInteract;
        this.gasService = gasService;
        this.tokensService = tokensService;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.keyService = keyService;
        this.genericWalletInteract = genericWalletInteract;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void openUniversalLink(Context context, Token token, String ticketIDs) {
        Intent intent = new Intent(context, SellDetailActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, ticketIDs);
        intent.putExtra(EXTRA_STATE, SellDetailActivity.SET_A_PRICE);
        intent.putExtra(EXTRA_PRICE, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public void startGasPriceUpdate(int chainId)
    {
        gasService.startGasListener(chainId);
    }

    public void sellTicketRouter(Context ctx, Token token) {
        sellTicketRouter.open(ctx, token);
    }
    public void showTransferToken(Context context, Token token, String tokenIds)
    {
        transferTicketRouter.open(context, token, tokenIds, wallet);
    }

    public void showFunction(Context ctx, Token token, String method, List<BigInteger> tokenIds)
    {
        Intent intent = new Intent(ctx, FunctionActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(C.EXTRA_STATE, method);
        BigInteger firstId = tokenIds != null ? tokenIds.get(0) : BigInteger.ZERO;
        intent.putExtra(C.EXTRA_TOKEN_ID, firstId.toString(16));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void showRedeemToken(Context ctx, Token token, List<BigInteger> ids) {

        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(ids, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemSignatureDisplayActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra(TICKET, token);
        intent.putExtra(TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void signMessage(byte[] signRequest, DAppFunction dAppFunction, Message<String> message, int chainId) {
        disposable = createTransactionInteract.sign(wallet, signRequest, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig, message),
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
        intent.putExtra(C.EXTRA_CONTRACT_NAME, additionalDetails);
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
        intent.putExtra(C.EXTRA_SYMBOL, nativeEth.tokenInfo.symbol);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, false);
        intent.putExtra(C.EXTRA_ENS_DETAILS, info);
        intent.putExtra(C.EXTRA_NETWORKID, nativeEth.tokenInfo.chainId);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.ETH.ordinal());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    public Token getToken(ContractAddress cAddr)
    {
        return tokensService.getToken(cAddr.chainId, cAddr.address);
    }

    public void selectRedeemToken(Context ctx, Token token, List<BigInteger> idList)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(idList, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemAssetSelectActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(TICKET_RANGE, parcel);
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
                .subscribe(wallet -> { this.wallet = wallet; }, this::onError);
    }

    public void resetSignDialog()
    {
        keyService.resetSigningDialog();
    }
}
