package com.alphawallet.app.widget;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AWWalletConnectClient;
import com.alphawallet.app.util.Hex;
import com.alphawallet.app.viewmodel.walletconnect.SignMethodDialogViewModel;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;
import com.walletconnect.walletconnectv2.core.exceptions.WalletConnectException;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import timber.log.Timber;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

public class SignMethodDialog extends BottomSheetDialog
{
    private final FunctionButtonBar functionBar;
    private final TextView dAppName;
    private final ImageView logo;
    private final TextView url;
    private final TextView walletTv;
    private final TextView message;
    private final ImageView networkIcon;
    private final ChainName networkName;
    private final Activity activity;
    private final ProgressBar progressBar;
    private WalletConnect.Model.SettledSession settledSession;
    private String messageTextHex;
    private final String walletAddress;
    private SignMethodDialogViewModel viewModel;

    public SignMethodDialog(@NonNull Activity activity, WalletConnect.Model.SettledSession settledSession, WalletConnect.Model.SessionRequest sessionRequest)
    {
        super(activity, R.style.FullscreenBottomSheetDialogStyle);
        this.activity = activity;
        this.settledSession = settledSession;
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_sign_method, null);
        setContentView(view);
        initViewModel();
        setCancelable(false);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        progressBar = findViewById(R.id.progress);
        logo = findViewById(R.id.logo);
        dAppName = findViewById(R.id.dapp_name);
        url = findViewById(R.id.url);
        walletTv = findViewById(R.id.wallet);
        message = findViewById(R.id.message);
        networkIcon = findViewById(R.id.network_icon);
        networkName = findViewById(R.id.network_name);
        functionBar = findViewById(R.id.layoutButtons);

        List<String> icons = settledSession.getPeerAppMetaData().getIcons();

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
        dAppName.setText(settledSession.getPeerAppMetaData().getName());
        url.setText(settledSession.getPeerAppMetaData().getUrl());
        String params = sessionRequest.getRequest().getParams();
        params = params.substring(1);
        params = params.substring(0, params.length() - 1);
        String[] array = params.split(", ");
        walletAddress = array[1];
        walletTv.setText(walletAddress);
        messageTextHex = array[0];
        message.setText(Hex.hexToUtf8(messageTextHex));

        long chainID = Long.parseLong(sessionRequest.getChainId().split(":")[1]);
        networkIcon.setImageResource(EthereumNetworkRepository.getChainLogo(chainID));
        networkName.setChainID(chainID);
        functionBar.setupFunctions(new StandardFunctionInterface()
        {
            @Override
            public void handleClick(String action, int actionId)
            {
                if (actionId == R.string.dialog_approve)
                {
                    approve(sessionRequest);
                } else if (actionId == R.string.dialog_reject)
                {
                    reject(sessionRequest);
                }
            }
        }, Arrays.asList(R.string.dialog_approve, R.string.dialog_reject));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider((ViewModelStoreOwner) activity).get(SignMethodDialogViewModel.class);
        viewModel.completed().observe((LifecycleOwner) activity, this::onCompleted);
    }

    private void onCompleted(Boolean completed)
    {
        if (completed)
        {
            dismiss();
            AWWalletConnectClient.viewModel = null;
        }
    }

    private void reject(WalletConnect.Model.SessionRequest sessionRequest)
    {
        WalletConnect.Model.JsonRpcResponse jsonRpcResponse = new WalletConnect.Model.JsonRpcResponse.JsonRpcError(sessionRequest.getRequest().getId(), new WalletConnect.Model.JsonRpcResponse.Error(0, "User rejected."));
        WalletConnect.Params.Response response = new WalletConnect.Params.Response(sessionRequest.getTopic(), jsonRpcResponse);
        try
        {
            WalletConnectClient.INSTANCE.respond(response, Timber::e);
        } catch (WalletConnectException e)
        {
            Timber.e(e);
        }
        dismiss();
    }

    private void approve(WalletConnect.Model.SessionRequest sessionRequest)
    {
        AWWalletConnectClient.viewModel = viewModel;
        EthereumMessage ethereumMessage = new EthereumMessage(messageTextHex, null, 0, SignMessageType.SIGN_PERSONAL_MESSAGE);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.sign(activity, ethereumMessage, walletAddress, sessionRequest);
    }

}
