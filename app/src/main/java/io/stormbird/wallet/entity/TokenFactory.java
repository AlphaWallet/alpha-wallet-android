package io.stormbird.wallet.entity;

import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.repository.entity.RealmERC721Token;
import io.stormbird.wallet.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 27/01/2018.
 */

public class TokenFactory
{
    public Token createToken(TokenInfo tokenInfo, BigDecimal balance, List<BigInteger> balances, List<Integer> burned, long updateBlancaTime, ContractType type)
    {
        Token thisToken;
        switch (type)
        {
            case ERC875:
            case ERC875LEGACY:
                thisToken = new Ticket(tokenInfo, balances, burned, updateBlancaTime);
                break;
            case ERC721:
                //TODO:
                thisToken = new ERC721Token(tokenInfo, new ArrayList<Asset>(), updateBlancaTime);
                break;
            case ERC20:
            case ETHEREUM:
                thisToken = new Token(tokenInfo, balance, updateBlancaTime);
                break;
            default:
                thisToken = new Token(tokenInfo, balance, updateBlancaTime);
                break;
        }

        thisToken.setInterfaceSpec(type);

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo, RealmToken realmItem, long updateBlancaTime)
    {
        Token thisToken;
        int typeOrdinal = realmItem.getInterfaceSpec();
        if (typeOrdinal > ContractType.CREATION.ordinal()) typeOrdinal = ContractType.NOT_SET.ordinal();

        ContractType type = ContractType.values()[typeOrdinal];
        String realmBalance = realmItem.getBalance();

        switch (type)
        {
            case ETHEREUM:
            case ERC20:
                if (realmBalance == null || realmBalance.length() == 0) realmBalance = "0";
                BigDecimal balance = new BigDecimal(realmBalance);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime);
                thisToken.setInterfaceSpecFromRealm(realmItem);
                break;

            case ERC875:
            case ERC875LEGACY:
                if (realmBalance == null) realmBalance = "";
                thisToken = new Ticket(tokenInfo, realmBalance, realmItem.getBurnList(), updateBlancaTime);
                thisToken.setInterfaceSpecFromRealm(realmItem);
                break;

            case OTHER:
                balance = new BigDecimal(0);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime);
                thisToken.setInterfaceSpec(ContractType.OTHER);
                break;

            default:
                balance = new BigDecimal(0);
                thisToken = new Token(tokenInfo, balance, updateBlancaTime);
                thisToken.setInterfaceSpec(ContractType.NOT_SET);
                break;

        }

        thisToken.restoreAuxDataFromRealm(realmItem);
        thisToken.lastBlockCheck = realmItem.getLastBlock();

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo, ContractType type)
    {
        Token thisToken;
        switch (type)
        {
            case ERC875:
            case ERC875LEGACY:
                thisToken = new Ticket(tokenInfo, new ArrayList<BigInteger>(), new ArrayList<Integer>(), 0);
                break;
            case ERC721:
                thisToken = new ERC721Token(tokenInfo, new ArrayList<Asset>(), 0);
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
                        null, 0);
                break;
        }

        thisToken.setInterfaceSpec(type);

        return thisToken;
    }

    public TokenInfo createTokenInfo(RealmToken realmItem)
    {
        return new TokenInfo(realmItem.getAddress(), realmItem.getName(), realmItem.getSymbol(),
                realmItem.getDecimals(), true, realmItem.getChainId());
    }

    public Token createERC721Token(RealmERC721Token realmItem, List<Asset> assets, long updateTime)
    {
        TokenInfo tf = new TokenInfo(realmItem.getAddress(), realmItem.getName(), realmItem.getSymbol(), 0, true, 0);
        return new ERC721Token(tf, assets, updateTime);
    }
}
