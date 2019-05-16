package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import io.stormbird.token.entity.FunctionDefinition;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.entity.TokenScriptResult;
import io.stormbird.wallet.entity.Wallet;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.DAppFunction;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.FunctionActivity;
import io.stormbird.wallet.ui.RedeemAssetSelectActivity;
import io.stormbird.wallet.ui.RedeemSignatureDisplayActivity;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.wallet.web3.entity.Message;

import java.math.BigInteger;
import java.util.List;

import static io.stormbird.wallet.C.Key.*;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final SellTicketRouter sellTicketRouter;
    private final TransferTicketRouter transferTicketRouter;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final TokensService tokensService;

    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService,
            SellTicketRouter sellTicketRouter,
            TransferTicketRouter transferTicketRouter,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            TokensService tokensService) {
        this.assetDefinitionService = assetDefinitionService;
        this.sellTicketRouter = sellTicketRouter;
        this.transferTicketRouter = transferTicketRouter;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.tokensService = tokensService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void sellTicketRouter(Context ctx, Token token) {
        sellTicketRouter.open(ctx, token);
    }
    public void showTransferToken(Context context, Token ticket) { transferTicketRouter.open(context, ticket); }
    public void showFunction(Context ctx, Token token, String method)
    {
        Intent intent = new Intent(ctx, FunctionActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(C.EXTRA_STATE, method);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void showRedeemToken(Context ctx, Token token, List<BigInteger> ids) {

        TicketRangeParcel parcel = new TicketRangeParcel(new TicketRange(ids, token.getAddress(), true));
        Intent intent = new Intent(ctx, RedeemSignatureDisplayActivity.class);
        intent.putExtra(WALLET, new Wallet(token.getWallet()));
        intent.putExtra(TICKET, token);
        intent.putExtra(TICKET_RANGE, parcel);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }

    public void signMessage(byte[] signRequest, DAppFunction dAppFunction, Message<String> message, int chainId, String walletAddress) {
        Wallet wallet = new Wallet(walletAddress);
        disposable = createTransactionInteract.sign(wallet, signRequest, chainId)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sig -> dAppFunction.DAppReturn(sig, message),
                           error -> dAppFunction.DAppError(error, message));
    }

    public String getTransactionBytes(Token token, BigInteger tokenId, FunctionDefinition def)
    {
        return fetchTokensInteract.getTransactionBytes(token, tokenId, def);
    }

    public TokenScriptResult getTokenScriptResult(Token token, BigInteger tokenId)
    {
        return assetDefinitionService.getTokenScriptResult(token, tokenId);
    }
}
