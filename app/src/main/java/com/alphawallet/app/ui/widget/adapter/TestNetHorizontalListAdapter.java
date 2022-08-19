package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.widget.TokenIcon;

import java.math.BigInteger;

public class TestNetHorizontalListAdapter extends RecyclerView.Adapter<TestNetHorizontalListAdapter.ViewHolder>
{
    private final Token[] tokens;
    protected final TokensService tokensService;
    private final Context context; //TODO - JB: should be final

    //TODO - JB: populate the token list using this method:
    // - from tokensService get the tokenFilter with getNetworkFilters()
    // - loop through this list and check for non-zero balance testnet (using getTokenOrBase(chainId, tokensService.getCurrentAddress()) )
    // - send list of tokens below but use Token[] instead of TokenCardMeta[]. Now you won't need TokensService or AssetDefinitionService

    public TestNetHorizontalListAdapter(Token[] tokens, TokensService tokensService, Context context)
    {
        this.tokens = tokens;
        this.tokensService = tokensService;
        this.context = context;
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
        Token token;
        holder.tokenIcon.clearLoad();
        try
        {
            token = tokensService.getToken(tokens[position].tokenInfo.chainId, tokens[position].getAddress());
            if (token == null)
            {
                holder.price.setText("");
                //TODO - JB: delete this
                return;
            }
            String coinBalance = token.getStringBalanceForUI(4);
            if (!TextUtils.isEmpty(coinBalance))
            {

                String symbol = token.getSymbol().substring(0, Math.min(token.getSymbol().length(), 5))
                        .toUpperCase();

                holder.price.setText(context.getString(R.string.valueSymbol, coinBalance, symbol));
            }

            // TODO - JB: use bindData(token.tokenInfo.chainId), remove import of assetDefinitionService
            holder.tokenIcon.bindData(token.tokenInfo.chainId);
            if (!token.isEthereum())
                holder.tokenIcon.setChainIcon(token.tokenInfo.chainId); //Add in when we upgrade the design

        }
        catch (Exception ex)
        {

        }

    }

    @Override
    public int getItemCount()
    {
        return tokens.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TokenIcon tokenIcon;
        TextView assetName, price;
        RelativeLayout mainLayout;

        ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            tokenIcon = itemView.findViewById(R.id.token_icon);
            assetName = itemView.findViewById(R.id.text_asset_name);
            price = itemView.findViewById(R.id.title_set_price);
            mainLayout = itemView.findViewById(R.id.chainLayout);
        }
    }
}
