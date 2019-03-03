package io.stormbird.wallet.ui;

import android.annotation.SuppressLint;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dagger.android.support.AndroidSupportInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.widget.adapter.TokensAdapter;
import io.stormbird.wallet.util.TabUtils;
import io.stormbird.wallet.viewmodel.WalletViewModel;
import io.stormbird.wallet.viewmodel.WalletViewModelFactory;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SystemView;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static io.stormbird.wallet.C.ErrorCode.EMPTY_COLLECTION;

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
    private TextView debugAddr;

    private SystemView systemView;
    private ProgressView progressView;
    private TokensAdapter adapter;
    private FragmentMessenger homeMessager;

    private boolean isVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        progressView = view.findViewById(R.id.progress_view);
        progressView.hide();

        progressView.setWhiteCircle();

        RecyclerView list = view.findViewById(R.id.list);
        debugAddr = view.findViewById(R.id.debug_addr);

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
        viewModel.checkAddr().observe(this, this::updateTitle);
        viewModel.tokensReady().observe(this, this::tokensReady);
        viewModel.fetchKnownContracts().observe(this, this::fetchKnownContracts);

        adapter = new TokensAdapter(getContext(), this::onTokenClick, viewModel.getAssetDefinitionService());
        adapter.setHasStableIds(true);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        viewModel.removeTokens().observe(this, adapter::onRemoveTokens);

        refreshLayout.setOnRefreshListener(this::refreshList);

        tokenReceiver = new TokensReceiver(getActivity(), this);

        initTabLayout(view);

        isVisible = true;

        viewModel.clearProcess();

        return view;
    }

    public void setTokenInterface(FragmentMessenger messenger)
    {
        homeMessager = messenger;
    }

    private void refreshList()
    {
        adapter.setClear();
        viewModel.reloadTokens();
    }

    private void updateTitle(String s)
    {
        debugAddr.setText(s);
    }

    private void checkTokens(Boolean dummy)
    {
        if (adapter.checkTokens())
        {
            viewModel.fetchTokens(); //require a full token refresh; number of tokens has changed
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isResumed()) { // fragment created
            viewModel.setVisibility(isVisible);
            if (isVisible) {
                viewModel.prepare();
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.setVisibility(false);
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
                    case 0:
                        adapter.setFilterType(TokensAdapter.FILTER_ALL);
                        viewModel.fetchTokens();
                        break;
                    case 1:
                        adapter.setFilterType(TokensAdapter.FILTER_CURRENCY);
                        viewModel.fetchTokens();
                        break;
                    case 2:
                        adapter.setFilterType(TokensAdapter.FILTER_ASSETS);
                        viewModel.fetchTokens();
                        break;
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
            case android.R.id.home: {
                //adapter.clear();
                //viewModel.showTransactions(getContext());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void onTokenClick(View view, Token token, BigInteger id) {
        Context context = view.getContext();
        token = viewModel.getTokenFromService(token);
        token.clickReact(viewModel, context);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.setVisibility(isVisible);
    }

    private void onTokens(Token[] tokens)
    {
        if (tokens != null)
        {
            adapter.setTokens(tokens);
        }
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
        viewModel.fetchTokens();
    }

    private void onDefaultNetwork(NetworkInfo networkInfo)
    {
        adapter.setDefaultNetwork(networkInfo);
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
        adapter.clear();
        viewModel.resetAndFetchTokens();
    }

    private void tokensReady(Boolean dummy)
    {
        if (homeMessager != null) homeMessager.TokensReady();
    }

    private void fetchKnownContracts(Integer networkId)
    {
        //fetch list of contracts for this network from the XML contract directory
        List<String> knownContracts = new ArrayList<>();
        int index = 0;
        switch (networkId)
        {
            case EthereumNetworkRepository.XDAI_ID:
                index = R.array.xDAI;
                break;
            case EthereumNetworkRepository.MAINNET_ID:
                index = R.array.MainNet;
                break;
            default:
                break;
        }

        if (index != 0)
        {
            String[] strArray = getResources().getStringArray(index);
            knownContracts.addAll(Arrays.asList(strArray));
            //initially assume all contracts added from XML have ERC20 interface
            //TODO: Handle querying delegate contracts
            for (String addr : strArray)
            {
                TokensService.setInterfaceSpec(addr, ContractType.ERC20);
            }
        }

        viewModel.checkKnownContracts(knownContracts);
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        viewModel.clearProcess();
        adapter.clear();
    }

    @Override
    public void addedToken()
    {
        viewModel.refreshAssetDefinedTokens(); //we loaded a new token, make balance query check the contract tokens
    }

    @Override
    public void changedLocale()
    {

    }
}
