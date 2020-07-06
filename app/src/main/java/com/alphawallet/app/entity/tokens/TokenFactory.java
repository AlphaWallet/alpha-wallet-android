package com.alphawallet.app.entity.tokens;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 27/01/2018.
 */

public class TokenFactory
{
    public Token createToken(TokenInfo tokenInfo, BigDecimal balance, List<BigInteger> balances, long updateBlancaTime, ContractType type, String networkName, long lastBlockCheck)
    {
        Token thisToken;
        switch (type)
        {
            case ERC875:
            case ERC875_LEGACY:
                if (balances == null) balances = new ArrayList<>();
                thisToken = new Ticket(tokenInfo, balances, updateBlancaTime, networkName, type);
                break;
            case ERC721_TICKET:
                if (balances == null) balances = new ArrayList<>();
                thisToken = new ERC721Ticket(tokenInfo, balances, updateBlancaTime, networkName, type);
                break;
            case ERC721:
            case ERC721_LEGACY:
                thisToken = new ERC721Token(tokenInfo, new ArrayList<Asset>(), updateBlancaTime, networkName, type);
                break;
            case NOT_SET:
            case ERC20:
            case OTHER:
            case CURRENCY:
            case DELETED_ACCOUNT:
            case CREATION:
            case ETHEREUM:
                thisToken = new Token(tokenInfo, balance, updateBlancaTime, networkName, type);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        thisToken.lastBlockCheck = lastBlockCheck;

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo, RealmToken realmItem, long updateBlancaTime, String networkName)
    {
        Token thisToken;
        int typeOrdinal = realmItem.getInterfaceSpec();
        if (typeOrdinal > ContractType.CREATION.ordinal()) typeOrdinal = ContractType.NOT_SET.ordinal();

        ContractType type = ContractType.values()[typeOrdinal];
        String realmBalance = realmItem.getBalance();

        switch (type)
        {
            case ETHEREUM:
                tokenInfo.isEnabled = true; //native eth always enabled
            case ERC20:
                if (realmBalance == null || realmBalance.length() == 0) realmBalance = "0";
                BigDecimal balance = new BigDecimal(realmBalance);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime, networkName, type);
                thisToken.pendingBalance = balance;
                break;
            case ERC721_TICKET:
                if (realmBalance == null) realmBalance = "";
                thisToken = new ERC721Ticket(tokenInfo, realmBalance, updateBlancaTime, networkName, type);
                break;
            case ERC875:
            case ERC875_LEGACY:
                if (realmBalance == null) realmBalance = "";
                thisToken = new Ticket(tokenInfo, realmBalance, updateBlancaTime, networkName, type);
                break;

            case OTHER:
                balance = new BigDecimal(0);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime, networkName, type);
                break;

            case CURRENCY:
                if (realmBalance == null || realmBalance.length() == 0) realmBalance = "0";
                balance = new BigDecimal(realmBalance);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime, networkName, ContractType.ETHEREUM);
                thisToken.pendingBalance = balance;
                break;

            case ERC721:
            case ERC721_LEGACY:
                if (realmBalance == null) realmBalance = "";
                thisToken = new ERC721Token(tokenInfo, null, updateBlancaTime, networkName, type);

                break;

            default:
                balance = new BigDecimal(0);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime, networkName, type);
                break;

        }

        thisToken.setupRealmToken(realmItem);

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo, ContractType type, String networkName)
    {
        Token thisToken;
        long currentTime = System.currentTimeMillis();
        switch (type)
        {
            case ERC875:
            case ERC875_LEGACY:
                thisToken = new Ticket(tokenInfo, new ArrayList<BigInteger>(), currentTime, networkName, type);
                break;
            case ERC721_TICKET:
                thisToken = new ERC721Ticket(tokenInfo, new ArrayList<BigInteger>(), currentTime, networkName, type);
                break;
            case ERC721:
            case ERC721_LEGACY:
            case ERC721_UNDETERMINED:
                thisToken = new ERC721Token(tokenInfo, new ArrayList<Asset>(), currentTime, networkName, type);
                break;
            case ETHEREUM:
                String[] split = tokenInfo.address.split("-");
                thisToken = new Token(
                        new TokenInfo(split[0],
                                      tokenInfo.name,
                                      tokenInfo.symbol,
                                      tokenInfo.decimals,
                                      true,
                                      tokenInfo.chainId),
                        BigDecimal.ZERO, currentTime, networkName, type);
                thisToken.pendingBalance = BigDecimal.ZERO;
                break;
            case ERC20:
            default:
                thisToken = new Token(
                        new TokenInfo(tokenInfo.address,
                                      tokenInfo.name,
                                      tokenInfo.symbol,
                                      tokenInfo.decimals,
                                      true,
                                      tokenInfo.chainId),
                        BigDecimal.ZERO, currentTime, networkName, type);
                break;
        }

        return thisToken;
    }

    public TokenInfo createTokenInfo(RealmToken realmItem)
    {
        return new TokenInfo(realmItem.getTokenAddress(), realmItem.getName(), realmItem.getSymbol(),
                realmItem.getDecimals(), realmItem.isEnabled(), realmItem.getChainId());
    }
}
