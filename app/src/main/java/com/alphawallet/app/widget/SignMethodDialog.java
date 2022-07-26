package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.walletconnect.entity.BaseRequest;
import com.alphawallet.app.walletconnect.util.WalletConnectHelper;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.walletconnect.sign.client.Sign;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SignMethodDialog extends BottomSheetDialog
{
    private FunctionButtonBar functionBar;
    private TextView dAppName;
    private ImageView logo;
    private TextView url;
    private TextView walletTv;
    private TextView message;
    private ImageView networkIcon;
    private ChainName networkName;
    private final Activity activity;
    private ImageView closeButton;
    private final Sign.Model.SessionRequest sessionRequest;
    private final BaseRequest request;
    private String walletAddress;
    private SignMethodDialogViewModel viewModel;
    private final Signable signable;
    private SignDataWidget signDataWidget;
    private final Sign.Model.AppMetaData metaData;

    public SignMethodDialog(@NonNull Activity activity, Sign.Model.SessionRequest sessionRequest, BaseRequest request, Sign.Model.AppMetaData metaData)
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
        setCancelable(false);

        initViews();
        bindData();
    }

    private void bindData()
    {
        List<String> icons = Objects.requireNonNull(metaData).getIcons();

        if (icons.isEmpty())
        {
            logo.setImageResource(R.drawable.ic_coin_eth_small);
        } else
        {
            Glide.with(activity)
                    .load(icons.get(0))
                    .circleCrop()
                    .into(logo);
        }
        dAppName.setText(metaData.getName());
        url.setText(metaData.getUrl());
        walletAddress = request.getWalletAddress();
        walletTv.setText(walletAddress);

        long chainID = WalletConnectHelper.getChainId(Objects.requireNonNull(sessionRequest.getChainId()));
        networkIcon.setImageResource(EthereumNetworkRepository.getChainLogo(chainID));
        networkName.setChainID(chainID);
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

        closeButton.setOnClickListener(v ->
        {
            viewModel.reject(sessionRequest);
            dismiss();
        });

        if (signable.getMessageType() == SignMessageType.SIGN_PERSONAL_MESSAGE
                || signable.getMessageType() == SignMessageType.SIGN_MESSAGE)
        {
            message.setText(Hex.hexToUtf8(signable.getMessage()));
        } else
        {
            message.setVisibility(View.GONE);
            signDataWidget.setVisibility(View.VISIBLE);
            signDataWidget.setupSignData(request.getSignable());
        }
    }

    private void initViews()
    {
        logo = findViewById(R.id.logo);
        dAppName = findViewById(R.id.dapp_name);
        url = findViewById(R.id.url);
        walletTv = findViewById(R.id.wallet);
        message = findViewById(R.id.message);
        networkIcon = findViewById(R.id.network_icon);
        networkName = findViewById(R.id.network_name);
        functionBar = findViewById(R.id.layoutButtons);
        closeButton = findViewById(R.id.image_close);
        signDataWidget = findViewById(R.id.sign_widget);
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
        } else
        {
            functionBar.setPrimaryButtonWaiting();
            viewModel.sign(activity, wallet, sessionRequest, signable);
        }
    }
}
