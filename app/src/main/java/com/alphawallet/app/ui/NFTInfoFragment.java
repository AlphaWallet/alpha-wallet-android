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
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.viewmodel.NFTInfoViewModel;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigInteger;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NFTInfoFragment extends BaseFragment {
    NFTInfoViewModel viewModel;

    NFTImageView assetImage;
    private Token token;
    private LinearLayout tokenInfoLayout;
    private TextView tokenDescription;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_nft_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null)
        {
            viewModel = new ViewModelProvider(this)
                    .get(NFTInfoViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));
            assetImage = view.findViewById(R.id.asset_image);
            tokenInfoLayout = view.findViewById(R.id.layout_token_info);
            tokenDescription = view.findViewById(R.id.token_description);

            if (token.isERC721())
            {
                List<BigInteger> assetList = token.getUniqueTokenIds();
                if (!assetList.isEmpty())
                {
                    NFTAsset asset = token.getAssetForToken(token.getUniqueTokenIds().get(0));
                    assetImage.setupTokenImage(asset);
                    tokenDescription.setText(asset.getDescription());
                }

                tokenInfoLayout.addView(new TokenInfoCategoryView(getContext(), "Details"));
                addInfoView("Issuer", token.tokenInfo.name);
                addInfoView("Contract Address", token.tokenInfo.address);
                addInfoView("Blockchain", token.getNetworkName());
            }
            else if (token.getInterfaceSpec() == ContractType.ERC1155)
            {
                NFTAsset asset = token.getCollectionMap().entrySet().iterator().next().getValue(); // Get first asset

                assetImage.setupTokenImage(asset);

                tokenDescription.setText(asset.getDescription());

                /** TODO: Doesn't seem to work for now, using a temporary working placeholder
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
                 **/
            }
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
