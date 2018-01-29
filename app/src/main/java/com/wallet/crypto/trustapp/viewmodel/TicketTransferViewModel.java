package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Ticket;
import com.wallet.crypto.trustapp.entity.TicketInfo;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.FetchTokensInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.TicketTransferInteract;
import com.wallet.crypto.trustapp.router.ConfirmationRouter;
import com.wallet.crypto.trustapp.router.TicketTransferRouter;

import java.math.BigInteger;

import static com.wallet.crypto.trustapp.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferViewModel extends BaseViewModel
{
    private final TicketTransferInteract ticketTransferInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TicketTransferRouter ticketTransferRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;

    private final MutableLiveData<Token[]> tokens = new MutableLiveData<>();
    private final MutableLiveData<Ticket> ticket = new MutableLiveData<>();

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    private String address;

    public TicketTransferViewModel(
            TicketTransferInteract ticketTransferInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            TicketTransferRouter ticketTransferRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ConfirmationRouter confirmationRouter) {
        this.ticketTransferInteract = ticketTransferInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.ticketTransferRouter = ticketTransferRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
    }

    public void prepare(String address) {
        this.address = address;
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    public LiveData<Token[]> tokens() {
        return tokens;
    }
    public LiveData<Ticket> ticket() {
        return ticket;
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        defaultWallet.setValue(wallet);
        disposable = fetchTokensInteract
                .fetch(wallet)
                .subscribe(this::onTokens, this::onError, this::onFetchTokensCompletable);
    }

    private void onFetchTokensCompletable() {
        progress.postValue(false);
        Token[] tokens = tokens().getValue();
        if (tokens == null || tokens.length == 0) {
            error.postValue(new ErrorEnvelope(EMPTY_COLLECTION, "tokens not found"));
        }
    }

    private void onTokens(Token[] tokens) {
        if (tokens != null && tokens.length > 0) {
            progress.postValue(true);
        }
        this.tokens.setValue(tokens);

        for (Token t : tokens) {
            if (t instanceof Ticket && t.tokenInfo.address.equals(address))
            {
                ticket.setValue((Ticket)t);
                ticket.postValue((Ticket)t);
                break;
            }
        }
    }

    public void openConfirmation(Context context, String to, String ids, String ticketIDs) {
        try
        {
            TicketInfo ticket = (TicketInfo) (this.ticket().getValue().tokenInfo);
            confirmationRouter.open(context, to, ids, ticket.address, ticket.decimals, ticket.symbol, ticketIDs);
        }
        catch (Exception e)
        {

        }
    }
}
