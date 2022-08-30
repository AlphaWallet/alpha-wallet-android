package com.alphawallet.app.ui.widget.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.widget.TokenWithBalanceView;

import timber.log.Timber;

public class TestNetHorizontalListAdapter extends RecyclerView.Adapter<TestNetHorizontalListAdapter.ViewHolder>
{
    private final Token[] tokens;
    //TODO - JB: populate the token list using this method:
    // - from tokensService get the tokenFilter with getNetworkFilters()
    // - loop through this list and check for non-zero balance testnet (using getTokenOrBase(chainId, tokensService.getCurrentAddress()) )
    // - send list of tokens below but use Token[] instead of TokenCardMeta[]. Now you won't need TokensService or AssetDefinitionService
    public  TestNetHorizontalListAdapter(Token[] token)
    {
        this.tokens = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horizontal_testnet_list, parent, false);
        return new TestNetHorizontalListAdapter.ViewHolder(view);
    }

    //TODO - JB: Fix formatting (no whitespace at top of function, fix tabbing)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        Token token = tokens[position];
        try
        {
           holder.tokenWithBalanceView.setTokenIconWithBalance(token);
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    @Override
    public int getItemCount()
    {
        return tokens.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TokenWithBalanceView tokenWithBalanceView;
        ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            tokenWithBalanceView = itemView.findViewById(R.id.token_with_balance_view);
        }
    }
}
