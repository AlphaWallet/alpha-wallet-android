package com.alphawallet.app.ui;


import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.NFTAssetsAdapter;
import com.alphawallet.app.ui.widget.adapter.NonFungibleTokenAdapter;
import com.alphawallet.app.ui.widget.divider.ItemOffsetDecoration;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.NFTAssetsViewModel;
import com.alphawallet.app.viewmodel.NFTAssetsViewModelFactory;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigInteger;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class NFTAssetsFragment extends BaseFragment implements OnAssetClickListener, TokensAdapterCallback {
    @Inject
    NFTAssetsViewModelFactory viewModelFactory;
    private NFTAssetsViewModel viewModel;

    private Token token;
    private Wallet wallet;
    private RecyclerView recyclerView;
    private ItemOffsetDecoration gridItemDecoration;
    private ListDivider listItemDecoration;

    private ActivityResultLauncher<Intent> handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() == null) return;
                String transactionHash = result.getData().getStringExtra(C.EXTRA_TXHASH);
                //process hash
                if (!TextUtils.isEmpty(transactionHash))
                {
                    Intent intent = new Intent();
                    intent.putExtra(C.EXTRA_TXHASH, transactionHash);
                    getActivity().setResult(RESULT_OK, intent);
                    getActivity().finish();
                }
            });

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
            viewModel = new ViewModelProvider(this, viewModelFactory)
                    .get(NFTAssetsViewModel.class);

            long chainId = getArguments().getLong(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
            token = viewModel.getTokensService().getToken(chainId, getArguments().getString(C.EXTRA_ADDRESS));
            wallet = getArguments().getParcelable(C.Key.WALLET);

            recyclerView = view.findViewById(R.id.recycler_view);

            gridItemDecoration = new ItemOffsetDecoration(recyclerView.getContext(), R.dimen.grid_divider_offset);

            listItemDecoration = new ListDivider(recyclerView.getContext());

            showGridView();
        }
    }

    @Override
    public void onAssetClicked(Pair<BigInteger, NFTAsset> item)
    {
        if (item.second.isCollection())
        {
            handleTransactionSuccess.launch(viewModel.showAssetListDetails(getContext(), wallet, token, item.second));
        }
        else
        {
            handleTransactionSuccess.launch(viewModel.showAssetDetails(getContext(), wallet, token, item.first));
        }
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected)
    {
        handleTransactionSuccess.launch(viewModel.showAssetDetails(getContext(), wallet, token, tokenIds.get(0)));
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
    {

    }

    public void showGridView()
    {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.removeItemDecoration(listItemDecoration);
        recyclerView.addItemDecoration(gridItemDecoration);
        recyclerView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));

        if (hasTokenScriptOverride(token))
        {
            NonFungibleTokenAdapter adapter = new NonFungibleTokenAdapter(this, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService(), getActivity(), true);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            NFTAssetsAdapter adapter = new NFTAssetsAdapter(getActivity(), token, this, true);
            recyclerView.setAdapter(adapter);

        }
    }

    public void showListView()
    {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.removeItemDecoration(gridItemDecoration);
        recyclerView.addItemDecoration(listItemDecoration);
        recyclerView.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.background_bottom_border));
        recyclerView.setPadding(0, 0, 0, 0);
        if (hasTokenScriptOverride(token))
        {
            NonFungibleTokenAdapter adapter = new NonFungibleTokenAdapter(this, token, viewModel.getAssetDefinitionService(), viewModel.getOpenseaService(), getActivity(), false);
            recyclerView.setAdapter(adapter);
        }
        else
        {
            NFTAssetsAdapter adapter = new NFTAssetsAdapter(getActivity(), token, this, false);
            recyclerView.setAdapter(adapter);
        }
    }

    private boolean hasTokenScriptOverride(Token t)
    {
        return viewModel.getAssetDefinitionService().hasTokenView(t.tokenInfo.chainId, t.getAddress(), AssetDefinitionService.ASSET_SUMMARY_VIEW_NAME);
    }
}
