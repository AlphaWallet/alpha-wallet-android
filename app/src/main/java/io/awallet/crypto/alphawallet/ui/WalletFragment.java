package io.awallet.crypto.alphawallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import io.awallet.crypto.alphawallet.entity.TokenInterface;
import io.awallet.crypto.alphawallet.entity.TokensReceiver;
import io.stormbird.token.tools.TokenDefinition;
import org.xml.sax.SAXException;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.ErrorEnvelope;
import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.Wallet;
import io.awallet.crypto.alphawallet.ui.widget.adapter.TokensAdapter;
import io.awallet.crypto.alphawallet.util.TabUtils;
import io.awallet.crypto.alphawallet.viewmodel.WalletViewModel;
import io.awallet.crypto.alphawallet.viewmodel.WalletViewModelFactory;
import io.awallet.crypto.alphawallet.widget.ProgressView;
import io.awallet.crypto.alphawallet.widget.SystemView;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

import static io.awallet.crypto.alphawallet.C.ErrorCode.EMPTY_COLLECTION;

/**
 * Created by justindeguzman on 2/28/18.
 */

public class WalletFragment extends Fragment implements View.OnClickListener, TokenInterface
{
    private static final String TAG = "WFRAG";

    @Inject
    WalletViewModelFactory walletViewModelFactory;
    private WalletViewModel viewModel;

    private TokensReceiver tokenReceiver;

    private SystemView systemView;
    private ProgressView progressView;
    private TokensAdapter adapter;
    private int networkId = 0;

    private Wallet wallet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        adapter = new TokensAdapter(getContext(), this::onTokenClick);
        adapter.setHasStableIds(true);
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        progressView = view.findViewById(R.id.progress_view);
        progressView.hide();

        progressView.setWhiteCircle();

        RecyclerView list = view.findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, walletViewModelFactory)
                .get(WalletViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.tokens().observe(this, this::onTokens);
        viewModel.total().observe(this, this::onTotal);
        viewModel.queueProgress().observe(this, progressView::updateProgress);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWalletBalance().observe(this, this::onBalanceChanged);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.refreshTokens().observe(this, this::refreshTokens);
        viewModel.tokenUpdate().observe(this, this::onToken);
        viewModel.endUpdate().observe(this, this::checkTokens);

        refreshLayout.setOnRefreshListener(viewModel::fetchTokens);

        tokenReceiver = new TokensReceiver(getActivity(), this);

        initTabLayout(view);

        return view;
    }

    private void checkTokens(Boolean dummy)
    {
        if (adapter.checkTokens())
        {
            viewModel.fetchTokens(); //require a full token refresh; number of tokens has changed
        }
    }

    private void onToken(Token token)
    {
        adapter.updateTokenCheck(token);
    }

    private void initTabLayout(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.currency));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.assets));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()) {
                    case 0: {
                        adapter.setFilterType(TokensAdapter.FILTER_ALL);
                        viewModel.fetchTokens();
                        break;
                    }
                    case 1: {
                        adapter.setFilterType(TokensAdapter.FILTER_CURRENCY);
                        viewModel.fetchTokens();
                        break;
                    }
                    case 2: {
                        adapter.setFilterType(TokensAdapter.FILTER_ASSETS);
                        viewModel.fetchTokens();
                        break;
                    }
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        TabUtils.changeTabsFont(getContext(), tabLayout);
        TabUtils.reflex(tabLayout);
    }

    private void onTotal(BigDecimal totalInCurrency) {
        adapter.setTotal(totalInCurrency);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                viewModel.showAddToken(getContext());
            }
            break;
            case R.id.action_edit: {
                viewModel.showEditTokens(getContext());
            }
            break;
            case android.R.id.home: {
                //adapter.clear();
                //viewModel.showTransactions(getContext());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void onTokenClick(View view, Token token) {
        Context context = view.getContext();
        token.clickReact(viewModel, context);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    private void onTokens(Token[] tokens)
    {
        for (Token t : tokens) if(!t.isTerminated() && t.hasPositiveBalance()) Log.d(TAG, t.getAddress() + " : " + t.getFullName());
        adapter.setTokens(tokens);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        if (errorEnvelope.code == EMPTY_COLLECTION) {
            systemView.showEmpty(getString(R.string.no_tokens));
        } else {
            systemView.showError(getString(R.string.error_fail_load_tokens), this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchTokens();
            }
            break;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getContext().unregisterReceiver(tokenReceiver);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        this.wallet = wallet;
        viewModel.fetchTokens();
        //get the XML address
        try
        {
            TokenDefinition ad = new TokenDefinition(getResources().getAssets().open("TicketingContract.xml"),
                    Locale.getDefault().getDisplayLanguage());
            String contractAddress = ad.getContractAddress(networkId);
            if (contractAddress != null)
            {
                adapter.setLiveTokenAddress(contractAddress.toLowerCase());
            }
        }
        catch (IOException |SAXException e)
        {
            e.printStackTrace();
        }
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        networkId = networkInfo.chainId;
//        adapter.setDefaultNetwork(networkInfo);
//        setBottomMenu(R.menu.menu_main_network);
    }

    private void onBalanceChanged(Map<String, String> balance) {
//        ActionBar actionBar = getSupportActionBar();
//        NetworkInfo networkInfo = viewModel.defaultNetwork().getValue();
//        Wallet wallet = viewModel.defaultWallet().getValue();
//        if (actionBar == null || networkInfo == null || wallet == null) {
//            return;
//        }
//        if (TextUtils.isEmpty(balance.get(C.USD_SYMBOL))) {
//            actionBar.setTitle(balance.get(networkInfo.symbol) + " " + networkInfo.symbol);
//            actionBar.setSubtitle("");
//        } else {
//            actionBar.setTitle("$" + balance.get(C.USD_SYMBOL));
//            actionBar.setSubtitle(balance.get(networkInfo.symbol) + " " + networkInfo.symbol);
//        }
    }

    /**
     * This is triggered by transaction view after we have found new tokens by scanning the transaction history
     * @param aBoolean - dummy param
     */
    private void refreshTokens(Boolean aBoolean)
    {
        viewModel.fetchTokens();
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        viewModel.abortAndRestart();
        adapter.clear();
    }

    @Override
    public void addedToken()
    {

    }
}
