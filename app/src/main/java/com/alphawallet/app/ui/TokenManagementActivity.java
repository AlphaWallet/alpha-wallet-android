package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
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
    private EditText search;

    private Wallet wallet;

    private Handler delayHandler = new Handler(Looper.getMainLooper());
    private Runnable workRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_management);
        toolbar();
        setTitle(getString(R.string.add_hide_tokens));

        initViews();
    }

    private void initViews() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(TokenManagementViewModel.class);
        viewModel.tokens().observe(this, this::onTokens);

        tokenList = findViewById(R.id.token_list);
        saveButton = findViewById(R.id.btn_apply);
        search = findViewById(R.id.edit_search);

        tokenList.setLayoutManager(new LinearLayoutManager(this));

        saveButton.setOnClickListener(v -> {
            new HomeRouter().open(this, true);
        });

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hideZeroBalanceTokens = pref.getBoolean("hide_zero_balance_tokens", false);
        hideZeroBalanceCheckBox = findViewById(R.id.checkbox_hide_zero_balance_tokens);
        hideZeroBalanceCheckBox.setChecked(hideZeroBalanceTokens);
        hideZeroBalanceCheckBox.setOnCheckedChangeListener((v, checked) -> {
            pref.edit().putBoolean("hide_zero_balance_tokens", checked).apply();
        });

        tokenList.requestFocus();
        search.addTextChangedListener(textWatcher);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void afterTextChanged(final Editable searchString) {
            if (workRunnable != null)
            {
                delayHandler.removeCallbacks(workRunnable);
            }
            workRunnable = () -> adapter.getFilter().filter(searchString);
            delayHandler.postDelayed(workRunnable, 500 /*delay*/);
        }
    };

    private void onTokens(Token[] tokenArray) {
        if (tokenArray != null && tokenArray.length > 0) {
            adapter = new TokenListAdapter(this, viewModel.getAssetDefinitionService(), tokenArray, this);
            tokenList.setAdapter(adapter);
        }
    }

    @Override
    public void onItemClick(Token token, boolean enabled) {
        viewModel.setTokenEnabled(wallet, token, enabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
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
        if (getIntent() != null) {
            String walletAddr = getIntent().getStringExtra(C.EXTRA_ADDRESS);
            wallet = new Wallet(walletAddr);
            viewModel.fetchTokens(walletAddr);
        } else {
            finish();
        }
    }
}
