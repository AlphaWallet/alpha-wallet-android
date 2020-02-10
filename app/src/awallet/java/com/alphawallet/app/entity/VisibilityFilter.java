package com.alphawallet.app.entity;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.entity.NetworkItem;

import java.util.concurrent.ConcurrentLinkedQueue;

public class VisibilityFilter
{
    private static int primaryChain = EthereumNetworkRepository.MAINNET_ID;
    private static String primaryChainName = C.ETHEREUM_NETWORK_NAME;

    public static boolean filterToken(Token token, boolean filterResult, Context context)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean hideZeroBalanceTokens = pref.getBoolean("hide_zero_balance_tokens", false);

        if (hideZeroBalanceTokens && !token.hasPositiveBalance()) {
            filterResult = false;
        }

        boolean badToken = token.isTerminated() || token.isBad();
        if (!token.tokenInfo.isEnabled) filterResult = false;
        if (token.isEthereum()) filterResult = true;
        return !badToken && filterResult;
    }

    public static void addPriorityTokens(ConcurrentLinkedQueue<ContractResult> unknownAddresses, TokensService tokensService)
    {

    }

    public static ContractType checkKnownTokens(TokenInfo tokenInfo)
    {
        return ContractType.OTHER;
    }

    public static boolean showContractAddress(Token token)
    {
        return true;
    }

    public static boolean isPrimaryNetwork(NetworkItem item)
    {
        return item.getChainId() == primaryChain;
    }

    public static String primaryNetworkName()
    {
        return primaryChainName;
    }

    public static long startupDelay()
    {
        return 0;
    }

    public static int getImageOverride()
    {
        return 0;
    }

    public static int getIssuerName()
    {
        return R.string.eth;
    }

    //Switch off dapp browser
    public static boolean hideDappBrowser()
    {
        return false;
    }

    public static boolean hideNetworkSettings() { return true; }

    public static boolean hideTabBar()
    {
        return false;
    }

    //Use to switch off direct transfer, only use magiclink transfer
    public static boolean hasDirectTransfer()
    {
        return true;
    }

    //Allow multiple wallets (true) or single wallet mode (false)
    public static boolean canChangeWallets()
    {
        return true;
    }
}
