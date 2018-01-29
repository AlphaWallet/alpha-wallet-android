package com.wallet.crypto.trustapp.entity;

import org.web3j.abi.datatypes.generated.Uint16;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by James on 27/01/2018.
 */

public class TokenFactory
{
    public Token CreateToken(TokenInfo tokenInfo, BigDecimal balance, List<Integer> balances)
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
                thisToken = new Ticket((TicketInfo)tokenInfo, balances);
            }
        }
        else
        {
            thisToken = new Token(tokenInfo, balance);
        }

        return thisToken;
    }
}
