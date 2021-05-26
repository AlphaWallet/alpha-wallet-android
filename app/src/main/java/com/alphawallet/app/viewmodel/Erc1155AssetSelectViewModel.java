package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.tokens.ERC1155Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import java.util.HashMap;
import java.util.Map;

public class Erc1155AssetSelectViewModel extends BaseViewModel {
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private MutableLiveData<Map<Long, ERC1155Asset>> assets = new MutableLiveData<>();


    public Erc1155AssetSelectViewModel(FetchTransactionsInteract fetchTransactionsInteract,
                                       AssetDefinitionService assetDefinitionService,
                                       TokensService tokensService)
    {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return this.assetDefinitionService;
    }

    public LiveData<Map<Long, ERC1155Asset>> assets() {
        return assets;
    }

    public void getAssets(Token token)
    {
        // TODO: Replace with actual data.
        // Map<Long, ERC1155Asset> data = token.getAssets();

        Map<Long, ERC1155Asset> fakeData = new HashMap<>();
        fakeData.put(1L, new ERC1155Asset("", "Sample Token", "10 Assets | Infinite FT"));
        fakeData.put(2L, new ERC1155Asset("", "Sample Token", "20 Assets | Infinite FT"));
        fakeData.put(3L, new ERC1155Asset("", "Sample Token", "30 Assets | Infinite FT"));

        assets.postValue(fakeData);
    }
}
