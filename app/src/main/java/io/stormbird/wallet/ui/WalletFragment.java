package io.stormbird.wallet.ui;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;
import io.stormbird.wallet.ui.widget.adapter.TokensAdapter;
import io.stormbird.wallet.ui.widget.entity.WarningData;
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

public class WalletFragment extends Fragment implements OnTokenClickListener, View.OnClickListener, TokenInterface, Runnable, BackupTokenCallback
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
    private View selectedToken;
    private Handler handler;

    private boolean checkWalletBackup;

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
        viewModel.currentWalletBalance().observe(this, this::onBalanceChanged);
        viewModel.refreshTokens().observe(this, this::refreshTokens);
        viewModel.tokenUpdate().observe(this, this::onToken);
        viewModel.tokensReady().observe(this, this::tokensReady);
        viewModel.fetchKnownContracts().observe(this, this::fetchKnownContracts);

        adapter = new TokensAdapter(getActivity(),this, viewModel.getAssetDefinitionService());
        adapter.setHasStableIds(true);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(this::refreshList);

        tokenReceiver = new TokensReceiver(getActivity(), this);

        initTabLayout(view);

        isVisible = true;

        viewModel.clearProcess();

        checkWalletBackup = true;

        return view;
    }

    public void setTokenInterface(FragmentMessenger messenger)
    {
        homeMessager = messenger;
    }

    private void refreshList()
    {
        checkWalletBackup = true;
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
            if (isVisible) viewModel.prepare();
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
        adapter.updateToken(token, false);
    }

    private void initTabLayout(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.currency));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.collectibles));

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
                        adapter.setFilterType(TokensAdapter.FILTER_COLLECTIBLES);
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
                viewModel.showAddToken(getActivity());
            }
            break;
            case android.R.id.home: {
                //adapter.clear();
                //viewModel.showTransactions(getContext());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> ids, boolean selected) {
        if (selectedToken == null)
        {
            selectedToken = view;
            token = viewModel.getTokenFromService(token);
            token.clickReact(viewModel, getActivity());
            handler.postDelayed(this, 700);
        }
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenId)
    {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (handler == null) handler = new Handler();
        selectedToken = null;
        viewModel.setVisibility(isVisible);
        viewModel.prepare();
    }

    private void onTokens(Token[] tokens)
    {
        if (tokens != null)
        {
            adapter.setTokens(tokens);
            if (checkWalletBackup && viewModel.getWalletType() == Wallet.WalletType.HDKEY && viewModel.getWalletAddr() != null)
            {
                checkNeedsBackup();
            }
        }
    }

    private void checkNeedsBackup()
    {
        checkWalletBackup = false;
        viewModel.getBackupRequirement(viewModel.getWalletAddr())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::addWarningIfRequired).isDisposed();
    }

    private void addWarningIfRequired(GenericWalletInteract.BackupLevel backupLevel)
    {
        WarningData wData;
        switch (backupLevel)
        {
            case BACKUP_NOT_REQUIRED:
                break;
            case PERIODIC_BACKUP:
                wData = new WarningData(this);
                wData.title = getString(R.string.time_to_backup_wallet);
                wData.detail = getString(R.string.recommend_monthly_backup);
                wData.buttonText = getString(R.string.back_up_wallet_action, viewModel.getWalletAddr().substring(0, 5));
                wData.colour = ContextCompat.getColor(getContext(), R.color.slate_grey);
                wData.buttonColour = ContextCompat.getColor(getContext(), R.color.backup_grey);
                wData.address = viewModel.getWalletAddr();
                adapter.addWarning(wData);
                break;
            case WALLET_NEVER_BACKED_UP:
                wData = new WarningData(this);
                wData.title = getString(R.string.wallet_not_backed_up);
                wData.detail = getString(R.string.not_backed_up_detail);
                wData.buttonText = getString(R.string.back_up_wallet_action, viewModel.getWalletAddr().substring(0, 5));
                wData.colour = ContextCompat.getColor(getContext(), R.color.warning_red);
                wData.buttonColour = ContextCompat.getColor(getContext(), R.color.warning_dark_red);
                wData.address = viewModel.getWalletAddr();
                adapter.addWarning(wData);
                break;
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
        getActivity().unregisterReceiver(tokenReceiver);
        viewModel.clearProcess();
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

    private List<ContractResult> getAllKnownContractsOnNetwork(int chainId)
    {
        int index = 0;
        List<ContractResult> result = new ArrayList<>();
        switch (chainId)
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

        if (index > 0)
        {
            String[] strArray = getResources().getStringArray(R.array.MainNet);
            for (String addr : strArray)
            {
                result.add(new ContractResult(addr, EthereumNetworkRepository.MAINNET_ID));
            }
        }

        return result;
    }

    private void fetchKnownContracts(Boolean notUsed)
    {
        //fetch list of contracts for this network from the XML contract directory
        List<ContractResult> knownContracts = new ArrayList<>(getAllKnownContractsOnNetwork(EthereumNetworkRepository.MAINNET_ID));
        knownContracts.addAll(getAllKnownContractsOnNetwork(EthereumNetworkRepository.XDAI_ID));

        viewModel.checkKnownContracts(knownContracts);
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        viewModel.clearProcess();
        adapter.clear();
        //viewModel.prepare();
    }

    @Override
    public void addedToken()
    {

    }

    @Override
    public void changedLocale()
    {

    }

    public void walletOutOfFocus()
    {
        if (viewModel != null) viewModel.clearProcess();
    }

    public void walletInFocus()
    {
        if (viewModel != null) viewModel.reloadTokens();
    }

    @Override
    public void run()
    {
        if (selectedToken != null && selectedToken.findViewById(R.id.token_layout) != null)
        {
            selectedToken.findViewById(R.id.token_layout).setBackgroundResource(R.drawable.background_marketplace_event);
        }
        selectedToken = null;
    }

    @Override
    public void BackupClick(String address)
    {
        //start backup of wallet here
        Intent intent = new Intent(getContext(), BackupKeyActivity.class);
        intent.putExtra("ADDRESS", address);
        intent.putExtra("TYPE", "HDKEY");
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        getActivity().startActivityForResult(intent, C.REQUEST_BACKUP_SEED);
    }

    @Override
    public void remindMeLater(String walletAddress)
    {
        if (getActivity() != null) { Toast.makeText(getActivity(), "TODO: Placeholder for remind me later message", Toast.LENGTH_LONG).show(); }
        viewModel.setKeyWarningDismissTime(walletAddress).isDisposed();
        adapter.removeBackupWarning();
    }

    public void storeWalletBackupTime(String backedUpKey)
    {
        viewModel.setKeyBackupTime(backedUpKey)
                .isDisposed();

        adapter.removeBackupWarning();
    }
}
