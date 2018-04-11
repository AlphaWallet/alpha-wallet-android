package io.awallet.crypto.alphawallet.entity;

import android.text.TextUtils;

import org.web3j.abi.datatypes.generated.Bytes32;

import io.awallet.crypto.alphawallet.repository.entity.RealmToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static io.awallet.crypto.alphawallet.repository.TokensRealmSource.ACTUAL_BALANCE_INTERVAL;

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
            thisToken = new Ticket(tokenInfo, balances, burnList, updateBlancaTime);
        }
        else
        {
            long now = System.currentTimeMillis();
            BigDecimal balance = new BigDecimal(realmItem.getBalance());
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
}
