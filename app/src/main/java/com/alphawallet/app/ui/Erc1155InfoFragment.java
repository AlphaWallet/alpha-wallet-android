package com.alphawallet.app.ui;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.viewmodel.Erc1155InfoViewModel;
import com.alphawallet.app.viewmodel.Erc1155InfoViewModelFactory;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class Erc1155InfoFragment extends BaseFragment {
    @Inject
    Erc1155InfoViewModelFactory viewModelFactory;
    private Erc1155InfoViewModel viewModel;

    private Token token;
    private LinearLayout tokenInfoLayout;

    private TokenInfoView issuer;
    private TokenInfoView totalReserve;
    private TokenInfoView created;
    private TokenInfoView assets;
    private TokenInfoView melts;
    private TokenInfoView hodlers;
    private TokenInfoView transfers;
    private TextView tokenDescription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        return inflater.inflate(R.layout.fragment_erc1155_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            token = getArguments().getParcelable(C.EXTRA_TOKEN_ID);

            tokenInfoLayout = view.findViewById(R.id.layout_token_info);
            tokenDescription = view.findViewById(R.id.token_description);

            issuer = new TokenInfoView(getContext(), "Issuer");
            totalReserve = new TokenInfoView(getContext(), "Total Reserve");
            created = new TokenInfoView(getContext(), "Created");
            assets = new TokenInfoView(getContext(), "Assets");
            melts = new TokenInfoView(getContext(), "Melts");
            hodlers = new TokenInfoView(getContext(), "Hodlers");
            transfers = new TokenInfoView(getContext(), "Transfers");

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Details"));
            tokenInfoLayout.addView(issuer);
            tokenInfoLayout.addView(totalReserve);
            tokenInfoLayout.addView(created);
            tokenInfoLayout.addView(assets);
            tokenInfoLayout.addView(melts);
            tokenInfoLayout.addView(hodlers);
            tokenInfoLayout.addView(transfers);

            tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Description"));
            tokenDescription.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam ut ante ut velit molestie rutrum. Praesent ut sapien vitae ex imperdiet efficitur id vel urna. Etiam ac gravida metus.");

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(Erc1155InfoViewModel.class);
        }
    }
}
