package com.alphawallet.app.entity;

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

    public static boolean filterToken(Token token, boolean filterResult)
    {
        boolean badToken = false;
        if (!token.hasDebugTokenscript && !token.hasPositiveBalance()) filterResult = false;
        if (token.isTerminated() || token.isBad()) badToken = true;

        if (token.isEthereum() && token.tokenInfo.chainId == EthereumNetworkRepository.MAINNET_ID) filterResult = true;
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

    //Switch off dapp browser?
    public static boolean hideDappBrowser()
    {
        return false;
    }

    public static boolean hideNetworkSettings() { return true; }

    public static boolean hideTabBar()
    {
        return false;
    }
}
