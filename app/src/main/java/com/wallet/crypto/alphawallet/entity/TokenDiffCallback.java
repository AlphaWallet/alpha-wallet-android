package com.wallet.crypto.alphawallet.entity;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

/**
 * Created by James on 15/03/2018.
 */

public class TokenDiffCallback extends DiffUtil.Callback {

    private final List<Token> mOldTokenList;
    private final List<Token> mNewTokenList;

    public TokenDiffCallback(List<Token> oldList, List<Token> newList) {
        this.mOldTokenList = oldList;
        this.mNewTokenList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldTokenList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewTokenList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return (mOldTokenList.get(oldItemPosition).getAddress().equals(mNewTokenList.get(
                newItemPosition).getAddress()));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        final Token oldToken = mOldTokenList.get(oldItemPosition);
        final Token newToken = mNewTokenList.get(newItemPosition);

        return (oldToken.getAddress().equals(newToken.getAddress()))
                && (oldToken.getStringBalance().equals(newToken.getStringBalance()));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}