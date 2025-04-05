package com.alphawallet.app.ui.widget.entity;

/**
 * Created by JB on 28/10/2020.
 */

import static com.alphawallet.app.util.ens.EnsResolver.USE_ENS_CHAIN;
import static com.alphawallet.app.util.ens.EnsResolver.CANCELLED_REQUEST;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteAddressAdapter;
import com.alphawallet.app.util.ens.AWEnsResolver;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.InputAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.web3j.crypto.Keys;

import java.util.HashMap;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by James on 4/12/2018.
 * Stormbird in Singapore
 */
public class ENSHandler implements Runnable
{
    public static final int ENS_RESOLVE_DELAY = 750; //In milliseconds
    public static final int ENS_TIMEOUT_DELAY = 8000;
    private final InputAddress host;
    private final Handler handler;
    private final AutoCompleteAddressAdapter adapterUrl;
    private final AWEnsResolver ensResolver;

    @Nullable
    private Disposable disposable;
    public volatile boolean waitingForENS = false;
    private boolean hostCallbackAfterENS = false;

    /**
     * Used to skip node sync check when user clicks ignore
     */
    public boolean performEnsSync = true;

    public ENSHandler(InputAddress host, AutoCompleteAddressAdapter adapter)
    {
        this.handler = new Handler(Looper.getMainLooper());
        this.adapterUrl = adapter;
        this.host = host;
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(USE_ENS_CHAIN), host.getContext(), host.getChain());

