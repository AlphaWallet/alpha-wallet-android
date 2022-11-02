package com.alphawallet.app.ui;

import static com.alphawallet.app.C.ETH_SYMBOL;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.repository.TokensRealmSource.databaseKey;
import static com.alphawallet.app.ui.MyAddressActivity.KEY_MODE;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;

import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AddressMode;
import com.alphawallet.app.entity.BuyCryptoInterface;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.router.SwapRouter;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.TabPagerAdapter;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.Erc20DetailViewModel;
import com.alphawallet.app.widget.ActivityHistoryList;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;

@AndroidEntryPoint
public class Erc20DetailActivity extends BaseActivity implements StandardFunctionInterface, BuyCryptoInterface
{
    Erc20DetailViewModel viewModel;

    public static final int HISTORY_LENGTH = 5;

    private String symbol;
    private Wallet wallet;
    private Token token;
    private TokenCardMeta tokenMeta;
    private RecyclerView tokenView;

    private TokensAdapter tokenViewAdapter;
    private ActivityHistoryList activityHistoryList = null;
    private Realm realm = null;
    private RealmResults<RealmToken> realmTokenUpdates;

    private ViewPager2 viewPager;

    private static class DetailPage {
        private final int tabNameResourceId;
        private final Fragment fragment;

        DetailPage(int tabNameResourceId, Fragment fragment)
        {
            this.tabNameResourceId = tabNameResourceId;
            this.fragment = fragment;
        }

        public Pair<String, Fragment> init(Context context, Bundle bundle)
        {
            this.fragment.setArguments(bundle);
            return new Pair<>(context.getString(tabNameResourceId), fragment);
        }
    }

    private final DetailPage[] detailPages = new DetailPage[] {
            new DetailPage(R.string.tab_info, new TokenInfoFragment()),
            new DetailPage(R.string.tab_activity, new TokenActivityFragment()),
            new DetailPage(R.string.tab_alert, new TokenAlertsFragment())
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc20_token_detail);
        symbol = null;
        toolbar();
        setupViewModel();

        if (savedInstanceState != null && savedInstanceState.containsKey(C.EXTRA_SYMBOL))
        {
            symbol = savedInstanceState.getString(C.EXTRA_ACTION_NAME);
            wallet = savedInstanceState.getParcelable(WALLET);
            long chainId = savedInstanceState.getLong(C.EXTRA_CHAIN_ID, MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, savedInstanceState.getString(C.EXTRA_ADDRESS));
        }
        else
        {
            getIntentData();
        }

        viewModel.checkForNewScript(token);

