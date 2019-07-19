package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.router.RedeemSignatureDisplayRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.entity.TicketRangeParcel;
import io.stormbird.token.entity.TicketRange;

/**
 * Created by James on 27/02/2018.
 */

public class RedeemAssetSelectViewModel extends BaseViewModel
{
    private final GenericWalletInteract genericWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final RedeemSignatureDisplayRouter redeemSignatureDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    public RedeemAssetSelectViewModel(
            GenericWalletInteract genericWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            RedeemSignatureDisplayRouter redeemSignatureDisplayRouter,
            AssetDefinitionService assetDefinitionService) {
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemSignatureDisplayRouter = redeemSignatureDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    public void prepare() {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void showRedeemSignature(Context ctx, TicketRange range, Token token)
    {
        TicketRangeParcel parcel = new TicketRangeParcel(range);
        redeemSignatureDisplayRouter.open(ctx, defaultWallet.getValue(), token, parcel);
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
