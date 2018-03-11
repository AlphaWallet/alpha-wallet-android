package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Base64;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.SalesOrder;
import com.wallet.crypto.alphawallet.entity.ServiceErrorException;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.ImportWalletInteract;
import com.wallet.crypto.alphawallet.service.ImportTokenService;
import com.wallet.crypto.alphawallet.ui.widget.OnImportKeystoreListener;
import com.wallet.crypto.alphawallet.ui.widget.OnImportPrivateKeyListener;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.tx.Contract;

import java.math.BigInteger;
import java.security.Signature;

import static com.wallet.crypto.alphawallet.service.MarketQueueService.sigFromByteArray;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModel extends BaseViewModel  {

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;

    private final MutableLiveData<String> newTransaction = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private String univeralImportLink;

    ImportTokenViewModel(FindDefaultWalletInteract findDefaultWalletInteract,
                         CreateTransactionInteract createTransactionInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
    }

    public void prepare(String importDataStr) {
        univeralImportLink = importDataStr;
        progress.postValue(true);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onWallet, this::onError);
    }

    public LiveData<Wallet> wallet() {
        return wallet;
    }

    private void onWallet(Wallet wallet) {
        progress.postValue(false);
        this.wallet.postValue(wallet);
        SalesOrder order = SalesOrder.parseUniversalLink(univeralImportLink);
        //now ecrecover the owner

        byte[] message = order.message;
        Sign.SignatureData sigData;
        String recoveredAddress = "";
        try {
            sigData = sigFromByteArray(order.signature);
            BigInteger recoveredKey = Sign.signedMessageToKey(message, sigData);
            recoveredAddress = Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //now get balance at recovered address
        //1. add required interact
        //2. after we get balance, check these tokens are still at that address, get the ID's and update the import token
        //3. allow import to continue.
        System.out.println(recoveredAddress);

    }

    public void onError(Throwable throwable) {
        if (throwable.getCause() instanceof ServiceErrorException) {
            if (((ServiceErrorException) throwable.getCause()).code == C.ErrorCode.ALREADY_ADDED){
                error.postValue(new ErrorEnvelope(C.ErrorCode.ALREADY_ADDED, null));
            }
        } else {
            error.postValue(new ErrorEnvelope(C.ErrorCode.UNKNOWN, throwable.getMessage()));
        }
    }

    public void performImport() {
        SalesOrder order = SalesOrder.parseUniversalLink(univeralImportLink);
        //ok let's try to drive this guy through
        final byte[] tradeData = SalesOrder.generateReverseTradeData(order);
        System.out.println("Approx value of trade: " + order.price);
        //now push the transaction
        progress.postValue(true);
        disposable = createTransactionInteract
                .create(wallet.getValue(), order.contractAddress, order.priceWei,
                        Contract.GAS_PRICE, Contract.GAS_LIMIT, tradeData)
                .subscribe(this::onCreateTransaction, this::onError);
    }



    private void onCreateTransaction(String transaction)
    {
        progress.postValue(false);
        newTransaction.postValue(transaction);
    }
}
