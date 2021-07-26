package com.alphawallet.app.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.viewmodel.TokenActivityViewModel;
import com.alphawallet.app.viewmodel.TokenActivityViewModelFactory;
import com.alphawallet.app.widget.ActivityHistoryList;

import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TokenActivityFragment extends BaseFragment {
    @Inject
    TokenActivityViewModelFactory viewModelFactory;

    private TokenActivityViewModel viewModel;

    private ActivityHistoryList history;

    private Wallet wallet;

    private Token token;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        return inflater.inflate(R.layout.fragment_token_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null)
        {
            token = getArguments().getParcelable(C.EXTRA_TOKEN_ID);
            wallet = getArguments().getParcelable(C.Key.WALLET);

            history = view.findViewById(R.id.history_list);

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(TokenActivityViewModel.class);

            setUpRecentTransactionsView();
        }
    }

    //Realm realm, Wallet wallet, Token token, TokensService svs, BigInteger tokenId, final int historyCount
    private void setUpRecentTransactionsView()
    {
        ActivityAdapter adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.getTransactionsInteract(),
                viewModel.getAssetDefinitionService());
        adapter.setDefaultWallet(wallet);
        history.setupAdapter(adapter);
        history.startActivityListeners(viewModel.getRealmInstance(wallet), wallet,
                token, viewModel.getTokensService(), BigInteger.ZERO, 15);
    }
}
