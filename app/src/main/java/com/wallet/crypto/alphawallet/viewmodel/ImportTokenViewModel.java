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

import org.web3j.tx.Contract;

import java.math.BigInteger;

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

// 0x696ecc55
// 0000000000000000000000000000000000000000000000000000000000000000
// 00000000000000000000000000000000000000000000000000000000000000a0
// 000000000000000000000000000000000000000000000000000000000000001c
// a85af7810c4dee72477b8cffc78589bffd59310c81e904a0248be2e6c2ddc09d
// 1b68176f496a041a2dc2d37b654e2ba57e232d9bea4a871a14752ea545a1f2a1
// 0000000000000000000000000000000000000000000000000000000000000002
// 00000000000000000000000000000000000000000000000000000000000000a4
// 00000000000000000000000000000000000000000000000000000000000000a5
