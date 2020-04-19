package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokenscript.TokenScriptFile;
import com.alphawallet.app.ui.widget.adapter.TokenScriptManagementAdapter;
import com.alphawallet.app.viewmodel.HomeViewModel;
import com.alphawallet.app.viewmodel.TokenScriptManagementViewModel;
import com.alphawallet.app.viewmodel.TokenScriptManagementViewModelFactory;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class TokenScriptManagementActivity extends BaseActivity {

    @Inject
    TokenScriptManagementViewModelFactory tokenScriptManagementViewModelFactory;

    private TokenScriptManagementViewModel viewModel;

    private RecyclerView tokenScriptList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_script_management);

        toolbar();
        setTitle(getString(R.string.tokenscript_management));
        enableDisplayHomeAsUp();

        tokenScriptList = findViewById(R.id.token_script_list);
        tokenScriptList.setLayoutManager(new LinearLayoutManager(this));

        viewModel = ViewModelProviders.of(this, tokenScriptManagementViewModelFactory)
                .get(TokenScriptManagementViewModel.class);

        Map<String, TokenScriptFile> allTokenFiles = viewModel.getFileList();
        Map<String, TokenScriptFile> filteredTokenFiles = new HashMap<>();

        //filter entries for AlphaWallet directory only
        for(String key : allTokenFiles.keySet()){
            TokenScriptFile file = allTokenFiles.get(key);
            if(file.getName() != null
                    && file.getPath().contains("/")
                    && file.getPath().contains(HomeViewModel.ALPHAWALLET_DIR)){
                filteredTokenFiles.put(key, file);
            }
        }

        tokenScriptList.setAdapter(new TokenScriptManagementAdapter(this, filteredTokenFiles));
    }
}
