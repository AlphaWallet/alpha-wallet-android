package com.wallet.crypto.alphawallet.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.alphawallet.entity.GasSettings;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.TokenRepository;
import com.wallet.crypto.alphawallet.router.GasSettingsRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class SellDetailModel extends BaseViewModel {
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();
    private final MutableLiveData<GasSettings> gasSettings = new MutableLiveData<>();

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MarketQueueService marketQueueService;

    SellDetailModel(FindDefaultWalletInteract findDefaultWalletInteract,
                          MarketQueueService marketQueueService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
    }

    public LiveData<Wallet> defaultWallet() {
        return defaultWallet;
    }

    public void prepare(Ticket ticket) {
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
    }

    public void generateMarketOrders(String indexSendList, String contractAddr, BigInteger price, String idList) {
        //generate a list of integers
        Ticket t = new Ticket(null, "0", 0);
        List<Integer> sends = t.parseIDListInteger(indexSendList);
        List<Integer> iDs = t.parseIDListInteger(idList);

        if (sends != null && sends.size() > 0)
        {
            short[] ticketIDs = new short[sends.size()];
            int index = 0;

            for (Integer i : sends)
            {
                ticketIDs[index++] = i.shortValue();
            }

            int firstIndex = iDs.get(0);

            price = price.multiply(BigInteger.valueOf(iDs.size()));

            marketQueueService.createMarketOrders(defaultWallet.getValue(), price, ticketIDs, contractAddr, firstIndex);
        }
    }
}
