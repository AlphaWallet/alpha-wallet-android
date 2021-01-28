package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnTokenManageClickListener;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelData;

public class TokenLabelViewHolder extends BinderViewHolder<ManageTokensLabelData> implements View.OnClickListener {

    public TextView textTitle;
    public TextView textIgnore;
    private OnTokenManageClickListener onTokenClickListener;

    public TokenLabelViewHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        textTitle = itemView.findViewById(R.id.text_title);
        textIgnore = itemView.findViewById(R.id.text_ignore);
        if (textIgnore != null) textIgnore.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable ManageTokensLabelData data, @NonNull Bundle addition) {
        textTitle.setText(data.title);
    }

    @Override
    public void onClick(View v) {
        //TODO to get Ignore view click
    }

    public void setOnTokenClickListener(OnTokenManageClickListener onClickListener) {
        this.onTokenClickListener = onClickListener;
    }
}