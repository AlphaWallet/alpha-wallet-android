package com.alphawallet.app.router;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.Erc20DetailActivity;
import com.alphawallet.app.ui.NFTActivity;

public class TokenDetailRouter
{
    public Intent makeERC20DetailsIntent(Context context, String address, String symbol, int decimals, boolean isToken, Wallet wallet, Token token, boolean hasDefinition)
    {
        Intent intent = new Intent(context, Erc20DetailActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, isToken);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, address);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        intent.putExtra(C.EXTRA_DECIMALS, decimals);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(C.EXTRA_HAS_DEFINITION, hasDefinition);
        return intent;
    }

    public void open(Activity context, String address, String symbol, int decimals, boolean isToken, Wallet wallet, Token token, boolean hasDefinition)
    {
        Intent intent = makeERC20DetailsIntent(context, address, symbol, decimals, isToken, wallet, token, hasDefinition);
        context.startActivityForResult(intent, C.TOKEN_SEND_ACTIVITY);
    }

    public void openERC1155(Activity context, Token token, Wallet wallet, boolean hasDefinition)
    {
        Intent intent = new Intent(context, NFTActivity.class);
        intent.putExtra(C.EXTRA_SENDING_TOKENS, true);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_SYMBOL, token.getSymbol());
        intent.putExtra(C.EXTRA_DECIMALS, 0);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra(C.EXTRA_HAS_DEFINITION, hasDefinition);
        context.startActivityForResult(intent, C.TOKEN_SEND_ACTIVITY);
    }
}
