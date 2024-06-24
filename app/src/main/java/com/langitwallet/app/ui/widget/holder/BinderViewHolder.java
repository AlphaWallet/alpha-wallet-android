package com.langitwallet.app.ui.widget.holder;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.langitwallet.app.R;
import com.langitwallet.app.C;
import com.langitwallet.app.entity.tokens.Token;
import com.langitwallet.app.ui.widget.TokensAdapterCallback;

public abstract class BinderViewHolder<T> extends RecyclerView.ViewHolder {
	public BinderViewHolder(int resId, ViewGroup parent) {
		super(LayoutInflater.from(parent.getContext())
				.inflate(resId, parent, false));
		LinearLayout backgroundLayout = findViewById(R.id.layout_background);
		if (backgroundLayout != null) { backgroundLayout.setLabelFor(0); }
	}

	public abstract void bind(@Nullable T data, @NonNull Bundle addition);

	public void bind(@Nullable T data) {
		bind(data, Bundle.EMPTY);
	}

	protected <T extends View> T findViewById(int id) {
		return itemView.findViewById(id);
	}

	protected Context getContext() {
		return itemView.getContext();
	}

	protected String getString(int stringResId) {
		return getContext().getString(stringResId);
	}

	public String getString(int stringResId, Object... args) {
		return getContext().getString(stringResId, args);
	}

	public void setOnTokenClickListener(TokensAdapterCallback tokensAdapterCallback) { }

	public void setFromTokenView() { }

	public void onDestroyView() { }

    protected void setTokenDetailName(Token token)
    {
        if (token.getSymbol().length() > C.SHORT_SYMBOL_LENGTH)
        {
            TextView tokenNameDetail = findViewById(R.id.token_name_detail);
            tokenNameDetail.setVisibility(View.VISIBLE);
            tokenNameDetail.setText(token.getFullName());
        }
        else
        {
            findViewById(R.id.token_name_detail).setVisibility(View.GONE);
        }
    }
}
