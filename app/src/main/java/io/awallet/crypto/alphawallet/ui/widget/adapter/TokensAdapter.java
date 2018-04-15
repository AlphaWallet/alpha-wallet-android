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

import org.web3j.utils.Numeric;

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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private static final String TAG = "TKNADAPTER";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;

    private int filterType;
    private Context context;
    private boolean needsRefresh;

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
        needsRefresh = true;
    }

    public TokensAdapter(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
        needsRefresh = true;
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
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
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
        return needsRefresh;
    }

    public void setTokens(Token[] tokens)
    {
        populateTokens(tokens);
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
                }
                break;

            default:
                updateToken(token);
                break;
        }
    }

    /**
     * Leveraging the power of Recycler view:
     * How I learned to love recycler view not to fight it.
     * Why assume that Google developers who only look at updating views
     * don't get exactly what's needed. A simple means to update only when something changes
     * based on the rules you provide in the 'SortedList' class.
     *
     * It works exactly as intended when you simply let it do its job.
     *
     * @param token
     */
    private void updateToken(Token token)
    {
        boolean updated = false;
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                Token thisToken = ((TokenSortedItem)si).value;
                if (thisToken.getAddress().equals(token.getAddress()))
                {
                    if (token.hasPositiveBalance())
                    {
                        items.add(new TokenSortedItem(token, calculateWeight(token)));
                    }
                    else
                    {
                        items.removeItemAt(i);
                        notifyItemRemoved(i);
                        notifyDataSetChanged();
                    }
                    updated = true;
                    break;
                }
            }
        }

        //New token added
        //after extensive testing it's better not to take any risks - emptying items and rebuilding the list never fails.
        //However all other methods (notify range changed, notify dataset etc GPF under heavy stress.
        //If you want to switch on the view stress test search for 'throw new BadContract' in TokenRepository and uncomment the random throw
        //this causes tokens to pop in and out of this view very frequently.
        if (!updated && token.hasPositiveBalance())
        {
            needsRefresh = true;
        }
    }

    private void populateTokens(Token[] tokens)
    {
        items.beginBatchedUpdates();
        if (needsRefresh) items.clear();
        items.add(total);

        for (int i = 0; i < tokens.length; i++) {
            Token token = tokens[i];
            if (token.tokenInfo.symbol.equals(ETH_SYMBOL) || token.hasPositiveBalance())
            {
                switch (filterType)
                {
                    case FILTER_ASSETS:
                    case FILTER_CURRENCY:
                        if (token.isCurrency())
                        {
                            items.add(new TokenSortedItem(token, calculateWeight(token)));
                        }
                        break;

                    default:
                        items.add(new TokenSortedItem(token, calculateWeight(token)));
                        break;
                }
            }
        }
        items.endBatchedUpdates();
        needsRefresh = false;
    }

    private int calculateWeight(Token token)
    {
        //calculate the weight from the name. Add the contract address too
        int weight = 0;
        //use first 5 letters + first 4 address to arbitrate
        String tokenName = token.getFullName();

        if(token.tokenInfo.symbol.equals(ETH_SYMBOL)) return 5;

        int i = 4;
        int pos = 0;

        while (i >= 0)
        {
            char c = tokenName.charAt(pos++);
            int w = Character.toLowerCase(c) - 'a' + 1;
            if (w > 0)
            {
                weight += ((i+4)*26)*w;
                i--;
            }
        }

        String address = Numeric.cleanHexPrefix(token.getAddress());
        for (i = 0; i < address.length() && i < 3; i++)
        {
            char c = address.charAt(i);
            int w = c - '0';
            weight += ((3-i)*10)*w;
        }

        return weight;
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
