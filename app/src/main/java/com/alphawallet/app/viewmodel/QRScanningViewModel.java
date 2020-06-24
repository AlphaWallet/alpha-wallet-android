package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.zxing.QRScanningActivity;

public class QRScanningViewModel extends BaseViewModel {

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final MyAddressRouter myAddressRouter;

    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();

    QRScanningViewModel(EthereumNetworkRepositoryType ethereumNetworkRepository,
                        FindDefaultNetworkInteract findDefaultNetworkInteract,
                        GenericWalletInteract genericWalletInteract,
                        MyAddressRouter myAddressRouter) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.myAddressRouter = myAddressRouter;
    }

    public void prepare(Context context)
    {
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        defaultNetwork.postValue(networkInfo);
        disposable = genericWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(final Wallet wallet)
    {
        defaultWallet.setValue(wallet);
    }

    public void showImportLink(Context ctx, String qrCode)
    {
        Intent intent = new Intent(ctx, ImportTokenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(C.IMPORT_STRING, qrCode);
        ctx.startActivity(intent);
    }

    public void showSend(Context ctx, QRResult result)
    {
        Intent intent = new Intent(ctx, SendActivity.class);
        boolean sendingTokens = (result.getFunction() != null && result.getFunction().length() > 0);
        String address = defaultWallet.getValue().address;
        int decimals = 18;

        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, ethereumNetworkRepository.getNetworkByChain(result.chainId).symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, defaultWallet.getValue());
        intent.putExtra(C.EXTRA_TOKEN_ID, (Token)null);
        intent.putExtra(C.EXTRA_AMOUNT, result);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        ctx.startActivity(intent);
    }

    public void loadUrl(Activity activity, String qrCode)
    {
        Intent intent = new Intent();
        intent.putExtra(C.EXTRA_URL, qrCode);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    public void finishWithCancel(Activity activity)
    {
        Intent intent = new Intent();
        activity.setResult(Activity.RESULT_CANCELED, intent);
        activity.finish();
    }

    public void showMyAddress(Activity activity)
    {
        myAddressRouter.open(activity, defaultWallet.getValue());
    }
}
