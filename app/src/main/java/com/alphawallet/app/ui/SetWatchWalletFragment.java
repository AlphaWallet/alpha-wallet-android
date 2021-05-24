package com.alphawallet.app.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.ui.widget.OnSetWatchWalletListener;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.EnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

/**
 * Created by James on 26/07/2019.
 * Stormbird in Sydney
 */
public class SetWatchWalletFragment extends Fragment implements View.OnClickListener, TextWatcher, LayoutCallbackListener
{
    private static final OnSetWatchWalletListener dummyWatchWalletListener = key -> {
    };
    private static final String validator = "[^x^a-f^A-F^0-9]";

    private PasswordInputView watchAddress;
    private Button importButton;
    private OnSetWatchWalletListener onSetWatchWalletListener = dummyWatchWalletListener;
    private Pattern pattern;
    private AWEnsResolver ensResolver;
    private AWalletAlertDialog dialog;

    public static SetWatchWalletFragment create()
    {
        return new SetWatchWalletFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_watch_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setupView();
    }

    private void setupView()
    {
        View.inflate(getActivity(), R.layout.fragment_watch_wallet, null);

        watchAddress = getActivity().findViewById(R.id.input_watch_address);
        importButton = getActivity().findViewById(R.id.import_action_ww);
        importButton.setOnClickListener(this);
        watchAddress.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);

        watchAddress.setLayoutListener(getActivity(), this);
    }

    private boolean paused = false;

    @Override
    public void onResume()
    {
        super.onResume();
        if ((watchAddress == null || watchAddress.getEditText() == null) && getActivity() != null) setupView();
    }

    @Override
    public void onClick(View view)
    {
        handleWatchAddress(view);
    }

    private void handleWatchAddress(View view)
    {
        if (watchAddress == null && getActivity() == null) return;
        if (watchAddress != null) watchAddress = getActivity().findViewById(R.id.input_watch_address);

        watchAddress.setError(null);
        String value = watchAddress.getText().toString();
        KeyboardUtils.hideKeyboard(view);

        if (!TextUtils.isEmpty(value))
        {
            if (Utils.isAddressValid(value))
            {
                onSetWatchWalletListener.onWatchWallet(value);
                return;
            }
            else
            {
                //try to resolve ENS
                getENSAddress(value);
                return;
            }
        }

        watchAddress.setError(getString(R.string.error_field_required));
    }

    private void updateButtonState(boolean enabled)
    {
        try
        {
            importButton.setActivated(enabled);
            importButton.setClickable(enabled);
            int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
            if (getContext() != null)
                importButton.setBackgroundColor(getContext().getColor(colorId));
        }
        catch (Exception e)
        {
            // Couldn't update state
        }
    }

    public void setOnSetWatchWalletListener(OnSetWatchWalletListener onSetWatchWalletListener)
    {
        this.onSetWatchWalletListener = onSetWatchWalletListener == null
                ? dummyWatchWalletListener
                : onSetWatchWalletListener;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        if (watchAddress.isErrorState())
            watchAddress.setError(null);
        String value = watchAddress.getText().toString();
        value = value.replaceAll("\\s+", "");
        final Matcher matcher = pattern.matcher(value);
        if (EnsResolver.isValidEnsName(value))
        {
            updateButtonState(true);
        }
        else if (matcher.find())
        {
            updateButtonState(false);
            watchAddress.setError(getString(R.string.ethereum_address_hint));
        }
        else if (Utils.isAddressValid(value) || EnsResolver.isValidEnsName(value))
        {
            updateButtonState(true);
        }
        else
        {
            if (value.length() > 42) watchAddress.setError(getString(R.string.ethereum_address_hint));
            updateButtonState(false);
        }
    }

    public void setAddress(String address)
    {
        if (address == null || getActivity() == null) return;
        watchAddress = getActivity().findViewById(R.id.input_watch_address);
        watchAddress.getEditText().setText(address);
    }

    private void getENSAddress(String name)
    {
        ensProgress();
        if (ensResolver == null)
            ensResolver = new AWEnsResolver(TokenRepository.getWeb3jService(MAINNET_ID), getContext());
        ensResolver.resolveENSAddress(name)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::fetchedENSAddress, this::onENSFail).isDisposed();
    }

    private void fetchedENSAddress(String address)
    {
        dialog.dismiss();
        if (!TextUtils.isEmpty(address))
        {
            onSetWatchWalletListener.onWatchWallet(address);
        }
        else
        {
            watchAddress.setError(getString(R.string.ethereum_address_hint));
        }
    }

    private void onENSFail(Throwable throwable)
    {
        dialog.dismiss();
        watchAddress.setError(getString(R.string.ethereum_address_hint));
    }

    @Override
    public void onLayoutShrunk()
    {
        if (importButton != null && importButton.getVisibility() == View.VISIBLE) importButton.setVisibility(View.GONE);
    }

    @Override
    public void onLayoutExpand()
    {
        if (importButton != null && importButton.getVisibility() == View.GONE) importButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInputDoneClick(View view)
    {
        handleWatchAddress(view);
    }

    private void ensProgress()
    {
        dialog = new AWalletAlertDialog(getActivity());
        dialog.setTitle(R.string.title_dialog_check_ens);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
