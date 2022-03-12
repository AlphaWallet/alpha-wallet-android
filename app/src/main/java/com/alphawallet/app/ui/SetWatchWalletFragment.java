package com.alphawallet.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnSetWatchWalletListener;
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.widget.InputAddress;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Created by James on 26/07/2019.
 * Stormbird in Sydney
 */
@AndroidEntryPoint
public class SetWatchWalletFragment extends ImportFragment implements AddressReadyCallback
{
    private static final OnSetWatchWalletListener dummyWatchWalletListener = key -> {
    };

    private InputAddress watchAddress;
    private Button importButton;
    private OnSetWatchWalletListener onSetWatchWalletListener = dummyWatchWalletListener;

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

        watchAddress = getView().findViewById(R.id.input_watch_address);
        importButton = getView().findViewById(R.id.import_action_ww);
        importButton.setOnClickListener(this);
        updateButtonState(false);
        watchAddress.setAddressCallback(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if ((watchAddress == null || watchAddress.getEditText() == null) && getActivity() != null) setupView();
    }

    @Override
    public void onClick(View view)
    {
        if (watchAddress == null && getActivity() == null) return;
        watchAddress.getAddress();
    }

    @Override
    public void addressReady(String address, String ensName)
    {
        KeyboardUtils.hideKeyboard(watchAddress.getInputView());
        onSetWatchWalletListener.onWatchWallet(address);
    }

    @Override
    public void addressValid(boolean valid)
    {
        if (importButton.isActivated() != valid)
        {
            updateButtonState(valid);
        }
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

    public void setAddress(String address)
    {
        if (address == null || getActivity() == null) return;
        watchAddress = getView().findViewById(R.id.input_watch_address);
        watchAddress.setAddress(address);
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
        watchAddress.getAddress();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {

    }

    @Override
    public void afterTextChanged(Editable s)
    {

    }
}
