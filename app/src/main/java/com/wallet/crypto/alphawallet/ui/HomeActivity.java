package com.wallet.crypto.alphawallet.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.ErrorEnvelope;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.ui.widget.adapter.TransactionsAdapter;
import com.wallet.crypto.alphawallet.util.RootUtil;
import com.wallet.crypto.alphawallet.viewmodel.BaseNavigationActivity;
import com.wallet.crypto.alphawallet.viewmodel.HomeViewModel;
import com.wallet.crypto.alphawallet.viewmodel.HomeViewModelFactory;
import com.wallet.crypto.alphawallet.widget.AWalletAlertDialog;
import com.wallet.crypto.alphawallet.widget.DepositView;
import com.wallet.crypto.alphawallet.widget.SystemView;

import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class HomeActivity extends BaseNavigationActivity implements View.OnClickListener {
    private static final int TRANSACTIONS = 0;
    private static final int MARKETPLACE = 1;
    private static final int WALLET = 2;
    private static final int SETTINGS = 3;

    private static final String HARD_PRIVATE_KEY = "bddb287b00c8047587f4fbf3de731a30f9404aa3735ae5d1766550534cf939da"; //0xc9034FF4266b1690d2B579584e5c3259009eD13c
    private static final String HARD_PRIVATE_KEY2 = "dcd6318be4fa8ab458b608804f06b8f25ccdab4f9d03c26fbbbe2b58e42f4df5"; //0x97e2bde4654Ca8Ea2cC30335Ea85eC1F9b10604A
    private static final String HARD_PRIVATE_KEY3 = "fa41ce4f689c883584cfcf29cd2526c7f4de4dede260753b0234e475b216a733"; //0x93922cDaBAa26d50E7C6Cb19EE3bCd03462Ed334

    @Inject
    HomeViewModelFactory homeViewModelFactory;
    private HomeViewModel viewModel;

    private SystemView systemView;
    private TransactionsAdapter adapter;
    private Dialog dialog;
    private ViewPager viewPager;
    private PagerAdapter pagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        toolbar();

        viewPager = findViewById(R.id.view_pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                showPage(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        initBottomNavigation();
        dissableDisplayHomeAsUp();

        adapter = new TransactionsAdapter(this::onTransactionClick);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView = findViewById(R.id.system_view);

        RecyclerView list = findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int position = list.getChildAdapterPosition(view);
                if (position == 0) {
                    outRect.top = (int) getResources().getDimension(R.dimen.big_margin);
                }
            }
        });
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, homeViewModelFactory)
                .get(HomeViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWalletBalance().observe(this, this::onBalanceChanged);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);

        refreshLayout.setOnRefreshListener(() -> viewModel.fetchTransactions(true));

        setBottomMenu(R.menu.menu_main_network);
        showPage(WALLET);

        viewModel.addHardKey(HARD_PRIVATE_KEY);
    }

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(view.getContext(), transaction);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onResume() {
        super.onResume();
        adapter.clear();
        viewModel.prepare();
        checkRoot();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        getMenuInflater().inflate(R.menu.menu_settings, menu);
//
//        NetworkInfo networkInfo = viewModel.defaultNetwork().getValue();
//        if (networkInfo != null && networkInfo.name.equals(ETHEREUM_NETWORK_NAME)) {
//            getMenuInflater().inflate(R.menu.menu_deposit, menu);
//        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                viewModel.showSettings(this);
            }
            break;
            case R.id.action_deposit: {
                openExchangeDialog();
            }
            break;
            case R.id.action_add: {
                viewModel.showAddToken(this);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchTransactions(true);
            }
            break;
            case R.id.action_buy: {
                openExchangeDialog();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_transactions: {
                if (getSelectedNavigationItem() != 0) {
                    showPage(TRANSACTIONS);
                }
                return true;
            }
            case R.id.action_marketplace: {
                if (getSelectedNavigationItem() != 1) {
                    showPage(MARKETPLACE);
                }
                return true;
            }
            case R.id.action_wallet: {
                if (getSelectedNavigationItem() != 2) {
                    showPage(WALLET);
                }
                return true;
            }
            case R.id.action_settings: {
                if (getSelectedNavigationItem() != 3) {
                    showPage(SETTINGS);
                }
                return true;
            }
        }
        return false;
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

    private void onTransactions(Transaction[] transaction) {
        adapter.addTransactions(transaction);
        invalidateOptionsMenu();
    }

    private void onDefaultWallet(Wallet wallet) {
        adapter.setDefaultWallet(wallet);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        adapter.setDefaultNetwork(networkInfo);
//        setBottomMenu(R.menu.menu_main_network);
//        selectNavigationItem(1);
//        setTitle("Wallet");
    }

    private void onError(ErrorEnvelope errorEnvelope) {
//        if (errorEnvelope.code == EMPTY_COLLECTION || adapter.getItemCount() == 0) {
//            EmptyTransactionsView emptyView = new EmptyTransactionsView(this, this);
//            emptyView.setNetworkInfo(viewModel.defaultNetwork().getValue());
//            systemView.showEmpty(emptyView);
//        }
        /* else {
            systemView.showError(getString(R.string.error_fail_load_transaction), this);
        }*/
    }

    private void checkRoot() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (RootUtil.isDeviceRooted() && pref.getBoolean("should_show_root_warning", true)) {
            pref.edit().putBoolean("should_show_root_warning", false).apply();
            AWalletAlertDialog dialog = new AWalletAlertDialog(this);
            dialog.setTitle(R.string.root_title);
            dialog.setMessage(R.string.root_body);
            dialog.setButtonText(R.string.ok);
            dialog.setButtonListener(v -> dialog.dismiss());
            dialog.show();
        }
    }

    private void openExchangeDialog() {
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (wallet == null) {
            Toast.makeText(this, getString(R.string.error_wallet_not_selected), Toast.LENGTH_SHORT)
                    .show();
        } else {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            DepositView view = new DepositView(this, wallet);
            view.setOnDepositClickListener(this::onDepositClick);
            dialog.setContentView(view);
            dialog.show();
            this.dialog = dialog;
        }
    }

    private void onDepositClick(View view, Uri uri) {
        viewModel.openDeposit(view.getContext(), uri);
    }

    private void showPage(int page) {
        switch (page) {
            case MARKETPLACE: {
                viewPager.setCurrentItem(MARKETPLACE);
                setTitle(getString(R.string.toolbar_header_marketplace));
                selectNavigationItem(MARKETPLACE);
                break;
            }
            case WALLET: {
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                break;
            }
            case SETTINGS: {
                viewPager.setCurrentItem(SETTINGS);
                setTitle(getString(R.string.toolbar_header_settings));
                selectNavigationItem(SETTINGS);
                break;
            }
            case TRANSACTIONS: {
                viewPager.setCurrentItem(TRANSACTIONS);
                setTitle(getString(R.string.toolbar_header_transactions));
                selectNavigationItem(TRANSACTIONS);
                break;
            }
            default:
                viewPager.setCurrentItem(WALLET);
                setTitle(getString(R.string.toolbar_header_wallet));
                selectNavigationItem(WALLET);
                break;
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case MARKETPLACE:
                    return new MarketplaceFragment();
                case WALLET:
                    return new WalletFragment();
                case SETTINGS:
                    return new NewSettingsFragment();
                case TRANSACTIONS:
                    return new TransactionsFragment();
                default:
                    return new WalletFragment();
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
