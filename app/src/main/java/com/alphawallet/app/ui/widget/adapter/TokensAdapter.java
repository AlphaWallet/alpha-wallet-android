package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnTokenClickListener;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.ui.widget.entity.ManageTokensSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TotalBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.entity.WarningSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder> {
    private static final String TAG = "TKNADAPTER";
    public static final int FILTER_ALL = 0;
    public static final int FILTER_CURRENCY = 1;
    public static final int FILTER_ASSETS = 2;
    public static final int FILTER_COLLECTIBLES = 3;
    private static final BigDecimal CUTOFF_VALUE = BigDecimal.valueOf(99999999999L);
    private final Realm realm;

    private int filterType;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private ContractLocator scrollToken; // designates a token that should be scrolled to

    private Context context;
    private String walletAddress;
    private boolean debugView = false;

    private boolean gridFlag;

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
        this.realm = tokensService.getTickerRealmInstance();
    }

    protected TokensAdapter(OnTokenClickListener onTokenClickListener, AssetDefinitionService aService) {
        this.onTokenClickListener = onTokenClickListener;
        this.assetService = aService;
        this.tokensService = null;
        this.realm = null;
    }

    @Override
    public long getItemId(int position) {
        Object obj = items.get(position);
        if (obj instanceof TokenSortedItem) {
            TokenCardMeta tcm = ((TokenSortedItem) obj).value;

             // This is an attempt to obtain a 'unique' id
             // to fully utilise the RecyclerView's setHasStableIds feature.
             // This will drastically reduce 'blinking' when the list changes
            return tcm.getUID();
        } else {
            return position;
        }
    }

    @Override
    public BinderViewHolder<?> onCreateViewHolder(ViewGroup parent, int viewType) {
        BinderViewHolder<?> holder = null;
        switch (viewType) {
            case TokenHolder.VIEW_TYPE: {
                TokenHolder tokenHolder = new TokenHolder(parent, assetService, tokensService, realm);
                tokenHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenHolder;
                break;
            }
            case TokenGridHolder.VIEW_TYPE: {
                TokenGridHolder tokenGridHolder = new TokenGridHolder(R.layout.item_token_grid, parent, assetService, tokensService);
                tokenGridHolder.setOnTokenClickListener(onTokenClickListener);
                holder = tokenGridHolder;
                break;
            }
            case ManageTokensHolder.VIEW_TYPE:
                holder = new ManageTokensHolder(R.layout.layout_manage_tokens, parent);
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

    public void onRViewRecycled(RecyclerView.ViewHolder holder)
    {
        ((BinderViewHolder<?>)holder).onDestroyView();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull BinderViewHolder holder)
    {
        holder.onDestroyView();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < items.size())
        {
            return items.get(position).viewType;
        }
        else
        {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    private void addManageTokensLayout() {
        if (walletAddress != null && !walletAddress.isEmpty()) {
            items.add(new ManageTokensSortedItem(new ManageTokensData(walletAddress), 0));
        }
    }

    public void addWarning(WarningData data)
    {
        items.add(new WarningSortedItem(data, 1));
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

    public void setTokens(TokenCardMeta[] tokens)
    {
        populateTokens(tokens, false);
    }

    /**
     * Update a single item in the recycler view
     *
     * @param token
     */
    public void updateToken(TokenCardMeta token, boolean notify)
    {
        if (canDisplayToken(token))
        {
            //does this token already exist with a different weight (ie name has changed)?
            removeMatchingTokenDifferentWeight(token);
            int position = -1;
            if (gridFlag)
            {
                position = items.add(new TokenSortedItem(TokenGridHolder.VIEW_TYPE, token, token.nameWeight));
            }
            else
            {
                TokenSortedItem tsi = new TokenSortedItem(TokenHolder.VIEW_TYPE, token, token.nameWeight);
                if (debugView) tsi.debug();
                position = items.add(tsi);
            }

            if (notify) notifyItemChanged(position);
        }
        else
        {
            removeToken(token);
        }
    }

    private void removeMatchingTokenDifferentWeight(TokenCardMeta token)
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i) instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) items.get(i);
                if (tsi.value.equals(token))
                {
                    if (tsi.value.nameWeight != token.nameWeight)
                    {
                        notifyItemChanged(i);
                        items.removeItemAt(i);
                        break;
                    }
                }
            }
        }
    }

    private TokenCardMeta getToken(int chainId, String tokenAddress)
    {
        String id = TokensRealmSource.databaseKey(chainId, tokenAddress);
        for (int i = 0; i < items.size(); i++) {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem) {
                TokenSortedItem tsi = (TokenSortedItem) si;
                if (tsi.value.tokenId.equalsIgnoreCase(id)) {
                    return tsi.value;
                }
            }
        }

        return null;
    }

    public void removeToken(TokenCardMeta token) {
        for (int i = 0; i < items.size(); i++) {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem) {
                TokenSortedItem tsi = (TokenSortedItem) si;
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.equalsIgnoreCase(token.tokenId)) {
                    items.removeItemAt(i);
                    break;
                }
            }
        }
    }

    public void removeToken(int chainId, String tokenAddress) {
        String id = TokensRealmSource.databaseKey(chainId, tokenAddress);
        for (int i = 0; i < items.size(); i++) {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem) {
                TokenSortedItem tsi = (TokenSortedItem) si;
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.equalsIgnoreCase(id)) {
                    items.removeItemAt(i);
                    break;
                }
            }
        }
    }

    private boolean canDisplayToken(TokenCardMeta token)
    {
        if (token == null) return false;
        //Add token to display list if it's the base currency, or if it has balance
        boolean allowThroughFilter = CustomViewSettings.tokenCanBeDisplayed(token.type, token.balance, token.getChain(), token.getAddress());
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
                if (!(token.isNFT()))
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
    private boolean checkTokenValue(TokenCardMeta token, boolean allowThroughFilter)
    {
        return allowThroughFilter && token.nameWeight < Integer.MAX_VALUE;
    }

    private void populateTokens(TokenCardMeta[] tokens, boolean clear)
    {
        items.beginBatchedUpdates();
        if (clear) {
            items.clear();
        }
        addManageTokensLayout();
        for (TokenCardMeta token : tokens)
        {
            updateToken(token, false);
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

    private void filterAdapterItems()
    {
        //now filter all the tokens accordingly and refresh display
        List<TokenCardMeta> filterTokens = new ArrayList<>();

        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) si;
                if (canDisplayToken(tsi.value))
                {
                    filterTokens.add(tsi.value);
                }
            }
        }

        populateTokens(filterTokens.toArray(new TokenCardMeta[0]), true);
    }

    public void setFilterType(int filterType)
    {
        this.filterType = filterType;
        gridFlag = filterType == FILTER_COLLECTIBLES;
        filterAdapterItems();
    }

    public void clear()
    {
        items.beginBatchedUpdates();
        items.clear();
        items.endBatchedUpdates();

        notifyDataSetChanged();
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
                    TokenCardMeta   token = tsi.value;
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

    public void onDestroy(RecyclerView recyclerView)
    {
        //ensure all holders have their realm listeners cleaned up
        if (recyclerView != null)
        {
            for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i)
            {
                ((BinderViewHolder<?>)recyclerView.getChildViewHolder(recyclerView.getChildAt(i))).onDestroyView();
            }
        }

        if (realm != null) realm.close();
    }

    public void setDebug()
    {
        debugView = true;
    }
}
