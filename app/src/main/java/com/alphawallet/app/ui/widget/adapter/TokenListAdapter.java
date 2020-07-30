package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.TokenManageType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenManageClickListener;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelData;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TokenLabelViewHolder;
import com.alphawallet.app.ui.widget.holder.TokenListHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.alphawallet.app.entity.TokenManageType.DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.HIDDEN_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_HIDDEN_TOKEN;

public class TokenListAdapter extends RecyclerView.Adapter<BinderViewHolder> implements Filterable, OnTokenManageClickListener {

    private final Context context;
    private ItemClickListener listener;
    protected final AssetDefinitionService assetService;
    private ArrayList<SortedItem> items;
    protected final TokensService tokensService;

    public TokenListAdapter(Context context, AssetDefinitionService aService, TokensService tService, TokenCardMeta[] tokens, ItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.assetService = aService;
        this.tokensService = tService;

        List<TokenCardMeta> tokenList = filterTokens(Arrays.asList(tokens));

        items = new ArrayList<>();
        items.addAll(setupList(tokenList));
    }

    private List<TokenCardMeta> filterTokens(List<TokenCardMeta> tokens) {
        ArrayList<TokenCardMeta> filteredList = new ArrayList<>();
        for (TokenCardMeta t : tokens)
        {
            if (!t.isEthereum() && !filteredList.contains(t))
            {
                filteredList.add(t);
            }
        }

        return filteredList;
    }

    private ArrayList<SortedItem> setupList(List<TokenCardMeta> tokens)
    {
        ArrayList<SortedItem> sortedItems = new ArrayList<>();
        for (TokenCardMeta tokenCardMeta : tokens)
        {
            TokenSortedItem sortedItem;
            Token token = tokensService.getToken(tokenCardMeta.getChain(), tokenCardMeta.getAddress());
            tokenCardMeta.isEnabled = token.tokenInfo.isEnabled;
            tokenCardMeta.isVisible = true;
            if (token.tokenInfo.isEnabled)
            {
                sortedItem = new TokenSortedItem(
                        DISPLAY_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            else
            {
                sortedItem = new TokenSortedItem(
                        HIDDEN_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            sortedItems.add(sortedItem);
        }
        sortedItems.add(new ManageTokensLabelSortedItem(
                LABEL_DISPLAY_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.display_tokens)),
                0));
        sortedItems.add(new ManageTokensLabelSortedItem(
                LABEL_HIDDEN_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.hidden_tokens)),
                0));

        Collections.sort(sortedItems, compareByWeight);

        return  sortedItems;
    }

    /*
    Below comparision is like
    First, check for the ViewType which could be any of @TokenManageType
    Second, if type is similar, check for the weight given to the Token
     */
    Comparator<SortedItem> compareByWeight = (o1, o2) -> {
        if (o1.viewType < o2.viewType)
        {
            return -1;
        }
        else if (o1.viewType > o2.viewType)
        {
            return 1;
        }
        else
        {
            return Integer.compare(o1.weight, o2.weight);
        }
    };

    @NonNull
    @Override
    public BinderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, @TokenManageType.ManageType int viewType) {
        switch (viewType) {
            case LABEL_DISPLAY_TOKEN:
            case LABEL_HIDDEN_TOKEN:
                TokenLabelViewHolder tokenLabelViewHolder = new TokenLabelViewHolder(R.layout.layout_manage_tokens_label, viewGroup);
                return tokenLabelViewHolder;
            case DISPLAY_TOKEN:
            case HIDDEN_TOKEN:
            default:
                TokenListHolder tokenListHolder = new TokenListHolder(R.layout.item_manage_token, viewGroup, assetService, tokensService);
                tokenListHolder.setOnTokenClickListener(this);
                return tokenListHolder;
        }
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        items.get(position).view = holder;
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        holder.bind(items.get(position).value, bundle);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public void onTokenClick(Token token, int position, boolean isChecked)
    {
        int type = items.get(position).viewType;

        if (type == DISPLAY_TOKEN)
        {
            items.get(position).viewType = HIDDEN_TOKEN;
            ((TokenCardMeta)items.get(position).value).isEnabled = false;
        }
        else
        {
            items.get(position).viewType = DISPLAY_TOKEN;
            ((TokenCardMeta)items.get(position).value).isEnabled = true;
        }

        Collections.sort(items, compareByWeight);
        notifyDataSetChanged();

        listener.onItemClick(token, isChecked);
    }

    public interface ItemClickListener {
        void onItemClick(Token token, boolean enabled);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String searchString = charSequence.toString();
                if (searchString.isEmpty())
                {
                    for (SortedItem row : items)
                    {
                        if (row instanceof TokenSortedItem)
                        {
                            TokenCardMeta sortedItem = (TokenCardMeta) row.value;
                            sortedItem.isVisible = true;
                        }
                    }
                }
                else
                {
                    ArrayList<SortedItem> tokenList = new ArrayList<>(items);
                    for (SortedItem row : tokenList)
                    {
                        if (row instanceof TokenSortedItem)
                        {
                            TokenCardMeta sortedItem = (TokenCardMeta) row.value;
                            final Token token = tokensService.getToken(sortedItem.getChain(), sortedItem.getAddress());
                            if (token.getFullName(assetService, 1).toLowerCase().contains(searchString.toLowerCase()))
                            {
                                sortedItem.isVisible = true;
                            }
                            else
                            {
                                sortedItem.isVisible = false;
                            }
                        }
                    }
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = items;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                notifyDataSetChanged();
            }
        };
    }
}
