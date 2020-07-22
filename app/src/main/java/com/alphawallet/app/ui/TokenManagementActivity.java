package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.ui.widget.adapter.TokenListAdapter;
import com.alphawallet.app.viewmodel.TokenManagementViewModel;
import com.alphawallet.app.viewmodel.TokenManagementViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TokenManagementActivity extends BaseActivity implements TokenListAdapter.ItemClickListener {
    @Inject
    TokenManagementViewModelFactory viewModelFactory;

    private TokenManagementViewModel viewModel;

    private RecyclerView tokenList;
    private Button saveButton;
    private TokenListAdapter adapter;
    private CheckBox hideZeroBalanceCheckBox;

    private Wallet wallet;

    private boolean isDataChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_management);
        toolbar();
        setTitle(getString(R.string.add_hide_tokens));
        tokenList = findViewById(R.id.token_list);
        tokenList.setLayoutManager(new LinearLayoutManager(this));

        saveButton = findViewById(R.id.btn_apply);
        saveButton.setOnClickListener(v -> {
            new HomeRouter().open(this, true);
        });
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TokenManagementViewModel.class);
        viewModel.tokens().observe(this, this::onTokens);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hideZeroBalanceTokens = pref.getBoolean("hide_zero_balance_tokens", false);
        hideZeroBalanceCheckBox = findViewById(R.id.checkbox_hide_zero_balance_tokens);
        hideZeroBalanceCheckBox.setChecked(hideZeroBalanceTokens);
        hideZeroBalanceCheckBox.setOnCheckedChangeListener((v, checked) -> {
            pref.edit().putBoolean("hide_zero_balance_tokens", checked).apply();
        });
    }

    private void onTokens(TokenCardMeta[] tokenArray) {
        if (tokenArray != null && tokenArray.length > 0)
        {
            adapter = new TokenListAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), tokenArray, this);
            tokenList.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(Token token, boolean enabled) {
        viewModel.setTokenEnabled(wallet, token, enabled);
        isDataChanged = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add)
        {
            viewModel.showAddToken(this);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        Below code is moved from onCreate to onResume to call each time when onResume is called.
        Reason behind is that when there is a custom token added with menu option,
        it should have fetch the tokens again.
         */
        if (getIntent() != null)
        {
            String walletAddr = getIntent().getStringExtra(C.EXTRA_ADDRESS);
            wallet = new Wallet(walletAddr);
            viewModel.fetchTokens(wallet);
        }
        else
        {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isDataChanged)
        {
            new HomeRouter().open(this, true);
        }
        super.onBackPressed();
    }
}
