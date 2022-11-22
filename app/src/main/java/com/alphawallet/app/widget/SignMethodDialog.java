package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.token.entity.Signable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.walletconnect.android.Core;
import com.walletconnect.sign.client.Sign;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SignMethodDialog extends BottomSheetDialog
{
    private final Activity activity;
    private final Sign.Model.SessionRequest sessionRequest;
    private final BaseRequest request;
    private final Signable signable;
    private final Core.Model.AppMetaData metaData;
    private SignMethodDialogViewModel viewModel;
    private BottomSheetToolbarView toolbar;
    private AddressDetailView dappName;
    private AddressDetailView dappUrl;
    private AddressDetailView wallet;
    private NetworkDisplayWidget networkDisplayWidget;
    private SignDataWidget signDataWidget;
    private FunctionButtonBar functionBar;
    private String walletAddress;

    public SignMethodDialog(@NonNull Activity activity, Sign.Model.SessionRequest sessionRequest, BaseRequest request, Core.Model.AppMetaData metaData)
    {
        super(activity);
        this.activity = activity;
        this.metaData = metaData;
        this.sessionRequest = sessionRequest;
        this.request = request;
        this.signable = request.getSignable();
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_sign_method, null);
        setContentView(view);
        initViewModel();

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        initViews();
        bindData();
    }

    private void initViews()
    {
        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        functionBar = findViewById(R.id.layoutButtons);
        signDataWidget = findViewById(R.id.sign_widget);
        dappName = findViewById(R.id.dapp_name);
        dappUrl = findViewById(R.id.dapp_url);
        wallet = findViewById(R.id.wallet);
        networkDisplayWidget = findViewById(R.id.network_display_widget);
    }

    private void bindData()
    {
        List<String> icons = Objects.requireNonNull(metaData).getIcons();
        if (!icons.isEmpty())
        {
            toolbar.setLogo(getContext(), icons.get(0));
        }

        dappName.setupRequester(metaData.getName());
        dappUrl.setupRequester(metaData.getUrl());

        walletAddress = request.getWalletAddress();
        wallet.setupRequester(walletAddress);

        networkDisplayWidget.setNetwork(WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId())));

        signDataWidget.setupSignData(request.getSignable());

        functionBar.setupFunctions(new StandardFunctionInterface()
        {
            @Override
            public void handleClick(String action, int actionId)
            {
                if (actionId == R.string.action_confirm)
                {
                    approve();
                }
            }
        }, Collections.singletonList(R.string.action_confirm));

        toolbar.setCloseListener(v -> {
            viewModel.reject(sessionRequest);
            dismiss();
        });

        setOnCancelListener(dialog -> viewModel.reject(sessionRequest));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(SignMethodDialogViewModel.class);
    }

    @SuppressLint("CheckResult")
    private void approve()
    {
        AWWalletConnectClient.viewModel = viewModel;
        viewModel.completed().observe((LifecycleOwner) activity, completed ->
        {
            if (completed)
            {
                dismiss();
                AWWalletConnectClient.viewModel = null;
            }
        });

        viewModel.findWallet(walletAddress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWalletFound);
    }

    private void onWalletFound(Wallet wallet)
    {
        // The method find may return the first wallet if the specified wallet not found
        if (!wallet.address.equals(walletAddress) || wallet.watchOnly())
        {
            Toast.makeText(getContext(), activity.getString(R.string.wc_wallet_not_match), Toast.LENGTH_SHORT).show();
        }
        else
        {
            functionBar.setPrimaryButtonWaiting();
            viewModel.sign(activity, wallet, sessionRequest, signable);
        }
    }
}
