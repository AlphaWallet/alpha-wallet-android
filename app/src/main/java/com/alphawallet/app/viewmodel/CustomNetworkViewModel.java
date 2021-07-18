package com.alphawallet.app.viewmodel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.Erc20DetailRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.zxing.QRScanningActivity;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class CustomNetworkViewModel extends BaseViewModel
{
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    CustomNetworkViewModel(
            EthereumNetworkRepositoryType ethereumNetworkRepository)
    {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public void addNetwork(String name, String rpcUrl, int chainId, String symbol, String blockExplorerUrl, boolean isTestnet) {
        this.ethereumNetworkRepository.addCustomRPCNetwork(name, rpcUrl, chainId, symbol, blockExplorerUrl, isTestnet);
    }
}
