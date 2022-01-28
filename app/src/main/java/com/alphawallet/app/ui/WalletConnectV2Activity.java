package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.util.StyledStringBuilder;
import com.alphawallet.app.viewmodel.WalletConnectV2ViewModel;
import com.alphawallet.app.viewmodel.WalletConnectV2ViewModelFactory;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.widget.ChainName;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.TokenIcon;
import com.bumptech.glide.Glide;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import dagger.android.AndroidInjection;

public class WalletConnectV2Activity extends BaseActivity implements StandardFunctionInterface, WalletConnectClient.WalletDelegate
{
    private WalletConnectV2ViewModel viewModel;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private TextView statusText;
    private TextView textName;
    private ChainName chainName;
    private TokenIcon chainIcon;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;

    @Inject
    WalletConnectV2ViewModelFactory viewModelFactory;
    private String url;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        AndroidInjection.inject(this);

        setContentView(R.layout.activity_wallet_connect);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));
        initViews();
        initViewModel();
        this.url = retrieveQrCode();
        viewModel.prepare();
    }

    private WalletConnectV2SessionItem retrieveSession()
    {
        return (WalletConnectV2SessionItem) getIntent().getParcelableExtra("session");
    }

    private String retrieveQrCode()
    {
        return getIntent().getExtras().getString("qrCode");
    }

    private void initViewModel()
    {

        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(WalletConnectV2ViewModel.class);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        if (url == null)
        {
            progressBar.setVisibility(View.GONE);
            functionBar.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.VISIBLE);
            displaySessionStatus(retrieveSession());
        } else
        {
            WalletConnectClient.INSTANCE.setWalletDelegate(this);
            viewModel.pair(url);
        }
    }

    private void displaySessionStatus(WalletConnectV2SessionItem session)
    {
        statusText.setText(R.string.online);
        statusText.setTextColor(getColor(R.color.nasty_green));
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);

        if (session.icon == null)
        {
            icon.setImageResource(R.drawable.ic_coin_eth_small);
        } else
        {
            Glide.with(this)
                    .load(session.icon)
                    .circleCrop()
                    .into(icon);
        }
        peerName.setText(session.name);
        textName.setText(session.name);
        peerUrl.setText(session.url);
        chainName.setChainID(session.chainId);
        chainIcon.setVisibility(View.VISIBLE);
//            chainIcon.bindData(viewModel.getTokensService().getServiceToken(viewModel.getChainId(sessionId)));
//            viewModel.startGasCycle(viewModel.getChainId(sessionId));
    }

    private void initViews()
    {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        statusText = findViewById(R.id.connection_status);
        textName = findViewById(R.id.text_name);
        chainName = findViewById(R.id.chain_name);
        chainIcon = findViewById(R.id.chain_icon);

        progressBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.GONE);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_end_session)));
        functionBar.setVisibility(View.GONE);
    }

    @Override
    public void onSessionDelete(@NonNull WalletConnect.Model.DeletedSession deletedSession)
    {

    }

    @Override
    public void onSessionNotification(@NonNull WalletConnect.Model.SessionNotification sessionNotification)
    {

    }

    @Override
    public void onSessionProposal(@NonNull WalletConnect.Model.SessionProposal sessionProposal)
    {
        Log.d("seaborn", "onSessionProposal");
        List<String> chains = sessionProposal.getChains();
        for (String chain : chains)
        {
            Log.d("seaborn", chain);
        }
        runOnUiThread(() ->
        {
            progressBar.setVisibility(View.GONE);
            functionBar.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.VISIBLE);
            showProposalDialog(sessionProposal);
        });
    }

    @Override
    public void onSessionRequest(@NonNull WalletConnect.Model.SessionRequest sessionRequest)
    {

    }

    private void showProposalDialog(WalletConnect.Model.SessionProposal sessionProposal)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder
                .setIcon(icon.getDrawable())
                .setTitle("Wallet Connect")
                .setMessage(buildMessage(sessionProposal.getUrl(), sessionProposal.getChains()))
                .setPositiveButton(R.string.dialog_approve, (d, w) ->
                {
                    approve(sessionProposal);
//                    client.approveSession(Arrays.asList(accounts), chainIdOverride);
//                    viewModel.createNewSession(getSessionId(), client.getPeerId(), client.getRemotePeerId(),
//                            new Gson().toJson(session), new Gson().toJson(peer), chainIdOverride);
//                    progressBar.setVisibility(View.GONE);
//                    functionBar.setVisibility(View.VISIBLE);
//                    infoLayout.setVisibility(View.VISIBLE);
//                    setupClient(getSessionId());
//                    if (fromDappBrowser)
//                    {
//                        //switch back to dappBrowser
//                        switchToDappBrowser();
//                    }
                })
                .setNeutralButton(R.string.hint_network_chain_id, (d, w) ->
                {
                    //pop open the selection dialog
//                    Intent intent = new Intent(this, SelectNetworkActivity.class);
//                    intent.putExtra(C.EXTRA_SINGLE_ITEM, true);
//                    intent.putExtra(C.EXTRA_CHAIN_ID, chainIdOverride);
//                    getNetwork.launch(intent);
                })
                .setNegativeButton(R.string.dialog_reject, (d, w) ->
                {
                    reject(sessionProposal);
                    finish();
                })
                .setCancelable(false)
                .create();
        dialog.show();
    }

    private void reject(WalletConnect.Model.SessionProposal sessionProposal)
    {
        WalletConnectClient.INSTANCE.reject(new WalletConnect.Params.Reject(getString(R.string.message_reject_request), sessionProposal.getTopic()), new WalletConnect.Listeners.SessionReject()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.RejectedSession rejectedSession)
            {

            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {

            }
        });
    }

    private void approve(WalletConnect.Model.SessionProposal sessionProposal)
    {
        List<String> accounts = getAccounts(sessionProposal.getChains());
        WalletConnect.Params.Approve approve = new WalletConnect.Params.Approve(sessionProposal, accounts);
        WalletConnectClient.INSTANCE.approve(approve, new WalletConnect.Listeners.SessionApprove()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.SettledSession settledSession)
            {
                showSessionsActivity();
                finish();
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {

            }
        });
    }

    private void showSessionsActivity()
    {
        Intent intent = new Intent(getApplication(), WalletConnectSessionActivity.class);
        intent.putExtra("wallet", viewModel.defaultWallet().getValue());
        startActivity(intent);
    }

    private List<String> getAccounts(List<String> chains)
    {
        List<String> result = new ArrayList<>();
        for (String chain : chains)
        {
            result.add(chain + ":" + viewModel.defaultWallet().getValue().address);
        }
        return result;
    }

    private Spannable buildMessage(String url, List<String> chains)
    {
        long networkId = Long.parseLong(chains.get(0).split(":")[1]);
        StyledStringBuilder sb = new StyledStringBuilder();
        sb.append(url);
        sb.startStyleGroup().append("\n\n").append(EthereumNetworkBase.getShortChainName(networkId));
        sb.setColor(ContextCompat.getColor(this, EthereumNetworkBase.getChainColour(networkId)));
        sb.applyStyles();
        return sb;
    }
}