        createWatcher();
        getENSHistoryFromPrefs(host.getContext());
    }

    private void createWatcher()
    {
        host.getInputView().setAdapter(adapterUrl);
        host.getInputView().setOnClickListener(v -> host.getInputView().showDropDown());
        waitingForENS = false;
    }

    public void checkAddress()
    {
        handler.removeCallbacks(this);
        if (couldBeENS(host.getInputText()))
        {
            waitingForENS = true;
            handler.postDelayed(this, ENS_RESOLVE_DELAY);
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
            host.setWaitingSpinner(false);
        }
        else if (Utils.isAddressValid(host.getInputText()))
        {
            //finding the ENS address is not required, only helpful so no need to wait
            handler.post(this);
        }
        else
        {
            waitingForENS = false;
        }
    }

    public void getAddress()
    {
        if (waitingForENS)
        {
            host.displayCheckingDialog(true);
            hostCallbackAfterENS = true;
            handler.postDelayed(this::checkIfWaitingForENS, ENS_TIMEOUT_DELAY);
        }
        else
        {
            if (Utils.isAddressValid(host.getInputText()) && TextUtils.isEmpty(host.getStatusText()))
            {
                //check our known ENS names list for a match
                String ensName = ensResolver.checkENSHistoryForAddress(host.getInputText());
                if (!TextUtils.isEmpty(ensName))
                {
                    host.setStatus(ensName);
                }
            }

            host.ENSComplete();
        }
    }

    public void handleHistoryItemClick(String ensName)
    {
        host.hideKeyboard();
        host.getInputView().removeTextChangedListener(host); //temporarily remove the watcher because we're handling the text change here
        host.getInputView().setText(ensName);
        host.getInputView().addTextChangedListener(host);
        host.getInputView().dismissDropDown();
        handler.removeCallbacksAndMessages(this);
        waitingForENS = true;
        handler.post(this);
    }

    public void onENSSuccess(String resolvedAddress, String ensDomain)
    {
        waitingForENS = false;
        host.setWaitingSpinner(false);
        if (Utils.isAddressValid(resolvedAddress) && canBeENSName(ensDomain))
        {
            host.getInputView().dismissDropDown();
            host.setENSAddress(resolvedAddress);
            if (host.getInputView().hasFocus())
                host.hideKeyboard(); //user was waiting for ENS, not in the middle of typing a value etc

            storeItem(resolvedAddress, ensDomain);
            host.ENSResolved(resolvedAddress, ensDomain);
        }
        else if (!TextUtils.isEmpty(resolvedAddress) && canBeENSName(resolvedAddress) && Utils.isAddressValid(ensDomain)) //in case user typed an address and hit an ENS name
        {
            host.getInputView().dismissDropDown();
            host.setENSName(host.getContext().getString(R.string.ens_resolved, resolvedAddress));
            //host.setStatus(host.getContext().getString(R.string.ens_resolved, resolvedAddress));
            if (host.getInputView().hasFocus())
                host.hideKeyboard(); //user was waiting for ENS, not in the middle of typing a value etc

            storeItem(ensDomain, resolvedAddress);
            host.ENSResolved(ensDomain, resolvedAddress);
        }
        else
        {
            host.setStatus(null);
        }

        checkIfWaitingForENS();
    }

    private void checkIfWaitingForENS()
    {
        host.setWaitingSpinner(false);
        handler.removeCallbacksAndMessages(null);
        if (hostCallbackAfterENS)
        {
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
            hostCallbackAfterENS = false;
            host.ENSComplete();
        }
    }

    private void onENSError(Throwable throwable)
    {
        host.setWaitingSpinner(false);
        host.setStatus(null);
        checkIfWaitingForENS();
    }

    public static boolean canBeENSName(String address)
    {
        return !Utils.isAddressValid(address) && !address.startsWith("0x") && address.length() > 5 && address.contains(".") && address.indexOf(".") <= address.length() - 2;
    }

    public static boolean couldBeENS(String address)
    {
        if (address == null || address.isEmpty()) return false;

        String[] split = address.split("[.]");
        if (split.length > 1)
        {
            String extension = split[split.length - 1];
            return !extension.isEmpty() && Utils.isAlNum(extension);
        }

        return false;
    }

    @Override
    public void run()
    {
        //address update delay check
        final String to = host.getInputText();

        if (disposable != null && !disposable.isDisposed()) disposable.dispose();

        //is this an address? If so, attempt reverse lookup or resolve from known ENS addresses
        if (Utils.isAddressValid(to))
        {
            host.setWaitingSpinner(true);

            disposable = ensResolver.reverseResolveEns(to)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(resolvedAddress -> {
                        if (!resolvedAddress.equals(CANCELLED_REQUEST))
                        {
                            onENSSuccess(resolvedAddress, to);
                        }
                    }, this::onENSError);
        }
        else if (canBeENSName(to))
        {
            host.setWaitingSpinner(true);
            host.ENSName(to);

            disposable = ensResolver.resolveENSAddress(to)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(resolvedAddress -> {
                        if (!resolvedAddress.equals(CANCELLED_REQUEST))
                        {
                            onENSSuccess(resolvedAddress, to);
                        }
                    }, this::onENSError);
        }
        else
        {
            host.setWaitingSpinner(false);
        }
    }

    //Given an Ethereum address, check if we can find a matching ENS name
    public Single<String> resolveENSNameFromAddress(String address)
    {
        return ensResolver.reverseResolveEns(address);
    }

    /**
     * This method will fetch stored ENS cached history of Reverse lookup
     *
     * @return Key Value pair of Address vs ENS name
     */
    private static HashMap<String, String> getENSHistoryFromPrefs(Context ctx)
    {
        HashMap<String, String> history;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(ctx).getString(C.ENS_HISTORY_PAIR, "");
        if (!historyJson.isEmpty())
        {
            history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>()
            {
            }.getType());
        }
        else
        {
            history = new HashMap<>();
        }

        return history;
    }

    public static String matchENSOrFormat(Context ctx, String ethAddress)
    {
        if (ethAddress == null) return "";
        String checkSumAddr = Keys.toChecksumAddress(ethAddress);
        if (!TextUtils.isEmpty(ethAddress) && Utils.isAddressValid(ethAddress))
        {
            HashMap<String, String> ensMap = getENSHistoryFromPrefs(ctx);
            String ensName = ensMap.get(ethAddress.toLowerCase());
            if (ensName == null) ensName = ensMap.get(checkSumAddr);
            return ensName != null ? ensName : Utils.formatAddress(ethAddress);
        }
        else
        {
            return Utils.formatAddress(ethAddress);
        }
    }

    public static String displayAddressOrENS(Context ctx, String ethAddress, boolean shrinkAddress)
    {
        String returnAddress = shrinkAddress ? Utils.formatAddress(ethAddress) : ethAddress;
        if (!TextUtils.isEmpty(ethAddress) && Utils.isAddressValid(ethAddress))
        {
            HashMap<String, String> ensMap = getENSHistoryFromPrefs(ctx);
            String ensName = ensMap.get(ethAddress);
            returnAddress = ensName != null ? ensName : returnAddress;
        }

        return returnAddress;
    }

    public static String displayAddressOrENS(Context ctx, String ethAddress)
    {
        return displayAddressOrENS(ctx, ethAddress, true);
    }

    /**
     * This method will store Address vs ENS name key value pair in preference.
     *
     * @param address Wallet Address
     * @param ensName Wallet Name
     */
    private void storeItem(String address, String ensName)
    {
        HashMap<String, String> history = getENSHistoryFromPrefs(host.getContext());

        if (!history.containsKey(address.toLowerCase()))
        {
            history.put(address.toLowerCase(), ensName);
            storeHistory(history);
        }

        adapterUrl.add(ensName);
    }

    /**
     * This method will store key value pair in preference
     *
     * @param history Key value pair
     */
    private void storeHistory(HashMap<String, String> history)
    {
        String historyJson = new Gson().toJson(history);
        PreferenceManager.getDefaultSharedPreferences(host.getContext()).edit().putString(C.ENS_HISTORY_PAIR, historyJson).apply();
    }

    /*public void setEnsNodeNotSyncCallback(EnsNodeNotSyncCallback callback)
    {
        Timber.d("setEnsNodeNotSyncCallback: ");
        ensResolver.nodeNotSyncCallback = callback;
    }*/
}
