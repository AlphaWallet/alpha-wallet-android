package com.alphawallet.app.ui;

import static com.alphawallet.app.C.ADDED_TOKEN;
import static com.alphawallet.app.repository.TokensRealmSource.ADDRESS_FORMAT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmToken;
import com.alphawallet.app.ui.widget.adapter.TokenListAdapter;
import com.alphawallet.app.viewmodel.TokenManagementViewModel;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmResults;

@AndroidEntryPoint
public class TokenManagementActivity extends BaseActivity implements TokenListAdapter.ItemClickListener {

    private TokenManagementViewModel viewModel;

    private RecyclerView tokenList;
    private Button saveButton;
    private TokenListAdapter adapter;
    private EditText search;

    private Wallet wallet;
    private Realm realm;
    private RealmResults<RealmToken> realmUpdates;
    private String realmId;
    private ArrayList<ContractLocator> tokenUpdates;

    private final Handler delayHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_management);
        toolbar();
        setTitle(getString(R.string.add_hide_tokens));
        initViews();
        tokenUpdates = null;
    }

    private void initViews() {
        viewModel = new ViewModelProvider(this)
                .get(TokenManagementViewModel.class);
        viewModel.tokens().observe(this, this::onTokens);

        wallet = new Wallet(viewModel.getTokensService().getCurrentAddress());
        tokenList = findViewById(R.id.token_list);
        saveButton = findViewById(R.id.btn_apply);
        search = findViewById(R.id.edit_search);

        tokenList.setLayoutManager(new LinearLayoutManager(this));

        saveButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        });

        tokenList.requestFocus();
        search.addTextChangedListener(textWatcher);
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void afterTextChanged(final Editable s) {
            delayHandler.removeCallbacksAndMessages(null);
            delayHandler.postDelayed(() -> {
                if (adapter != null) adapter.filter(s.toString());
            }, 750);
        }
    };

    private void onTokens(TokenCardMeta[] tokenArray) {
        if (tokenArray != null && tokenArray.length > 0)
        {
            adapter = new TokenListAdapter(this, viewModel.getAssetDefinitionService(), viewModel.getTokensService(), tokenArray, this);
            tokenList.setAdapter(adapter);

            startRealmListener(wallet);
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

    final ActivityResultLauncher<Intent> addTokenLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                tokenUpdates = result.getData().getParcelableArrayListExtra(ADDED_TOKEN);
            });

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_add)
        {
            addTokenLauncher.launch(new Intent(this, AddTokenActivity.class));
        }
        else if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
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
            if (walletAddr == null) walletAddr = viewModel.getTokensService().getCurrentAddress();
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
        super.onBackPressed();
        if (search.getText().length() > 0)
        {
            search.setText("");
            return;
        }
        //setup init
        Intent iResult = new Intent();
        iResult.putParcelableArrayListExtra(ADDED_TOKEN, tokenUpdates);
        setResult(RESULT_OK, iResult);
        finish();
    }

    private void startRealmListener(Wallet wallet)
    {
        if (realmId == null || !realmId.equalsIgnoreCase(wallet.address))
        {
            realmId = wallet.address;
            realm = viewModel.getRealmInstance(wallet);
            setRealmListener();
        }
    }

    private void setRealmListener()
    {
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        realmUpdates = realm.where(RealmToken.class)
                .like("address", ADDRESS_FORMAT)
                .findAllAsync();
        realmUpdates.addChangeListener(realmTokens -> {
            String filterText = search.getText().toString();
            if (realmTokens.size() == 0 || filterText.length() > 0) return;

            //Insert when discover
            for (RealmToken token : realmTokens)
            {
                if (adapter.isTokenPresent(token.getTokenAddress())) continue;

                String balance = TokensRealmSource.convertStringBalance(token.getBalance(), token.getContractType());
                Token t = viewModel.getTokensService().getToken(token.getChainId(), token.getTokenAddress()); //may not be needed to group

                TokenCardMeta meta = new TokenCardMeta(token.getChainId(), token.getTokenAddress(), balance,
                        token.getUpdateTime(), viewModel.getAssetDefinitionService(), token.getName(), token.getSymbol(), token.getContractType(),
                        viewModel.getTokensService().getTokenGroup(t));
                meta.lastTxUpdate = token.getLastTxTime();
                adapter.addToken(meta);
            }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (realm != null) realm.close();
        if (adapter != null) adapter.onDestroy();
    }

}
