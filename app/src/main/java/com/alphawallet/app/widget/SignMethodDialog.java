package com.alphawallet.app.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.walletconnect.walletconnectv2.client.WalletConnect;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

public class SignMethodDialog extends BottomSheetDialog
{
    private final FunctionButtonBar functionBar;
    private final TextView dAppName;
    private final ImageView logo;
    private final TextView url;
    private final TextView wallet;
    private final TextView message;
    private final ImageView networkIcon;
    private final ChainName networkName;

    public SignMethodDialog(@NonNull Context context, WalletConnect.Model.SettledSession settledSession, WalletConnect.Model.SessionRequest sessionRequest)
    {
        super(context, R.style.FullscreenBottomSheetDialogStyle);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sign_method, null);
        setContentView(view);
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        logo = findViewById(R.id.logo);
        dAppName = findViewById(R.id.dapp_name);
        url = findViewById(R.id.url);
        wallet = findViewById(R.id.wallet);
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
            Glide.with(context)
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
        wallet.setText(array[1]);
        message.setText(array[0]);

        long chainID = Long.parseLong(sessionRequest.getChainId().split(":")[1]);
        networkIcon.setImageResource(EthereumNetworkRepository.getChainLogo(chainID));
        networkName.setChainID(chainID);
        functionBar.setupFunctions(new StandardFunctionInterface()
        {
            @Override
            public void handleClick(String action, int actionId)
            {

            }
        }, Arrays.asList(R.string.dialog_approve, R.string.dialog_reject));
    }
}
