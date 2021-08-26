package com.alphawallet.app.ui;


import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.ui.widget.adapter.Erc1155AssetsAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.Erc1155AssetsViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetsViewModelFactory;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class Erc1155AssetsFragment extends BaseFragment implements OnAssetClickListener {
    @Inject
    Erc1155AssetsViewModelFactory viewModelFactory;
    private Erc1155AssetsViewModel viewModel;

    private Token token;
    private Wallet wallet;
    private RecyclerView recyclerView;
    private Erc1155AssetsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        AndroidSupportInjection.inject(this);
        return inflater.inflate(R.layout.fragment_erc1155_assets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            token = getArguments().getParcelable(C.EXTRA_TOKEN);
            wallet = getArguments().getParcelable(C.Key.WALLET);

            recyclerView = view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.addItemDecoration(new ListDivider(getContext()));

            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(Erc1155AssetsViewModel.class);

            onAssets(token);
        }
    }

    private void onAssets(Token token) {
        adapter = new Erc1155AssetsAdapter(getContext(), token.getCollectionMap(), this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAssetClicked(Pair<BigInteger, NFTAsset> item)
    {
        if (item.second.isCollection())
        {
            viewModel.showAssetListDetails(getContext(), wallet, token, item.second);
        }
        else
        {
            viewModel.showAssetDetails(getContext(), wallet, token, item.first);
        }
    }
}
