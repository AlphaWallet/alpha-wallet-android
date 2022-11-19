package com.alphawallet.app.ui;

import static java.util.stream.Collectors.toList;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.NamespaceParser;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.ui.widget.adapter.ChainAdapter;
import com.alphawallet.app.ui.widget.adapter.MethodAdapter;
import com.alphawallet.app.ui.widget.adapter.WalletAdapter;
import com.alphawallet.app.util.LayoutHelper;
import com.alphawallet.app.viewmodel.SelectNetworkFilterViewModel;
import com.alphawallet.app.viewmodel.WalletConnectV2ViewModel;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.bumptech.glide.Glide;
import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletConnectV2Activity extends BaseActivity implements StandardFunctionInterface, AWWalletConnectClient.WalletConnectV2Callback
{
    @Inject
    AWWalletConnectClient awWalletConnectClient;
    private WalletConnectV2ViewModel viewModel;
    private SelectNetworkFilterViewModel selectNetworkFilterViewModel;

    private ImageView icon;
    private TextView peerName;
    private TextView peerUrl;
    private ProgressBar progressBar;
    private LinearLayout infoLayout;
    private FunctionButtonBar functionBar;
    private ListView chainList;
    private ListView walletList;
    private ListView methodList;

    private WalletConnectV2SessionItem session;
    private WalletAdapter walletAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet_connect_v2);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));
        initViews();

        String url = retrieveUrl();
        if (!TextUtils.isEmpty(url))
        {
            progressBar.setVisibility(View.VISIBLE);
            awWalletConnectClient.pair(url, (msg) -> {
                if (TextUtils.isEmpty(msg))
                {
                    return;
                }
                new Handler().post(() -> {
                    Toast.makeText(WalletConnectV2Activity.this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
            return;
        }

        this.session = retrieveSession(getIntent());
        initViewModel();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        this.session = retrieveSession(intent);
        initViewModel();
    }

    private String retrieveUrl()
    {
        return getIntent().getStringExtra("url");
    }

    private WalletConnectV2SessionItem retrieveSession(Intent intent)
    {
        return intent.getParcelableExtra("session");
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(WalletConnectV2ViewModel.class);

        viewModel.wallets().observe(this, this::onWalletsFetched);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);

        selectNetworkFilterViewModel = new ViewModelProvider(this)
                .get(SelectNetworkFilterViewModel.class);
    }

    private void onWalletsFetched(Wallet[] wallets)
    {
        tryDisplaySessionStatus();
    }

    private void tryDisplaySessionStatus()
    {
        if (viewModel.wallets().getValue() != null && viewModel.defaultWallet().getValue() != null)
        {
            displaySessionStatus(session);
            progressBar.setVisibility(View.GONE);
            functionBar.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void onDefaultWallet(Wallet wallet)
    {
        tryDisplaySessionStatus();
    }

    private void displaySessionStatus(WalletConnectV2SessionItem session)
    {
        if (session.icon == null)
        {
            icon.setImageResource(R.drawable.ic_coin_eth_small);
        }
        else
        {
            Glide.with(this)
                    .load(session.icon)
                    .circleCrop()
                    .into(icon);
        }
        peerName.setText(session.name);
        peerUrl.setText(session.url);

        chainList.setAdapter(new ChainAdapter(this, session.chains));
        if (session.settled)
        {
            walletAdapter = new WalletAdapter(this, findWallets(session.wallets));
        }
        else
        {
            walletAdapter = new WalletAdapter(this, viewModel.wallets().getValue(), viewModel.defaultWallet().getValue());
        }
        walletList.setAdapter(walletAdapter);
        methodList.setAdapter(new MethodAdapter(this, session.methods));
        resizeList();

        if (session.settled)
        {
            functionBar.setupFunctions(new StandardFunctionInterface()
            {
                @Override
                public void handleClick(String action, int actionId)
                {
                    endSessionDialog();
                }
            }, Collections.singletonList(R.string.action_end_session));
        }
        else
        {
            functionBar.setupFunctions(new StandardFunctionInterface()
            {
                @Override
                public void handleClick(String action, int actionId)
                {
                    if (actionId == R.string.dialog_approve)
                    {
                        approve(AWWalletConnectClient.sessionProposal);
                    }
                    else
                    {
                        reject(AWWalletConnectClient.sessionProposal);
                    }
                }
            }, Arrays.asList(R.string.dialog_approve, R.string.dialog_reject));
        }

    }

    private List<Wallet> findWallets(List<String> addresses)
    {
        List<Wallet> result = new ArrayList<>();
        Map<String, Wallet> map = toMap(Objects.requireNonNull(viewModel.wallets().getValue()));
        for (String address : addresses)
        {
            Wallet wallet = map.get(address);
            if (wallet == null)
            {
                wallet = new Wallet(address);
            }
            result.add(wallet);
        }
        return result;
    }

    private Map<String, Wallet> toMap(Wallet[] wallets)
    {
        HashMap<String, Wallet> map = new HashMap<>();
        for (Wallet wallet : wallets)
        {
            map.put(wallet.address, wallet);
        }
        return map;
    }

    private void resizeList()
    {
        LayoutHelper.resizeList(chainList);
        LayoutHelper.resizeList(walletList);
        LayoutHelper.resizeList(methodList);
    }

    private void initViews()
    {
        progressBar = findViewById(R.id.progress);
        infoLayout = findViewById(R.id.layout_info);
        icon = findViewById(R.id.icon);
        peerName = findViewById(R.id.peer_name);
        peerUrl = findViewById(R.id.peer_url);
        chainList = findViewById(R.id.chain_list);
        walletList = findViewById(R.id.wallet_list);
        methodList = findViewById(R.id.method_list);

        progressBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.GONE);

        functionBar = findViewById(R.id.layoutButtons);
        functionBar.setupFunctions(this, Arrays.asList(R.string.dialog_approve, R.string.dialog_reject));

        functionBar.setVisibility(View.GONE);
    }

    private void endSessionDialog()
    {
        runOnUiThread(() ->
        {

            AWalletAlertDialog dialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
            dialog.setTitle(R.string.dialog_title_disconnect_session);
            dialog.setButton(R.string.action_close, v -> {
                dialog.dismiss();
                killSession(session.sessionId);
            });
            dialog.setSecondaryButton(R.string.action_cancel, v -> dialog.dismiss());
            dialog.setCancelable(false);
            dialog.show();
        });
    }

    private void killSession(String sessionId)
    {
        awWalletConnectClient.disconnect(sessionId, this);
    }

    private void reject(Sign.Model.SessionProposal sessionProposal)
    {
        awWalletConnectClient.reject(sessionProposal, this);
    }

    private void approve(Sign.Model.SessionProposal sessionProposal)
    {
        List<Long> disabledNetworks = disabledNetworks(sessionProposal.getRequiredNamespaces());
        if (disabledNetworks.isEmpty())
        {
            awWalletConnectClient.approve(sessionProposal, getSelectedAccounts(), this);
        }
        else
        {
            showDialog(disabledNetworks);
        }
    }

    private void showDialog(List<Long> disabledNetworks)
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(this);
        dialog.setMessage(String.format(getString(R.string.network_must_be_enabled), joinNames(disabledNetworks)));
        dialog.setButton(R.string.select_active_networks, view -> {
            Intent intent = new Intent(this, SelectNetworkFilterActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });
        dialog.setSecondaryButton(R.string.action_cancel, (view) -> dialog.dismiss());
        dialog.show();
    }

    @NonNull
    private String joinNames(List<Long> disabledNetworks)
    {
        return disabledNetworks.stream()
                .map((chainId) -> {
                    NetworkInfo network = selectNetworkFilterViewModel.getNetworkByChain(chainId);
                    if (network != null)
                    {
                        return network.name;
                    }
                    return String.valueOf(chainId);
                })
                .collect(Collectors.joining(", "));
    }

    private List<Long> disabledNetworks(Map<String, Sign.Model.Namespace.Proposal> requiredNamespaces)
    {
        NamespaceParser namespaceParser = new NamespaceParser();
        namespaceParser.parseProposal(requiredNamespaces);
        List<Long> enabledChainIds = selectNetworkFilterViewModel.getActiveNetworks();
        List<Long> result = new ArrayList<>();
        List<Long> chains = namespaceParser.getChains().stream().map((s) -> Long.parseLong(s.split(":")[1])).collect(toList());
        for (Long chainId : chains)
        {
            if (!enabledChainIds.contains(chainId))
            {
                result.add(chainId);
            }
        }
        return result;
    }

    private List<String> getSelectedAccounts()
    {
        return walletAdapter.getSelectedWallets().stream()
                .map((wallet) -> wallet.address).collect(toList());
    }

    @Override
    public void onSessionProposalApproved()
    {
        finish();
    }

    @Override
    public void onSessionProposalRejected()
    {
        finish();
    }

    @Override
    public void onSessionDisconnected()
    {
        finish();
    }
}
