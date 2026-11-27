package com.alphawallet.app.ui.widget.holder;

import androidx.annotation.Nullable;

import com.alphawallet.app.entity.tokens.Token;

/**
 * Supplies tokens already loaded for a RecyclerView window. Implementations must
 * never perform synchronous storage work from this method.
 */
public interface TokenProvider
{
    @Nullable
    Token getToken(long chainId, String address);
}
