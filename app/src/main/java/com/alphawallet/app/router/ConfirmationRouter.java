package com.alphawallet.app.router;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.TransactionTooLargeException;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.web3.entity.Web3Transaction;

import com.alphawallet.token.tools.Convert;

import com.alphawallet.app.entity.ConfirmationType;

import java.math.BigInteger;

//TODO: Refactor when we add token type to token class
public class ConfirmationRouter {
    public void open(Context context, String to, BigInteger amount, String contractAddress, int decimals, String symbol, boolean sendingTokens, BigInteger gasPrice, String ensDetails, int chainId) {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_AMOUNT, amount.toString());
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, sendingTokens);
        intent.putExtra(C.EXTRA_ENS_DETAILS, ensDetails);
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.putExtra(C.EXTRA_GAS_PRICE, gasPrice.toString());
        int tokenType = ConfirmationType.ETH.ordinal();
        if (sendingTokens) tokenType = ConfirmationType.ERC20.ordinal();
        intent.putExtra(C.TOKEN_TYPE, tokenType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    //Sign transaction for dapp browser
    public void open(Activity context, Web3Transaction transaction, String networkName, String requesterURL, int chainId) throws TransactionTooLargeException
    {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_WEB3TRANSACTION, transaction);
        intent.putExtra(C.EXTRA_AMOUNT, Convert.fromWei(transaction.value.toString(10), Convert.Unit.WEI).toString());
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.WEB3TRANSACTION.ordinal());
        intent.putExtra(C.EXTRA_NETWORK_NAME, networkName);
        intent.putExtra(C.EXTRA_ACTION_NAME, requesterURL);
        intent.putExtra(C.EXTRA_NETWORKID, chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.REQUEST_TRANSACTION_CALLBACK);
    }

    public void openERC721Transfer(Context context, String to, String tokenId, String contractAddress, String name, String tokenName, String ensDetails, Token token)
    {
        Intent intent = new Intent(context, ConfirmationActivity.class);
        intent.putExtra(C.EXTRA_TO_ADDRESS, to);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, contractAddress);
        intent.putExtra(C.EXTRA_DECIMALS, 0);
        intent.putExtra(C.EXTRA_SYMBOL, tokenName);
        intent.putExtra(C.EXTRA_AMOUNT, "0");
        intent.putExtra(C.EXTRA_SENDING_TOKENS, true);
        intent.putExtra(C.TOKEN_TYPE, ConfirmationType.ERC721.ordinal());
        intent.putExtra(C.EXTRA_TOKENID_LIST, tokenId);
        intent.putExtra(C.EXTRA_ACTION_NAME, name);
        intent.putExtra(C.EXTRA_ENS_DETAILS, ensDetails);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.putExtra(C.EXTRA_NETWORKID, token.tokenInfo.chainId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
