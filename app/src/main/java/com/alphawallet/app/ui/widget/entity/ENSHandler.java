package com.alphawallet.app.ui.widget.entity;

import android.app.Activity;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.entity.ENSCallback;
import com.alphawallet.app.ui.widget.adapter.AutoCompleteUrlAdapter;
import com.alphawallet.app.util.KeyboardUtils;

import com.alphawallet.app.R;

import com.alphawallet.app.widget.AWalletAlertDialog;

import static org.web3j.crypto.WalletUtils.isValidAddress;

/**
 * Created by James on 4/12/2018.
 * Stormbird in Singapore
 */
public class ENSHandler
{
    public  static final int ENS_RESOLVE_DELAY = 1500; //In milliseconds

    private final LinearLayout layoutENSResolve;
    //private final TextView myAddressText;
    private final AutoCompleteTextView toAddressEditText;
    private final TextView textENS;
    private TextWatcher ensTextWatcher;
    private String ensName;
    private final Handler handler;
    private final AutoCompleteUrlAdapter adapterUrl;
    private final TextView toAddressError;
    private final Activity host;
    private final Runnable runnable;
    private final ENSCallback ensCallback;

    private AWalletAlertDialog dialog;

    public volatile boolean waitingForENS = false;
    public boolean transferAfterENS = false;

    public ENSHandler(Activity host, Handler handler, AutoCompleteUrlAdapter adapter, Runnable processs, ENSCallback ensCallback)
    {
        this.layoutENSResolve = host.findViewById(R.id.layout_ens);
        this.toAddressEditText = host.findViewById(R.id.edit_to_address);
        this.textENS = host.findViewById(R.id.text_ens_resolve);
        this.toAddressError = host.findViewById(R.id.to_address_error);
        this.handler = handler;
        this.adapterUrl = adapter;
        this.host = host;
        this.runnable = processs;
        this.ensCallback = ensCallback;

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
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, ENS_RESOLVE_DELAY);
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
        handler.post(runnable);
    }

    public void onENSSuccess(String address)
    {
        if (address.equals("0"))
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
        ensCallback.ENSCheck(to);
    }

    public static boolean canBeENSName(String address)
    {
        return address.length() > 5 && !address.startsWith("0x") && address.contains(".");
    }
}
