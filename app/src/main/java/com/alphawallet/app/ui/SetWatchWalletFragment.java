package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.alphawallet.app.ui.widget.OnSetWatchWalletListener;
import com.alphawallet.app.util.Utils;

import com.alphawallet.app.R;

import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        watchAddress = getActivity().findViewById(R.id.input_watch_address);
        importButton = getActivity().findViewById(R.id.import_action_ww);
        importButton.setOnClickListener(this);
        watchAddress.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);

        watchAddress.setLayoutListener(getActivity(), this, getActivity().findViewById(R.id.bottom_marker_ww));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (watchAddress == null && getActivity() != null) setupView();
    }

    @Override
    public void onClick(View view)
    {
        handleWatchAddress(view);
    }

    private void handleWatchAddress(View view)
    {
        watchAddress.setError(null);
        String value = watchAddress.getText().toString();

        if (!TextUtils.isEmpty(value))
        {
            if (Utils.isAddressValid(value))
            {
                onSetWatchWalletListener.onWatchWallet(value);
                return;
            }
            else
            {
                watchAddress.setError(getString(R.string.ethereum_address_hint));
                return;
            }
        }

        watchAddress.setError(getString(R.string.error_field_required));
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setActivated(enabled);
        importButton.setClickable(enabled);
        int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
        if (getContext() != null)
            importButton.setBackgroundColor(getContext().getColor(colorId));
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
        final Matcher matcher = pattern.matcher(value);
        if (matcher.find())
        {
            updateButtonState(false);
            watchAddress.setError(getString(R.string.ethereum_address_hint));
        }
        else if (Utils.isAddressValid(value))
        {
            updateButtonState(true);
        }
        else
        {
            updateButtonState(false);
        }
    }

    public void setAddress(String address)
    {
        watchAddress.getEditText().setText(address);
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
}
