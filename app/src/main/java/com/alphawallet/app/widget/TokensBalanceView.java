package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.TestNetHorizontalListAdapter;


public class TokensBalanceView extends LinearLayout
{
    RecyclerView horizontalListView;

    public TokensBalanceView(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_token_with_balance_view, this);
        horizontalListView = findViewById(R.id.horizontal_list);
    }

    public void bindTokens(Token[] token)
    {
        TestNetHorizontalListAdapter testNetHorizontalListAdapter = new TestNetHorizontalListAdapter(token, getContext());
        horizontalListView.setAdapter(testNetHorizontalListAdapter);
    }

    public void blankView()
    {
        //clear adapter
        bindTokens(new Token[0]);
    }
}

