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
import com.alphawallet.app.ui.widget.entity.AddressReadyCallback;
import com.alphawallet.app.util.AWEnsResolver;
import com.alphawallet.app.util.EnsResolver;
import com.alphawallet.app.util.KeyboardUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.InputAddress;
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
public class SetWatchWalletFragment extends Fragment implements View.OnClickListener, LayoutCallbackListener, AddressReadyCallback
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

        watchAddress = getActivity().findViewById(R.id.input_watch_address);
        importButton = getActivity().findViewById(R.id.import_action_ww);
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
        watchAddress = getActivity().findViewById(R.id.input_watch_address);
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
}
