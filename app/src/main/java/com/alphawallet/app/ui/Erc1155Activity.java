package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.TabPagerAdapter;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.ScrollControlViewPager;
import com.alphawallet.app.util.TabUtils;
import com.alphawallet.app.viewmodel.Erc1155ViewModel;
import com.alphawallet.app.viewmodel.Erc1155ViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.google.android.material.tabs.TabLayout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.alphawallet.app.C.Key.WALLET;

public class Erc1155Activity extends BaseActivity implements StandardFunctionInterface {
    @Inject
    Erc1155ViewModelFactory viewModelFactory;
    Erc1155ViewModel viewModel;

    private Menu menu;
    private Wallet wallet;
    private Token token;
    private FunctionButtonBar functionBar;
    private int menuItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155);
        toolbar();
        getIntentData();
        initViewModel();
        setTitle(token.tokenInfo.name);
        setupViewPager();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(Erc1155ViewModel.class);
    }

    private void getIntentData()
    {
        wallet = getIntent().getParcelableExtra(WALLET);
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN);
    }

    private void setupViewPager()
    {
        Erc1155InfoFragment infoFragment = new Erc1155InfoFragment();
        Erc1155AssetsFragment assetsFragment = new Erc1155AssetsFragment();
        TokenActivityFragment tokenActivityFragment = new TokenActivityFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(C.EXTRA_TOKEN, token);
        bundle.putParcelable(WALLET, wallet);
        infoFragment.setArguments(bundle);
        assetsFragment.setArguments(bundle);
        tokenActivityFragment.setArguments(bundle);

        List<Pair<String, Fragment>> pages = new ArrayList<>();
        pages.add(0, new Pair<>("Info", infoFragment));
        pages.add(1, new Pair<>("Assets", assetsFragment));
        pages.add(2, new Pair<>("Activity", tokenActivityFragment));

        ScrollControlViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new TabPagerAdapter(getSupportFragmentManager(), pages));
        setupTabs(viewPager);
    }

    private void setupTabs(ScrollControlViewPager viewPager)
    {
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        tabLayout.setupWithViewPager(viewPager);

        TabUtils.decorateTabLayout(this, tabLayout);

        viewPager.setCurrentItem(1, true);
        menuItem = R.menu.menu_select;

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                switch (tab.getPosition())
                {
                    case 0:
                        showFunctionBar(true);
                        menuItem = 0;
                        invalidateOptionsMenu();
                        break;
                    case 1:
                        showFunctionBar(false);
                        menuItem = R.menu.menu_select;
                        invalidateOptionsMenu();
                        break;
                    default:
                        showFunctionBar(false);
                        menuItem = 0;
                        invalidateOptionsMenu();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab)
            {

            }
        });
    }

    private void showFunctionBar(boolean show)
    {
        if (functionBar == null && !show) return;
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            if (functionBar == null) setupFunctionBar();
            functionBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.menu = menu;
        if (menuItem != 0)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(menuItem, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_select) {
            viewModel.openSelectionMode(this, token, wallet);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupFunctionBar()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
            functionBar.setVisibility(View.GONE);
        }
    }
}
