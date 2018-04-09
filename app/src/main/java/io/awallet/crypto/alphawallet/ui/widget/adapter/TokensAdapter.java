package io.awallet.crypto.alphawallet.ui.widget.adapter;

import android.content.Context;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenDiffCallback;
import io.awallet.crypto.alphawallet.entity.Transaction;
import io.awallet.crypto.alphawallet.entity.TransactionDiffCallback;
import io.awallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.SortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TokenSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.entity.TotalBalanceSortedItem;
import io.awallet.crypto.alphawallet.ui.widget.holder.BinderViewHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TokenHolder;
import io.awallet.crypto.alphawallet.ui.widget.holder.TotalBalanceHolder;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;

    private int filterType;
    private Context context;
    private int tokenCount;

    protected final OnTokenClickListener onTokenClickListener;
    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>() {
        @Override
        public int compare(SortedItem o1, SortedItem o2) {
            return o1.compare(o2);
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

    protected TotalBalanceSortedItem total = new TotalBalanceSortedItem(null);

    public TokensAdapter(Context context, OnTokenClickListener onTokenClickListener) {
        this.context = context;
        this.onTokenClickListener = onTokenClickListener;
    }

    public TokensAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    public TokensAdapter() {
        onTokenClickListener = null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(R.layout.item_token, parent);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
                setAnimation(holder.itemView);
            }
            break;
            // NB to save ppl a lot of effort this view doesn't show - item_total_balance has height coded to 1dp.
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
                setAnimation(holder.itemView);
            }
        }
        return holder;
    }

    private void setAnimation(View viewToAnimate) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_from_bottom);
            viewToAnimate.startAnimation(animation);
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        items.get(position).view = holder;
        holder.bind(items.get(position).value);
    }

    @Override
    public int getItemViewType(int position) {
        int type = items.get(position).viewType;
        return type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean checkTokens()
    {
        if (items.size() != 0 && tokenCount != (items.size() - 1))
        {
            tokenCount = 0;
            return true;
        }
        else
        {
            tokenCount = 0;
            return false;
        }
    }

    public void setTokens(Token[] tokens)
    {
        if (items.size() == 0 || tokenCount != (items.size() - 1))
        {
            //only need a full update if there's been a change in token count
            populateTokens(tokens);
        }

        tokenCount = 0;
    }

    public void updateTokenCheck(Token token)
    {
        switch (filterType)
        {
            case FILTER_ASSETS:
            case FILTER_CURRENCY:
                if (token.isCurrency())
                {
                    updateToken(token);
                    tokenCount++; //can't put this in updateToken, we are checking for size mismatch
                }
                break;

            default:
                updateToken(token);
                tokenCount++;
                break;
        }
    }

    private void updateToken(Token token)
    {
        //update the token in place if required
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                Token thisToken = ((TokenSortedItem)si).value;
                if (thisToken.getAddress().equals(token.getAddress()))
                {
                    //TODO: see if balance or any other data changed, only update if different
                    ((TokenSortedItem)si).value = token;
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void populateTokens(Token[] tokens)
    {
        items.beginBatchedUpdates();
        items.clear();
        items.add(total);

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];
            if (token.tokenInfo.symbol.equals(ETH_SYMBOL) || token.getBalanceQty() > 0)
            {
                switch (filterType)
                {
                    case FILTER_ASSETS:
                    case FILTER_CURRENCY:
                        if (token.isCurrency())
                        {
                            items.add(new TokenSortedItem(token, 10 + i));
                        }
                        break;

                    default:
                        items.add(new TokenSortedItem(token, 10 + i));
                        break;
                }
            }
        }
        items.endBatchedUpdates();
    }

    public void setTotal(BigDecimal totalInCurrency) {
        total = new TotalBalanceSortedItem(totalInCurrency);
        //see if we need an update
        items.beginBatchedUpdates();
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TotalBalanceSortedItem)
            {
                items.remove((TotalBalanceSortedItem)si);
                items.add(total);
                notifyItemChanged(i);
                break;
            }
        }
        items.endBatchedUpdates();
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    public void clear() {
        items.clear();
    }
}
