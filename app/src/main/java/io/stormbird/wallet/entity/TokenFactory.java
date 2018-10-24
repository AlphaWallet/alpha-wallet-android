package io.stormbird.wallet.entity;

import android.text.TextUtils;

import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.repository.entity.RealmERC721Token;
import io.stormbird.wallet.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.stormbird.wallet.repository.TokensRealmSource.ACTUAL_BALANCE_INTERVAL;

/**
 * Created by James on 27/01/2018.
 */

public class TokenFactory
{
    public Token createToken(TokenInfo tokenInfo, BigDecimal balance, List<BigInteger> balances, List<Integer> burned, long updateBlancaTime)
    {
        Token thisToken;
        if (tokenInfo.isStormbird)
        {
            if (balances == null)
            {
                thisToken = null; //prune this entry out if it doesn't match
            }
            else
            {
                thisToken = new Ticket(tokenInfo, balances, burned, updateBlancaTime);
            }
        }
        else
        {
            thisToken = new Token(tokenInfo, balance, updateBlancaTime);
        }

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo, RealmToken realmItem, long updateBlancaTime)
    {
        Token thisToken;
        if (tokenInfo.isStormbird)
        {
            String balances = realmItem.getBalance();
            String burnList = realmItem.getBurnList();
            thisToken = new Ticket(tokenInfo, balances, burnList, updateBlancaTime);
        }
        else
        {
            long now = System.currentTimeMillis();
            BigDecimal balance = TextUtils.isEmpty(realmItem.getBalance()) || realmItem.getUpdatedTime() + ACTUAL_BALANCE_INTERVAL < now
                ? null : new BigDecimal(realmItem.getBalance());
            thisToken = new Token(tokenInfo, balance, updateBlancaTime);
        }

        return thisToken;
    }

    public Token createTokenBalance(TokenInfo tokenInfo, RealmToken realmItem, long updateBlancaTime)
    {
        Token thisToken;
        if (tokenInfo.isStormbird)
        {
            String balances = realmItem.getBalance();
            String burnList = realmItem.getBurnList();
            if (balances == null) balances = "";
            thisToken = new Ticket(tokenInfo, balances, burnList, updateBlancaTime);
        }
        else
        {
            long now = System.currentTimeMillis();
            String realmBalance = realmItem.getBalance();
            if (realmBalance == null) realmBalance = "0";
            BigDecimal balance = new BigDecimal(realmBalance);
            thisToken = new Token(tokenInfo, balance, updateBlancaTime);
        }

        return thisToken;
    }

    public Token createToken(TokenInfo tokenInfo)
    {
        Token thisToken;
        if (tokenInfo.isStormbird)
        {
            thisToken = new Ticket(tokenInfo, (List<BigInteger>)null, (List<Integer>)null, 0);
        }
        else
        {
            thisToken = new Token(
                    new TokenInfo(tokenInfo.address,
                            tokenInfo.name,
                            tokenInfo.symbol,
                            tokenInfo.decimals,
                            true),
                    null, 0);
        }

        return thisToken;
    }

    public TokenInfo createTokenInfo(RealmToken realmItem)
    {
        return new TokenInfo(realmItem.getAddress(), realmItem.getName(), realmItem.getSymbol(),
                realmItem.getDecimals(), true, realmItem.isStormbird());
    }

    public Token createERC721Token(RealmERC721Token realmItem, List<Asset> assets, long updateTime)
    {
        TokenInfo tf = new TokenInfo(realmItem.getAddress(), realmItem.getName(), realmItem.getSymbol(), 0, true);
        return new ERC721Token(tf, assets, updateTime);
    }
}
