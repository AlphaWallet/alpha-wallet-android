package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.VisibilityFilter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TotalBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.entity.WarningSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private static final String TAG = "TKNADAPTER";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;
    public static final int FILTER_COLLECTIBLES = 3;
    private static final BigDecimal CUTOFF_VALUE = BigDecimal.valueOf(99999999999L);

    private int filterType;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private ContractLocator scrollToken; // designates a token that should be scrolled to

    private Context context;

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

    public TokensAdapter(OnTokenClickListener onTokenClickListener, AssetDefinitionService aService, TokensService tService, Context context) {
        this.onTokenClickListener = onTokenClickListener;
        this.assetService = aService;
        this.tokensService = tService;
        this.context = context;
    }

    protected TokensAdapter(OnTokenClickListener onTokenClickListener, AssetDefinitionService aService) {
        this.onTokenClickListener = onTokenClickListener;
        this.assetService = aService;
        this.tokensService = null;
    }

    @Override
    public long getItemId(int position) {
        Object obj = items.get(position);
        if (obj instanceof TokenSortedItem) {
            Token token = ((TokenSortedItem) obj).value;

             // This is an attempt to obtain a 'unique' id
             // to fully utilise the RecyclerView's setHasStableIds feature.
             // This will drastically reduce 'blinking' when the list changes
            return token.getUID();
        } else {
            return position;
        }
    }

    @Override
    public BinderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(R.layout.item_token, parent, assetService);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
            }
            break;
            case WarningHolder.VIEW_TYPE:
                holder = new WarningHolder(R.layout.item_warning, parent);
                break;
            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, null, assetService, false);
                break;
            default:
            // NB to save ppl a lot of effort this view doesn't show - item_total_balance has height coded to 1dp.
            case TotalBalanceHolder.VIEW_TYPE: {
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
            }
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position) {
        items.get(position).view = holder;
        holder.bind(items.get(position).value);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addWarning(WarningData data)
    {
        items.add(new WarningSortedItem(data, 0));
    }

    public void removeBackupWarning()
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == WarningHolder.VIEW_TYPE)
            {
                items.removeItemAt(i);
                notifyItemRemoved(i);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void setTokens(Token[] tokens)
    {
        populateTokens(tokens);
    }

    /**
     * Update a single item in the recycler view
     *
     * @param token
     */
    public void updateToken(Token token, boolean internal)
    {
        checkLiveToken(token);
        if (tokensService != null) tokensService.markTokenUpdated(token);
        if (canDisplayToken(token))
        {
            items.add(new TokenSortedItem(token, token.getNameWeight()));
        }
        else
        {
            removeToken(token);
        }
    }

    public void removeToken(Token token) {
        for (int i = 0; i < items.size(); i++) {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem) {
                TokenSortedItem tsi = (TokenSortedItem) si;
                Token thisToken = tsi.value;
                if (thisToken.getAddress().equals(token.getAddress()) && thisToken.tokenInfo.chainId == token.tokenInfo.chainId) {
                    items.removeItemAt(i);
                    break;
                }
            }
        }
    }

    private boolean canDisplayToken(Token token)
    {
        if (token == null) return false;
        //Add token to display list if it's the base currency, or if it has balance
        boolean allowThroughFilter = VisibilityFilter.filterToken(token, true, context);
        allowThroughFilter = checkTokenValue(token, allowThroughFilter);

        switch (filterType)
        {
            case FILTER_ASSETS:
                if (token.isEthereum())
                {
                    allowThroughFilter = false;
                }
                break;
            case FILTER_CURRENCY:
                if (!token.isEthereum())
                {
                    allowThroughFilter = false;
                }
                break;
            case FILTER_COLLECTIBLES:
                if (!(token.isERC721() || token.isERC721Ticket()))
                {
                    allowThroughFilter = false;
                }
                break;
            default:
                break;
        }

        return allowThroughFilter;
    }

    // This checks to see if the token is likely malformed
    private boolean checkTokenValue(Token token, boolean allowThroughFilter)
    {
        if (!allowThroughFilter) return false; //quick return

        //first check for bad value
        BigDecimal tokenValue = new BigDecimal(token.getScaledBalance());
        if (tokenValue.compareTo(CUTOFF_VALUE) >= 0)
        {
            String name = token.getFullName();
            return name.length() <= 18;
        }
        else
        {
            return true;
        }
    }

    private void populateTokens(Token[] tokens)
    {
        items.beginBatchedUpdates();
//        items.add(total);

        for (Token token : tokens)
        {
            updateToken(token, true);
        }
        items.endBatchedUpdates();
        notifyDataSetChanged();
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

    private void filterAdapterItems()
    {
        if (filterType == FILTER_ALL) return;

        //now filter all the tokens accordingly and refresh display
        List<Token> filterTokens = new ArrayList<>();

        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) si;
                if (tsi.value != null && canDisplayToken(tsi.value))
                {
                    filterTokens.add(tsi.value);
                }
            }
        }

        items.beginBatchedUpdates();
        items.clear();
//        items.add(total);
        for (Token token : filterTokens)
        {
            items.add(new TokenSortedItem(token, token.getNameWeight()));
        }
        items.endBatchedUpdates();
    }

    public void setFilterType(int filterType)
    {
        this.filterType = filterType;
        filterAdapterItems();
    }

    public void clear()
    {
        items.beginBatchedUpdates();
        items.clear();
        items.endBatchedUpdates();
        notifyDataSetChanged();
    }

    private void checkLiveToken(Token t)
    {
        if (t != null) t.checkIsMatchedInXML(assetService);
    }

    public boolean hasBackupWarning()
    {
        return items.size() > 0 && items.get(0).viewType == WarningHolder.VIEW_TYPE;
    }

    public void setScrollToken(ContractLocator importToken)
    {
        scrollToken = importToken;
    }

    public int getScrollPosition()
    {
        if (scrollToken != null)
        {
            for (int i = 0; i < items.size(); i++)
            {
                Object si = items.get(i);
                if (si instanceof TokenSortedItem)
                {
                    TokenSortedItem tsi   = (TokenSortedItem) si;
                    Token           token = tsi.value;
                    if (scrollToken.equals(token))
                    {
                        scrollToken = null;
                        return i;
                    }
                }
            }
        }

        return -1;
    }
}
