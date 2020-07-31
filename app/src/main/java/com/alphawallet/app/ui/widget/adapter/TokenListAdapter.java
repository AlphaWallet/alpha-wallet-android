package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
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

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.TokenManageType.DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.HIDDEN_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_HIDDEN_TOKEN;

public class TokenListAdapter extends RecyclerView.Adapter<BinderViewHolder> implements OnTokenManageClickListener {

    private final Context context;
    private ItemClickListener listener;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;

    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            //Note: ViewTypes are numbered in order of appearance
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
        }

        @Override
        public void onChanged(int position, int count) {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem) {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem item1, SortedItem item2) {
            return item1.areItemsTheSame(item2);
        }

        @Override
        public void onInserted(int position, int count) {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            notifyItemMoved(fromPosition, toPosition);
        }
    });

    public TokenListAdapter(Context context, AssetDefinitionService aService, TokensService tService, TokenCardMeta[] tokens, ItemClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.assetService = aService;
        this.tokensService = tService;

        List<TokenCardMeta> tokenList = filterTokens(Arrays.asList(tokens));

        setupList(tokenList);
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

    private void setupList(List<TokenCardMeta> tokens)
    {
        int hiddenTokensCount = 0;

        items.clear();
        items.beginBatchedUpdates();

        for (TokenCardMeta tokenCardMeta : tokens)
        {
            TokenSortedItem sortedItem;
            Token token = tokensService.getToken(tokenCardMeta.getChain(), tokenCardMeta.getAddress());
            tokenCardMeta.isEnabled = token.tokenInfo.isEnabled;

            if (token.tokenInfo.isEnabled)
            {
                sortedItem = new TokenSortedItem(
                        DISPLAY_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            else
            {
                hiddenTokensCount++;
                sortedItem = new TokenSortedItem(
                        HIDDEN_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            items.add(sortedItem);
        }
        items.add(new ManageTokensLabelSortedItem(
                LABEL_DISPLAY_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.display_tokens)),
                0));

        //if there are no hidden tokens found no need to display label
        if (hiddenTokensCount > 0)
        {
            items.add(new ManageTokensLabelSortedItem(
                    LABEL_HIDDEN_TOKEN,
                    new ManageTokensLabelData(context.getString(R.string.hidden_tokens)),
                    0));
        }

        items.endBatchedUpdates();
    }

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
        if (!(items.get(position).value instanceof TokenCardMeta)) return;

        TokenCardMeta tcm = (TokenCardMeta)items.get(position).value;

        TokenSortedItem updateItem = new TokenSortedItem(
                DISPLAY_TOKEN, tcm, tcm.nameWeight
        );;

        items.beginBatchedUpdates();
        if (items.get(position).viewType == DISPLAY_TOKEN)
        {
            updateItem.value.isEnabled = false;
            updateItem.viewType = HIDDEN_TOKEN;

            //Ensure hidden token label displayed if required
            items.add(new ManageTokensLabelSortedItem(
                    LABEL_HIDDEN_TOKEN,
                    new ManageTokensLabelData(context.getString(R.string.hidden_tokens)),
                    0));
        }
        else
        {
            updateItem.value.isEnabled = true;
        }

        items.removeItemAt(position);
        items.add(updateItem);
        items.endBatchedUpdates();

        notifyDataSetChanged();

        listener.onItemClick(token, isChecked);
    }

    public void filter(String searchString)
    {
        tokensService.getAllTokenMetas(searchString)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateList, error -> { })
                .isDisposed();
    }

    private void updateList(TokenCardMeta[] metas)
    {
        setupList(Arrays.asList(metas));
        notifyDataSetChanged();
    }

    public interface ItemClickListener {
        void onItemClick(Token token, boolean enabled);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }
}
