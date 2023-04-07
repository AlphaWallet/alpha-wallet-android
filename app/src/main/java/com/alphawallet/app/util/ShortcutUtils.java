package com.alphawallet.app.util;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;

import java.math.BigInteger;

public class ShortcutUtils
{
    public static void createShortcut(Pair<BigInteger, NFTAsset> pair, Intent intent, Context context, Token token)
    {
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, token.getAddress())
                .setShortLabel(getName(token, pair.second))
                .setLongLabel(getName(token, pair.second))
                .setIcon(IconCompat.createWithResource(context, EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId)))
                .setIntent(intent)
                .build();

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut);
    }

    private static String getName(Token token, NFTAsset asset)
    {
        if (asset.getName() == null)
        {
            return token.getFullName();
        }
        return asset.getName();
    }
}
