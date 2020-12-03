package com.alphawallet.app.ui.widget.entity;

/**
 * Created by JB on 28/10/2020.
 */

import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.AutoCompleteTextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteAddressAdapter;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.widget.InputAddress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.web3j.crypto.WalletUtils.isValidAddress;

/**
 * Created by James on 4/12/2018.
 * Stormbird in Singapore
 */
public class ENSHandler implements Runnable
{
    public  static final int ENS_RESOLVE_DELAY = 750; //In milliseconds
    private final InputAddress host;
    private TextWatcher ensTextWatcher;
    private final Handler handler;
    private final AutoCompleteAddressAdapter adapterUrl;
    private AWEnsResolver ensResolver;
    private final AutoCompleteTextView toAddressEditText;
    private final float standardTextSize;

    @Nullable
    private Disposable disposable;
    public volatile boolean waitingForENS = false;
    public boolean transferAfterENS = false;

    public ENSHandler(InputAddress host, AutoCompleteAddressAdapter adapter)
    {
        this.handler = new Handler();
        this.adapterUrl = adapter;
        this.host = host;
        this.toAddressEditText = host.getEditText();
        this.ensResolver = null;

        standardTextSize = toAddressEditText.getTextSize();

        createWatcher();
        getENSHistoryFromPrefs();
    }

    private void createWatcher()
    {
        toAddressEditText.setAdapter(adapterUrl);
        toAddressEditText.setOnClickListener(v -> toAddressEditText.showDropDown());

        waitingForENS = false;

        ensTextWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                host.setStatus(null);
                float ts = toAddressEditText.getTextSize();
                int amount = toAddressEditText.getText().length();
                if (amount > 30 && ts == standardTextSize)
                {
                    toAddressEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, standardTextSize*0.85f); //shrink text size to fit
                }
                else if (amount <= 30 && ts < standardTextSize)
                {
                    toAddressEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, standardTextSize);
                }
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                host.setStatus(null);
                checkAddress();
            }
        };

        toAddressEditText.addTextChangedListener(ensTextWatcher);
    }

    private void checkAddress()
    {
        waitingForENS = true;
        handler.removeCallbacks(this);
        handler.postDelayed(this, ENS_RESOLVE_DELAY);
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        host.setWaitingSpinner(false);
    }

    public void getAddress()
    {
        if (waitingForENS)
        {
            host.displayCheckingDialog(true);
            transferAfterENS = true;
        }
        else
        {
            host.ENSComplete();
        }
    }

    public void handleHistoryItemClick(String ensName)
    {
        host.hideKeyboard();
        toAddressEditText.removeTextChangedListener(ensTextWatcher); //temporarily remove the watcher because we're handling the text change here
        toAddressEditText.setText(ensName);
        toAddressEditText.addTextChangedListener(ensTextWatcher);
        toAddressEditText.dismissDropDown();
        handler.removeCallbacksAndMessages(this);
        waitingForENS = true;
        handler.post(this);
    }

    public void onENSSuccess(String resolvedAddress, String ensDomain)
    {
        waitingForENS = false;
        if (!TextUtils.isEmpty(resolvedAddress) && isValidAddress(resolvedAddress) && canBeENSName(ensDomain))
        {
            toAddressEditText.dismissDropDown();
            host.setStatus(resolvedAddress);
            if (toAddressEditText.hasFocus()) host.hideKeyboard(); //user was waiting for ENS, not in the middle of typing a value etc

            storeItem(resolvedAddress, ensDomain);
        }
        else if (!TextUtils.isEmpty(resolvedAddress) && canBeENSName(resolvedAddress) && isValidAddress(ensDomain)) //in case user typed an address and hit an ENS name
        {
            toAddressEditText.dismissDropDown();
            host.setStatus(host.getContext().getString(R.string.ens_resolved, resolvedAddress));
            if (toAddressEditText.hasFocus()) host.hideKeyboard(); //user was waiting for ENS, not in the middle of typing a value etc

            storeItem(ensDomain, resolvedAddress);
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
        if (transferAfterENS)
        {
            transferAfterENS = false;
            host.ENSComplete();
        }
    }

    private void onENSError(Throwable throwable)
    {
        host.setStatus(null);
        checkIfWaitingForENS();
    }

    public static boolean canBeENSName(String address)
    {
        return !TextUtils.isEmpty(address) && !address.startsWith("0x") && address.length() > 5 && address.contains(".") && address.indexOf(".") <= address.length() - 2;
    }

    @Override
    public void run()
    {
        //address update delay check
        final String to = toAddressEditText.getText().toString().trim();

        if (disposable != null && !disposable.isDisposed()) disposable.dispose();

        //is this an address? If so, attempt reverse lookup or resolve from known ENS addresses
        if (!TextUtils.isEmpty(to) && isValidAddress(to))
        {
            initENSHandler();
            host.setWaitingSpinner(true);

            disposable = ensResolver.resolveEnsName(to)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(resolvedAddress -> onENSSuccess(resolvedAddress, to), this::onENSError);
        }
        else if (canBeENSName(to))
        {
            initENSHandler();
            host.setWaitingSpinner(true);

            disposable = ensResolver.resolveENSAddress(to)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(resolvedAddress -> onENSSuccess(resolvedAddress, to), this::onENSError);
        }
        else
        {
            host.setWaitingSpinner(false);
        }
    }

    /**
     * This method will fetch stored ENS cached history of Reverse lookup
     * @return Key Value pair of Address vs ENS name
     */
    private HashMap<String, String> getENSHistoryFromPrefs()
    {
        HashMap<String, String> history;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(host.getContext()).getString(C.ENS_HISTORY_PAIR, "");
        if (!historyJson.isEmpty())
        {
            history = new Gson().fromJson(historyJson, new TypeToken<HashMap<String, String>>(){}.getType());
        }
        else
        {
            history = new HashMap<>();
        }

        return history;
    }

    /**
     * This method will store Address vs ENS name key value pair in preference.
     * @param address Wallet Address
     * @param ensName Wallet Name
     */
    private void storeItem(String address, String ensName)
    {
        HashMap<String, String> history = getENSHistoryFromPrefs();

        if (!history.containsKey(address.toLowerCase()))
        {
            history.put(address.toLowerCase(), ensName);
            storeHistory(history);
        }

        adapterUrl.add(ensName);
    }

    /**
     * This method will store key value pair in preference
     * @param history Key value pair
     */
    private void storeHistory(HashMap<String, String> history)
    {
        String historyJson = new Gson().toJson(history);
        PreferenceManager.getDefaultSharedPreferences(host.getContext()).edit().putString(C.ENS_HISTORY_PAIR, historyJson).apply();
    }

    private void initENSHandler()
    {
        if (ensResolver == null)
        {
            this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), host.getContext());
        }
    }
}
