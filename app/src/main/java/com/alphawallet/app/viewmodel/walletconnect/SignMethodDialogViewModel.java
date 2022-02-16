package com.alphawallet.app.viewmodel.walletconnect;

import android.app.Activity;

import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.cryptokeys.SignatureFromKey;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.BaseViewModel;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.tools.Numeric;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;
import com.walletconnect.walletconnectv2.core.exceptions.WalletConnectException;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Single;
import timber.log.Timber;

@HiltViewModel
public class SignMethodDialogViewModel extends BaseViewModel
{
    private FetchWalletsInteract fetchWalletsInteract;
    private TransactionRepositoryType transactionRepositoryType;
    private KeyService keyService;
    private MutableLiveData<Boolean> completed = new MutableLiveData<>(false);

    @Inject
    public SignMethodDialogViewModel(FetchWalletsInteract fetchWalletsInteract, TransactionRepositoryType transactionRepositoryType, KeyService keyService)
    {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.transactionRepositoryType = transactionRepositoryType;
        this.keyService = keyService;
    }

    public void sign(Activity activity, EthereumMessage ethereumMessage, String walletAddress, WalletConnect.Model.SessionRequest sessionRequest)
    {
        Single<Wallet> signer = fetchWalletsInteract.getWallet(walletAddress);
        signer.subscribe(wallet ->
        {
            keyService.getAuthenticationForSignature(wallet, activity, new SignAuthenticationCallback()
            {
                @Override
                public void gotAuthorisation(boolean gotAuth)
                {
                    if (gotAuth)
                    {
                        long chainId = Long.parseLong(sessionRequest.getChainId().split(":")[1]);
                        Single<SignatureFromKey> signature = transactionRepositoryType.getSignature(wallet, ethereumMessage, chainId);
                        signature
                                .delay(3, TimeUnit.SECONDS) // The WC connection shutdown when show biometric, when back to foreground, it will open new connection, so need delay to wait the connection opened
                                .subscribe(signatureFromKey -> onSuccess(signatureFromKey, sessionRequest), SignMethodDialogViewModel.this::onError);
                    }
                }

                @Override
                public void cancelAuthentication()
                {
                    Timber.d("cancelAuthentication");
                }
            });
        }, this::onWalletFetchError);
    }

    private void onWalletFetchError(Throwable throwable)
    {
        Timber.e(throwable);
    }

    public void onError(Throwable throwable)
    {
        Timber.e(throwable);
    }

    private void onSuccess(SignatureFromKey signatureFromKey, WalletConnect.Model.SessionRequest sessionRequest)
    {

        String result = Numeric.toHexString(signatureFromKey.signature);
        WalletConnect.Model.JsonRpcResponse jsonRpcResponse = new WalletConnect.Model.JsonRpcResponse.JsonRpcResult(sessionRequest.getRequest().getId(), result);
        WalletConnect.Params.Response response = new WalletConnect.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        try
        {
            WalletConnectClient.INSTANCE.respond(response, Timber::e);
            completed.postValue(true);
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
    }

    public void completeAuthentication(Operation taskCode)
    {
        keyService.completeAuthentication(taskCode);
    }

    public void failedAuthentication(Operation taskCode)
    {
        keyService.failedAuthentication(taskCode);
    }

    public LiveData<Boolean> completed()
    {
        return completed;
    }
}
