package com.alphawallet.app.ui.widget.entity;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ENSCallback;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteUrlAdapter;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.widget.AWalletAlertDialog;

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
    private final AutoCompleteUrlAdapter adapterUrl;
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

    public ENSHandler(Activity host, AutoCompleteUrlAdapter adapter, ENSCallback ensCallback)
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
        this.ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(EthereumNetworkRepository.MAINNET_ID));
        this.ensSpinner.setVisibility(View.GONE);
        createWatcher();
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

    public void onENSSuccess(String address)
    {
        if (TextUtils.isEmpty(address) || address.equals("0"))
        {
            hideENS(address);
        }
        else
        {
            waitingForENS = false;
            toAddressEditText.dismissDropDown();
            layoutENSResolve.setVisibility(View.VISIBLE);
            textENS.setText(address);
            if (toAddressEditText.hasFocus())
                KeyboardUtils.hideKeyboard(host.getCurrentFocus()); //user was waiting for ENS, not in the middle of typing a value etc
            checkIfWaitingForENS();
            toAddressError.setVisibility(View.GONE);
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
                .subscribe(this::onENSSuccess, this::onENSError);
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
}
