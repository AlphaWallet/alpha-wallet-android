package com.alphawallet.app.ui.widget.entity;

import android.app.Activity;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ENSCallback;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteAddressAdapter;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.widget.AWalletAlertDialog;
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

    private final LinearLayout layoutENSResolve;
    private final AutoCompleteTextView toAddressEditText;
    private final TextView textENS;
    private TextWatcher ensTextWatcher;
    private String ensName;
    private final Handler handler;
    private final AutoCompleteAddressAdapter adapterUrl;
    private final TextView toAddressError;
    private final Activity host;
    private final ProgressBar ensSpinner;
    private final ENSCallback ensCallback;
    private final AWEnsResolver ensResolver;

    @Nullable
    private Disposable disposable;

    private AWalletAlertDialog dialog;

    public volatile boolean waitingForENS = false;
    public boolean transferAfterENS = false;

    public ENSHandler(Activity host, AutoCompleteAddressAdapter adapter, ENSCallback ensCallback)
    {
        this.layoutENSResolve = host.findViewById(R.id.layout_ens);
        this.toAddressEditText = host.findViewById(R.id.edit_to_address);
        this.textENS = host.findViewById(R.id.text_ens_resolve);
        this.toAddressError = host.findViewById(R.id.to_address_error);
        this.ensSpinner = host.findViewById(R.id.ens_fetch_progres);
        this.handler = new Handler();
        this.adapterUrl = adapter;
        this.host = host;
        this.ensCallback = ensCallback;
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID), host.getApplicationContext());
        this.ensSpinner.setVisibility(View.GONE);
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
                toAddressError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                textENS.setText("");
                checkAddress();
            }
        };

        toAddressEditText.addTextChangedListener(ensTextWatcher);
    }

    private void checkAddress()
    {
        if (!transferAfterENS)
        {
            waitingForENS = true;
            handler.removeCallbacks(this);
            handler.postDelayed(this, ENS_RESOLVE_DELAY);
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
            ensSpinner.setVisibility(View.GONE);
        }
    }

    public String getAddressFromEditView()
    {
        //check send address
        ensName = null;
        toAddressError.setVisibility(View.GONE);
        String to = toAddressEditText.getText().toString();
        if (!isValidAddress(to))
        {
            String ens = to;
            to = textENS.getText().toString();
            ensName = "@" + ens + " (" + to + ")";
        }

        if (!isValidAddress(to))
        {
            to = null;
            if (waitingForENS)
            {
                transferAfterENS = true;
                onENSProgress(true);
            }
            else
            {
                toAddressError.setVisibility(View.VISIBLE);
                toAddressError.setText(host.getString(R.string.error_invalid_address));
            }
        }
        else if (ensName != null)
        {
            adapterUrl.add(toAddressEditText.getText().toString());
        }

        return to;
    }

    public void onENSProgress(boolean shouldShowProgress)
    {
        if (shouldShowProgress)
        {
            dialog = new AWalletAlertDialog(host);
            dialog.setIcon(AWalletAlertDialog.NONE);
            dialog.setTitle(R.string.title_dialog_check_ens);
            dialog.setProgressMode();
            dialog.setCancelable(false);
            dialog.show();
        }
        else if (dialog != null && dialog.isShowing())
        {
            dialog.dismiss();
        }
    }

    public void handleHistoryItemClick(String ensName)
    {
        KeyboardUtils.hideKeyboard(host.getCurrentFocus());
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
        if (TextUtils.isEmpty(resolvedAddress) || resolvedAddress.equals("0"))
        {
            hideENS(resolvedAddress);
        }
        else
        {
            waitingForENS = false;
            toAddressEditText.dismissDropDown();
            layoutENSResolve.setVisibility(View.VISIBLE);
            textENS.setText(resolvedAddress);
            if (toAddressEditText.hasFocus())
                KeyboardUtils.hideKeyboard(host.getCurrentFocus()); //user was waiting for ENS, not in the middle of typing a value etc
            checkIfWaitingForENS();
            toAddressError.setVisibility(View.GONE);

            storeItem(resolvedAddress, ensDomain);
        }
    }

    public void hideENS(String name)
    {
        waitingForENS = false;
        layoutENSResolve.setVisibility(View.GONE);
        checkIfWaitingForENS();
    }

    private void checkIfWaitingForENS()
    {
        ensSpinner.setVisibility(View.GONE);
        onENSProgress(false);
        if (transferAfterENS)
        {
            transferAfterENS = false;
            ensCallback.ENSComplete();
        }
    }

    public String getEnsName()
    {
        return ensName;
    }

    public void checkENS()
    {
        //address update delay check
        final String to = toAddressEditText.getText().toString();

        if (disposable != null && !disposable.isDisposed()) disposable.dispose();

        ensSpinner.setVisibility(View.VISIBLE);

        disposable = ensResolver.resolveENSAddress(to)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resolvedAddress -> onENSSuccess(resolvedAddress, to), this::onENSError);
    }

    private void onENSError(Throwable throwable)
    {
        hideENS("");
    }

    public static boolean canBeENSName(String address)
    {
        return address.length() > 5 && !address.startsWith("0x") && address.contains(".");
    }

    @Override
    public void run()
    {
        checkENS();
    }

    /**
     * This method will fetch stored ENS cached history of Reverse lookup
     * @return Key Value pair of Address vs ENS name
     */
    private HashMap<String, String> getENSHistoryFromPrefs()
    {
        HashMap<String, String> history;
        String historyJson = PreferenceManager.getDefaultSharedPreferences(host).getString(C.ENS_HISTORY_PAIR, "");
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
    }

    /**
     * This method will store key value pair in preference
     * @param history Key value pair
     */
    private void storeHistory(HashMap<String, String> history)
    {
        String historyJson = new Gson().toJson(history);
        PreferenceManager.getDefaultSharedPreferences(host).edit().putString(C.ENS_HISTORY_PAIR, historyJson).apply();
    }
}
