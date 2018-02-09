package com.wallet.crypto.alphawallet.entity;

import android.text.TextUtils;

import com.wallet.crypto.alphawallet.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.util.List;

import static com.wallet.crypto.alphawallet.repository.TokensRealmSource.ACTUAL_BALANCE_INTERVAL;

/**
 * Created by James on 27/01/2018.
 */

public class TokenFactory
{
    public Token createToken(TokenInfo tokenInfo, BigDecimal balance, List<Integer> balances, long updateBlancaTime)
    {
        Token thisToken;
        if (tokenInfo instanceof TicketInfo)
        {
            if (balances == null)
            {
                thisToken = null; //prune this entry out if it doesn't match
            }
            else
            {
                thisToken = new Ticket((TicketInfo)tokenInfo, balances, updateBlancaTime);
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
        if (tokenInfo instanceof TicketInfo)
        {
            String balances = realmItem.getBalance();
            thisToken = new Ticket((TicketInfo)tokenInfo, balances, updateBlancaTime);
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

    public Token createToken(TokenInfo tokenInfo)
    {
        Token thisToken;
        if (tokenInfo instanceof TicketInfo)
        {
            thisToken = new Ticket((TicketInfo)tokenInfo, (List<Integer>)null, 0);
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
        TokenInfo ti;
        TokenInfo token = new TokenInfo(realmItem.getAddress(), realmItem.getName(), realmItem.getSymbol(),
                realmItem.getDecimals(), true);
        if (realmItem.isStormbird())
        {
            ti = new TicketInfo(token);
        }
        else
        {
            ti = token;
        }

        return ti;
    }
}
