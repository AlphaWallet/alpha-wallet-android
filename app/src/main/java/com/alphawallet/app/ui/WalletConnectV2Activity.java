package com.alphawallet.app.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
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
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.service.AWWalletConnectClient;
import com.alphawallet.app.ui.widget.adapter.ChainAdapter;
import com.alphawallet.app.ui.widget.adapter.MethodAdapter;
import com.alphawallet.app.ui.widget.adapter.WalletAdapter;
import com.alphawallet.app.util.LayoutHelper;
import com.alphawallet.app.viewmodel.WalletConnectV2ViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.bumptech.glide.Glide;
import com.walletconnect.walletconnectv2.client.WalletConnect;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletConnectV2Activity extends BaseActivity implements StandardFunctionInterface
{
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
    private Wallet[] wallets;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet_connect_v2);
        toolbar();
        setTitle(getString(R.string.title_wallet_connect));
        initViews();
        initViewModel();
        this.session = retrieveSession();
//        displaySessionStatus(this.session);
        viewModel.fetchWallets();
    }

    private WalletConnectV2SessionItem retrieveSession()
    {
        return getIntent().getParcelableExtra("session");
    }

    private void initViewModel()
    {

        viewModel = new ViewModelProvider(this)
                .get(WalletConnectV2ViewModel.class);

        viewModel.wallets().observe(this, this::onWalletsFetched);
    }

    private void onWalletsFetched(Wallet[] wallets)
    {
        this.wallets = wallets;
        displaySessionStatus(session);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);
        displaySessionStatus(session);
    }

    private void displaySessionStatus(WalletConnectV2SessionItem session)
    {
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
        peerUrl.setText(session.url);

        chainList.setAdapter(new ChainAdapter(this, session.chains));
        walletList.setAdapter(new WalletAdapter(this, this.wallets));
        methodList.setAdapter(new MethodAdapter(this, session.methods));
        resizeList();

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

//        functionBar.setupFunctions(new StandardFunctionInterface()
//        {
//            @Override
//            public void handleClick(String action, int actionId)
//            {
//                endSessionDialog();
//            }
//        }, Collections.singletonList(R.string.action_end_session));
    }

    private void resizeList()
    {
        LayoutHelper.resizeList(chainList);
        LayoutHelper.resizeList(walletList);
        LayoutHelper.resizeList(methodList);
    }

    private List<String> getChains(List<String> accounts)
    {
        List<String> result = new ArrayList<>();
        for (String account : accounts)
        {
            result.add(account.substring(0, account.lastIndexOf(":")));
        }
        return result;
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
                        killSession(session);
                    })
                    .setNegativeButton(R.string.action_cancel, (d, w) ->
                    {
                        d.dismiss();
                    })
                    .create();
            dialog.show();
        });
    }

    private void killSession(WalletConnectV2SessionItem session)
    {
        WalletConnectClient.INSTANCE.disconnect(new WalletConnect.Params.Disconnect(session.sessionId, "User disconnect the session."), new WalletConnect.Listeners.SessionDelete()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.DeletedSession deletedSession)
            {
                finish();
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {

            }
        });
    }

    public void onSessionProposal(@NonNull WalletConnect.Model.SessionProposal sessionProposal)
    {
        runOnUiThread(() ->
        {

            progressBar.setVisibility(View.GONE);
            functionBar.setVisibility(View.VISIBLE);
            infoLayout.setVisibility(View.VISIBLE);

            displaySessionStatus(sessionProposal);

        });
    }

    private void displaySessionStatus(WalletConnect.Model.SessionProposal sessionProposal)
    {

        progressBar.setVisibility(View.GONE);
        functionBar.setVisibility(View.VISIBLE);
        infoLayout.setVisibility(View.VISIBLE);

        Glide.with(getApplication())
                .load(sessionProposal.getIcon())
                .circleCrop()
                .into(icon);
        peerName.setText(sessionProposal.getName());
        peerUrl.setText(sessionProposal.getUrl());

        chainList.setAdapter(new ChainAdapter(this, sessionProposal.getChains()));
//        walletList.setAdapter(new WalletAdapter(this, getWallets()));
        methodList.setAdapter(new MethodAdapter(this, sessionProposal.getMethods()));
        resizeList();
    }

    private void reject(WalletConnect.Model.SessionProposal sessionProposal)
    {
        WalletConnectClient.INSTANCE.reject(new WalletConnect.Params.Reject(getString(R.string.message_reject_request), sessionProposal.getTopic()), new WalletConnect.Listeners.SessionReject()
        {
            @Override
            public void onSuccess(@NonNull WalletConnect.Model.RejectedSession rejectedSession)
            {
                finish();
            }

            @Override
            public void onError(@NonNull Throwable throwable)
            {
            }
        });
    }

    private void approve(WalletConnect.Model.SessionProposal sessionProposal)
    {
        List<String> accounts = getAccounts(session.chains);
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
//        intent.putExtra("wallet", viewModel.defaultWallet().getValue());
        startActivity(intent);
    }

    private List<String> getAccounts(List<String> chains)
    {
        List<String> result = new ArrayList<>();
        for (String chain : chains)
        {
//            result.add(chain + ":" + viewModel.defaultWallet().getValue().address);
        }
        return result;
    }

}
