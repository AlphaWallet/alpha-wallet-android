package com.alphawallet.app.ui;


import android.os.Bundle;
import android.text.TextUtils;
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
import com.alphawallet.app.entity.opensea.AssetContract;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.viewmodel.NFTInfoViewModel;
import com.alphawallet.app.viewmodel.NFTInfoViewModelFactory;
import com.alphawallet.app.widget.TokenIcon;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class NFTInfoFragment extends BaseFragment {
    @Inject
    NFTInfoViewModelFactory viewModelFactory;
    private NFTInfoViewModel viewModel;

    private Token token;
    private LinearLayout tokenInfoLayout;

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
            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(NFTInfoViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));

            tokenInfoLayout = view.findViewById(R.id.layout_token_info);
            tokenDescription = view.findViewById(R.id.token_description);

            if (token.isERC721())
            {
                tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Details"));
                addInfoView("Issuer", token.tokenInfo.name);
                addInfoView("Contract Address", token.tokenInfo.address);
                addInfoView("Blockchain", token.getNetworkName());
            }
            else
            {
                AssetContract assetContract = token.getAssetContract();
                if (assetContract != null)
                {
                    tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Details"));
                    addInfoView("Issuer", assetContract.getName());
                    addInfoView("Created", assetContract.getCreationDate());
                    //addInfoView("Total Reserve", assetContract.getName());
                    //addInfoView("Assets", assetContract.getName());
                    //addInfoView("Melts", assetContract.getName());
                    //addInfoView("Hodlers", assetContract.getName());
                    //addInfoView("Transfers", assetContract.getName());

                    if (!TextUtils.isEmpty(assetContract.getDescription()))
                    {
                        tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Description"));
                        tokenDescription.setText(assetContract.getDescription());
                    }
                }
            }

            TokenIcon icon = view.findViewById(R.id.token_icon);
            icon.bindData(token, viewModel.getAssetDefinitionService());
        }
    }

    private void addInfoView(String elementName, String name)
    {
        if (!TextUtils.isEmpty(name))
        {
            TokenInfoView v = new TokenInfoView(getContext(), elementName);
            v.setValue(name);
            tokenInfoLayout.addView(v);
        }
    }
}
