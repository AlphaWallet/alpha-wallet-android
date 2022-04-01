package com.alphawallet.app.ui;

import android.app.AlertDialog;
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

import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.walletconnect.WalletConnectV2SessionItem;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.alphawallet.app.ui.widget.adapter.ChainAdapter;
import com.alphawallet.app.ui.widget.adapter.MethodAdapter;
import com.alphawallet.app.ui.widget.adapter.WalletAdapter;
import com.alphawallet.app.util.LayoutHelper;
import com.alphawallet.app.viewmodel.WalletConnectV2ViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.bumptech.glide.Glide;
import com.walletconnect.walletconnectv2.client.WalletConnect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletConnectV2Activity extends BaseActivity implements StandardFunctionInterface, AWWalletConnectClient.WalletConnectV2Callback
{
    @Inject
    AWWalletConnectClient awWalletConnectClient;
    private WalletConnectV2ViewModel viewModel;

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
            new Handler().postDelayed(() -> awWalletConnectClient.pair(url), 3000);
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
        } else
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
        } else
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
        } else
        {
            functionBar.setupFunctions(new StandardFunctionInterface()
            {
                @Override
                public void handleClick(String action, int actionId)
                {
                    if (actionId == R.string.dialog_approve)
                    {
                        approve(AWWalletConnectClient.sessionProposal);
                    } else
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
            AlertDialog.Builder builder = new AlertDialog.Builder(WalletConnectV2Activity.this);
            AlertDialog dialog = builder.setTitle(R.string.dialog_title_disconnect_session)
                    .setPositiveButton(R.string.dialog_ok, (d, w) ->
                    {
                        killSession(session.sessionId);
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) ->
                    {
                        d.dismiss();
                    })
                    .create();
            dialog.show();
        });
    }

    private void killSession(String sessionId)
    {
        awWalletConnectClient.disconnect(sessionId, this);
    }

    private void reject(WalletConnect.Model.SessionProposal sessionProposal)
    {
        awWalletConnectClient.reject(sessionProposal, this);
    }

    private void approve(WalletConnect.Model.SessionProposal sessionProposal)
    {
        List<String> accounts = getAccounts(sessionProposal.getChains());
        awWalletConnectClient.approve(sessionProposal, accounts, this);
    }

    private void showSessionsActivity()
    {
        Intent intent = new Intent(getApplication(), WalletConnectSessionActivity.class);
        startActivity(intent);
    }

    private List<String> getAccounts(List<String> chains)
    {
        List<Wallet> wallets = walletAdapter.getSelectedWallets();
        List<String> result = new ArrayList<>();
        for (String chain : chains)
        {
            for (Wallet wallet : wallets)
            {
                result.add(chain + ":" + wallet.address);
            }
        }
        return result;
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
