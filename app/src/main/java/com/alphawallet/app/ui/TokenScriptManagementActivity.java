package com.alphawallet.app.ui;

import android.os.Bundle;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TokenLocator;
import com.alphawallet.app.ui.widget.adapter.TokenScriptManagementAdapter;
import com.alphawallet.app.viewmodel.TokenScriptManagementViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TokenScriptManagementActivity extends BaseActivity {


    private TokenScriptManagementViewModel viewModel;

    private RecyclerView tokenScriptList;

    private TokenScriptManagementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_script_management);

        toolbar();
        setTitle(getString(R.string.tokenscript_management));
        enableDisplayHomeAsUp();

        tokenScriptList = findViewById(R.id.token_script_list);
        tokenScriptList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(TokenScriptManagementViewModel.class);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initViewModel();
        refreshList(false);
    }

    public void refreshList(boolean refreshScripts)
    {
        final TokenScriptManagementActivity thisActivity = this;

        viewModel.onPrepare(refreshScripts);

        viewModel.getTokenLocatorsLiveData().observe(this, new Observer<List<TokenLocator>>() {
            @Override
            public void onChanged(List<TokenLocator> tokenList) {
                if (adapter == null) adapter = new TokenScriptManagementAdapter(thisActivity, tokenList, viewModel.getAssetService());
                else adapter.refreshList(tokenList);
                tokenScriptList.setAdapter(adapter);
            }
        });
    }
}
