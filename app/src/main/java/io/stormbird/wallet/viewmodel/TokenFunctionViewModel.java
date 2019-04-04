package io.stormbird.wallet.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.FunctionActivity;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;
    private final SellTicketRouter sellTicketRouter;
    private final TransferTicketRouter transferTicketRouter;

    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService,
            SellTicketRouter sellTicketRouter,
            TransferTicketRouter transferTicketRouter) {
        this.assetDefinitionService = assetDefinitionService;
        this.sellTicketRouter = sellTicketRouter;
        this.transferTicketRouter = transferTicketRouter;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }

    public void sellTicketRouter(Context ctx, Token token) {
        sellTicketRouter.open(ctx, token);
    }
    public void showTransferToken(Context context, Token ticket) { transferTicketRouter.open(context, ticket); }
    public void showFunction(Context ctx, Token token, String viewData)
    {
        Intent intent = new Intent(ctx, FunctionActivity.class);
        intent.putExtra(TICKET, token);
        intent.putExtra(C.EXTRA_STATE, viewData);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }
}