        setTitle(token.tokenInfo.name + " (" + token.tokenInfo.symbol + ")");
        initViews();
        setUpTokenView();
        setUpRecentTransactionsView();
    }

    private void initViews()
    {
        Bundle bundle = new Bundle();
        //Samoa TODO: Use TokensService getToken in the 3 fragments
        bundle.putString(C.EXTRA_ADDRESS, token.getAddress());
        bundle.putLong(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        bundle.putParcelable(WALLET, wallet);

        List<Pair<String, Fragment>> pages = getPages(bundle);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(this, pages));
        viewPager.setOffscreenPageLimit(detailPages.length);  // to retain fragments in memory
        viewPager.setUserInputEnabled(false);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        // connect viewPager and TabLayout
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(pages.get(position).first)
        ).attach();

        // TODO: addOnTabSelectedListener if you need to refresh values when switching tabs

        TabUtils.decorateTabLayout(this, tabLayout);
    }

    private List<Pair<String, Fragment>> getPages(Bundle bundle)
    {
        List<Pair<String, Fragment>> pages = new ArrayList<>();
        for (int i = 0; i< detailPages.length; i++) {
            pages.add(i, detailPages[i].init(this, bundle));
        }
        return pages;
    }

    private void setupButtons()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupBuyFunction(this, viewModel.getOnRampRepository());
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void setupViewModel()
    {
        if (viewModel == null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(Erc20DetailViewModel.class);
            viewModel.newScriptFound().observe(this, this::onNewScript);
            viewModel.sig().observe(this, this::onSignature);
            viewModel.scriptUpdateInProgress().observe(this, this::startScriptDownload);
//            findViewById(R.id.certificate_spinner).setVisibility(View.VISIBLE); //Samoa TODO: restore certificate toolbar
        }
    }

    private void startScriptDownload(Boolean status)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.setVisibility(View.VISIBLE);
        if (status)
        {
            certificateToolbar.startDownload();
        }
        else
        {
            certificateToolbar.stopDownload();
            certificateToolbar.hideCertificateResource();
        }
    }

    private void onNewScript(Boolean hasNewScript)
    {
        final TokenGroup group = viewModel.getTokensService().getTokenGroup(token);
        //found a new tokenscript for this token, create a new meta with balance set to trigger view update; view will update the token name
        tokenViewAdapter.updateToken(new TokenCardMeta(token.tokenInfo.chainId, token.getAddress(), "force_update",
                token.updateBlancaTime, token.lastTxCheck, token.getInterfaceSpec(), group), true);
        viewModel.checkTokenScriptValidity(token); //check script signature
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.stopDownload();
        setupButtons();
    }

    private void onSignature(XMLDsigDescriptor descriptor)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.onSigData(descriptor, this);
    }

    private void setUpRecentTransactionsView()
    {
        if (activityHistoryList != null) return;
        activityHistoryList = findViewById(R.id.history_list);
        ActivityAdapter adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.getTransactionsInteract(),
                viewModel.getAssetDefinitionService());

        adapter.setDefaultWallet(wallet);

        //TODO: Fix Realm leak
        activityHistoryList.setupAdapter(adapter);
        activityHistoryList.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                token, viewModel.getTokensService(), HISTORY_LENGTH);
    }

    private void setUpTokenView()
    {
        if (tokenViewAdapter != null) return;
        tokenView = findViewById(R.id.token_view);
        tokenView.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically()
            {
                return false;
            }
        });
        tokenViewAdapter = new TokensAdapter(null, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), null);
        tokenViewAdapter.updateToken(tokenMeta, true);
        tokenView.setAdapter(tokenViewAdapter);
        setTokenListener();
        setupButtons();
        viewModel.checkTokenScriptValidity(token);
    }

    private void getIntentData()
    {
        if (symbol != null) return;

        symbol = getIntent().getStringExtra(C.EXTRA_SYMBOL);
        symbol = symbol == null ? ETH_SYMBOL : symbol;
        wallet = getIntent().getParcelableExtra(WALLET);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, MAINNET_ID);
        token = viewModel.getTokensService().getTokenOrBase(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        token.group = viewModel.getTokensService().getTokenGroup(token);
        tokenMeta = new TokenCardMeta(token, token.getName());
        viewModel.checkForNewScript(token);
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(C.EXTRA_SYMBOL, symbol);
        outState.putParcelable(WALLET, wallet);
        outState.putLong(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        outState.putString(C.EXTRA_ADDRESS, token.getAddress());
    }

    private void setTokenListener()
    {
        if (realm == null) realm = viewModel.getRealmInstance(wallet);
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        String dbKey = databaseKey(token.tokenInfo.chainId, token.tokenInfo.address.toLowerCase());
        final TokenGroup group = viewModel.getTokensService().getTokenGroup(token);
        realmTokenUpdates = realm.where(RealmToken.class).equalTo("address", dbKey)
                .greaterThan("addedTime", System.currentTimeMillis() - 5 * DateUtils.MINUTE_IN_MILLIS).findAllAsync();
        realmTokenUpdates.addChangeListener(realmTokens -> {
            if (realmTokens.size() == 0) return;
            for (RealmToken t : realmTokens)
            {
                TokenCardMeta meta = new TokenCardMeta(t.getChainId(), t.getTokenAddress(), t.getBalance(),
                        t.getUpdateTime(), t.getLastTxTime(), t.getContractType(), group);
                meta.isEnabled = t.isEnabled();

                if (tokenMeta == null)
                {
                    tokenMeta = meta;
                }
                else if (!tokenMeta.balance.equals(meta.balance))
                {
                    playNotification();
                }

                tokenViewAdapter.updateToken(meta, true);
            }
        });
    }

    private void playNotification()
    {
        try
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(this, notification);
            r.play();
        }
        catch (Exception e)
        {
            //empty
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
//        getMenuInflater().inflate(R.menu.menu_show_contract, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        else if (item.getItemId() == R.id.action_show_contract)
        {
            viewModel.showContractInfo(this, wallet, token);
        }
        return false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (activityHistoryList != null) activityHistoryList.onDestroy();
        if (realmTokenUpdates != null) realmTokenUpdates.removeAllChangeListeners();
        if (tokenViewAdapter != null && tokenView != null) tokenViewAdapter.onDestroy(tokenView);
        if (realm != null) realm.close();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.getTokensService().clearFocusToken();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (viewModel == null)
        {
            restartView();
        }
        else
        {
            activityHistoryList.resetAdapter();
            activityHistoryList.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                    token, viewModel.getTokensService(), HISTORY_LENGTH);
            viewModel.getTokensService().setFocusToken(token);
            viewModel.restartServices();
        }
    }

    private void restartView()
    {
        setupViewModel();
        getIntentData();
        setUpTokenView();
        setUpRecentTransactionsView();
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        Intent intent = new Intent(this, FunctionActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(WALLET, wallet);
        intent.putExtra(C.EXTRA_STATE, function);
        intent.putExtra(C.EXTRA_TOKEN_ID, BigInteger.ZERO.toString(16));
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }

    @Override
    public void showSend()
    {
        viewModel.showSendToken(this, wallet, token);
    }

    @Override
    public void showReceive()
    {
        viewModel.showMyAddress(this, wallet, token);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        String transactionHash = null;

        switch (requestCode)
        {
            case C.COMPLETED_TRANSACTION: //completed a transaction send and got with either a hash or a null
                if (data != null) transactionHash = data.getStringExtra(C.EXTRA_TXHASH);
                if (transactionHash != null)
                {
                    //switch to activity view
                    viewPager.setCurrentItem(1);        // 0-INFO, 1-ACTIVITY, 2-ALERTS
                }
                break;
        }
    }

    @Override
    public void handleClick(String action, int actionId)
    {
        if (actionId == R.string.convert_to_xdai)
        {
            openDapp(C.XDAI_BRIDGE_DAPP);
        }
        else if (actionId == R.string.swap_with_quickswap)
        {
            String queryPath = "?use=v2&inputCurrency=" + (token.isEthereum() ? ETH_SYMBOL : token.getAddress());
            openDapp(C.QUICKSWAP_EXCHANGE_DAPP + queryPath);
        }
        else if (actionId == R.string.swap)
        {
            //openDapp(formatOneInchCall(token));
            new SwapRouter().open(this, token, wallet);
        }
    }

    private String formatOneInchCall(Token token)
    {
        String token1;
        String token2;
        if (token.isERC20())
        {
            token1 = token.getAddress();
            token2 = EthereumNetworkBase.getNetworkInfo(token.tokenInfo.chainId).symbol;
        }
        else
        {
            token1 = token.tokenInfo.symbol;
            token2 = "";
        }

        return C.ONEINCH_EXCHANGE_DAPP.replace("[CHAIN]", String.valueOf(token.tokenInfo.chainId))
                .replace("[TOKEN1]", token1).replace("[TOKEN2]", token2);
    }

    private void openDapp(String dappURL)
    {
        //switch to dappbrowser and open at dappURL
        Intent intent = new Intent();
        intent.putExtra(C.DAPP_URL_LOAD, dappURL);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void handleBuyFunction(Token token)
    {
        Intent intent = viewModel.getBuyIntent(wallet.address, token);
        setResult(RESULT_OK, intent);
        viewModel.track(Analytics.Action.BUY_WITH_RAMP);
        finish();
    }

    @Override
    public void handleGeneratePaymentRequest(Token token) {
        Intent intent = new Intent(this, MyAddressActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(KEY_MODE, AddressMode.MODE_POS.ordinal());
        this.startActivity(intent);
    }
}
