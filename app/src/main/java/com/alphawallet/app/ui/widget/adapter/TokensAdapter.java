package com.alphawallet.app.ui.widget.adapter;

import android.content.Intent;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.TokenFilter;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.repository.TokensMappingRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.entity.ChainItem;
import com.alphawallet.app.ui.widget.entity.HeaderItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.ui.widget.entity.ManageTokensSearchItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TestNetTipsItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.ui.widget.entity.TotalBalanceSortedItem;
import com.alphawallet.app.ui.widget.entity.WalletConnectSessionSortedItem;
import com.alphawallet.app.ui.widget.entity.WarningData;
import com.alphawallet.app.ui.widget.entity.WarningSortedItem;
import com.alphawallet.app.ui.widget.holder.AssetInstanceScriptHolder;
import com.alphawallet.app.ui.widget.holder.BinderViewHolder;
import com.alphawallet.app.ui.widget.holder.ChainNameHeaderHolder;
import com.alphawallet.app.ui.widget.holder.HeaderHolder;
import com.alphawallet.app.ui.widget.holder.ManageTokensHolder;
import com.alphawallet.app.ui.widget.holder.SearchTokensHolder;
import com.alphawallet.app.ui.widget.holder.TestNetTipsHolder;
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WalletConnectSessionHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder>
{
    private static final String TAG = "TKNADAPTER";

    private TokenFilter filterType = TokenFilter.ALL;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private final ActivityResultLauncher<Intent> managementLauncher;
    private ContractLocator scrollToken; // designates a token that should be scrolled to

    private String walletAddress;
    private boolean debugView = false;

    private boolean gridFlag;
    private boolean showTestNetTips = false;

    protected final TokensAdapterCallback tokensAdapterCallback;
    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<SortedItem>()
    {
        @Override
        public int compare(SortedItem o1, SortedItem o2)
        {
            return o1.compare(o2);
        }

        @Override
        public void onChanged(int position, int count)
        {
            notifyItemRangeChanged(position, count);
        }

        @Override
        public boolean areContentsTheSame(SortedItem oldItem, SortedItem newItem)
        {
            return oldItem.areContentsTheSame(newItem);
        }

        @Override
        public boolean areItemsTheSame(SortedItem item1, SortedItem item2)
        {
            return item1.areItemsTheSame(item2);
        }

        @Override
        public void onInserted(int position, int count)
        {
            notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count)
        {
            notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition)
        {
            notifyItemMoved(fromPosition, toPosition);
        }
    });

    protected TotalBalanceSortedItem total = new TotalBalanceSortedItem(null);


    public TokensAdapter(TokensAdapterCallback tokensAdapterCallback, AssetDefinitionService aService, TokensService tService,
                         ActivityResultLauncher<Intent> launcher)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
        this.assetService = aService;
        this.tokensService = tService;
        this.managementLauncher = launcher;

        new TokensMappingRepository(aService.getTokenLocalSource());
    }

    protected TokensAdapter(TokensAdapterCallback tokensAdapterCallback, AssetDefinitionService aService)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
        this.assetService = aService;
        this.tokensService = null;

        new TokensMappingRepository(aService.getTokenLocalSource());
        this.managementLauncher = null;
    }

    @Override
    public long getItemId(int position)
    {
        Object obj = items.get(position);
        if (obj instanceof TokenSortedItem)
        {
            TokenCardMeta tcm = ((TokenSortedItem) obj).value;

            // This is an attempt to obtain a 'unique' id
            // to fully utilise the RecyclerView's setHasStableIds feature.
            // This will drastically reduce 'blinking' when the list changes
            return tcm.getUID();
        }
        else
        {
            return position;
        }
    }

    @NonNull
    @Override
    public BinderViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        BinderViewHolder<?> holder;
        switch (viewType)
        {
            case TokenHolder.VIEW_TYPE:
                TokenHolder tokenHolder = new TokenHolder(parent, assetService, tokensService);
                tokenHolder.setOnTokenClickListener(tokensAdapterCallback);
                holder = tokenHolder;
                break;

            case TokenGridHolder.VIEW_TYPE:
                TokenGridHolder tokenGridHolder = new TokenGridHolder(R.layout.item_token_grid, parent, assetService, tokensService);
                tokenGridHolder.setOnTokenClickListener(tokensAdapterCallback);
                holder = tokenGridHolder;
                break;

            case ManageTokensHolder.VIEW_TYPE:
                ManageTokensHolder manageTokensHolder = new ManageTokensHolder(R.layout.layout_manage_tokens_with_buy, parent);
                manageTokensHolder.setOnTokenClickListener(tokensAdapterCallback);
                holder = manageTokensHolder;
                break;

            case HeaderHolder.VIEW_TYPE:
                holder = new HeaderHolder(R.layout.layout_tokens_header, parent);
                break;

            case TestNetTipsHolder.VIEW_TYPE:
                holder = new TestNetTipsHolder(R.layout.layout_testnet_header, parent);
                holder.setOnTokenClickListener(tokensAdapterCallback);
                break;

            case SearchTokensHolder.VIEW_TYPE:
                holder = new SearchTokensHolder(R.layout.layout_manage_token_search, parent, tokensAdapterCallback::onSearchClicked);
                break;

            case WarningHolder.VIEW_TYPE:
                holder = new WarningHolder(R.layout.item_warning, parent);
                break;

            case WalletConnectSessionHolder.VIEW_TYPE:
                holder = new WalletConnectSessionHolder(R.layout.item_wallet_connect_sessions, parent);
                break;

            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, null, assetService, false);
                break;

            case ChainNameHeaderHolder.VIEW_TYPE:
                holder = new ChainNameHeaderHolder(R.layout.item_chainname_header, parent);
                break;

            // NB to save ppl a lot of effort this view doesn't show - item_total_balance has height coded to 1dp.
            case TotalBalanceHolder.VIEW_TYPE:
            default:
                holder = new TotalBalanceHolder(R.layout.item_total_balance, parent);
                break;
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(BinderViewHolder holder, int position)
    {
        items.get(position).view = holder;
        holder.bind(items.get(position).value);
    }

    public void onRViewRecycled(RecyclerView.ViewHolder holder)
    {
        ((BinderViewHolder<?>) holder).onDestroyView();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull BinderViewHolder holder)
    {
        holder.onDestroyView();
    }

    @Override
    public int getItemViewType(int position)
    {
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
    public int getItemCount()
    {
        return items.size();
    }

    public void setWalletAddress(String walletAddress)
    {
        this.walletAddress = walletAddress;
    }

    private void addSearchTokensLayout()
    {
        if (walletAddress != null && !walletAddress.isEmpty())
        {
            items.add(new ManageTokensSearchItem(new ManageTokensData(walletAddress, managementLauncher), -1));
        }
    }

    //Only show the header if the item type is added to the list
    private void addHeaderLayout(TokenCardMeta tcm)
    {
        items.add(new HeaderItem(tcm.group));
        items.add(new ChainItem(tcm.getChain(), tcm.group));
    }

    private void addManageTokensLayout()
    {
        if (walletAddress != null && !walletAddress.isEmpty() && tokensService.isMainNetActive()
                && (filterType == TokenFilter.ALL || filterType == TokenFilter.ASSETS))
        { //only show buy button if filtering all or assets
            items.add(new ManageTokensSortedItem(new ManageTokensData(walletAddress, managementLauncher)));
        }
    }

    public void addWarning(WarningData data)
    {
        items.add(new WarningSortedItem(data, 1));
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
                position = items.add(new TokenSortedItem(TokenGridHolder.VIEW_TYPE, token, token.getNameWeight()));
            }
            else
            {
                TokenSortedItem tsi = new TokenSortedItem(TokenHolder.VIEW_TYPE, token, token.getNameWeight());
                //get fiat value
                tsi.setFiatValue(tokensService.getTokenFiatValue(token.getChain(), token.getAddress()));
                if (debugView) tsi.debug();
                position = items.add(tsi);
                addHeaderLayout(token);
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
                    if (tsi.value.getNameWeight() != token.getNameWeight())
                    {
                        notifyItemChanged(i);
                        items.removeItemAt(i);
                        break;
                    }
                }
            }
        }
    }

    public void removeToken(TokenCardMeta token)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) si;
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.equalsIgnoreCase(token.tokenId))
                {
                    items.removeItemAt(i);
                    break;
                }
            }
        }
    }

    public void removeToken(long chainId, String tokenAddress)
    {
        String id = TokensRealmSource.databaseKey(chainId, tokenAddress);
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) si;
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.equalsIgnoreCase(id))
                {
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
        boolean allowThroughFilter = CustomViewSettings.tokenCanBeDisplayed(token);
        allowThroughFilter = checkTokenValue(token, allowThroughFilter);

        switch (filterType)
        {
            case ALL:
                // Show all
                // if (token.isNFT()) allowThroughFilter = false;
                break;
            case ASSETS:
                allowThroughFilter = allowThroughFilter && token.group == TokenGroup.ASSET;
                break;
            case DEFI:
                allowThroughFilter = allowThroughFilter && token.group == TokenGroup.DEFI;
                break;
            case GOVERNANCE:
                allowThroughFilter = allowThroughFilter && token.group == TokenGroup.GOVERNANCE;
                break;
            case COLLECTIBLES:
                allowThroughFilter = allowThroughFilter && token.isNFT();
                break;
            case ATTESTATIONS:
                //TODO
                break;
            case NO_FILTER:
                allowThroughFilter = true;
                break;
        }

        return allowThroughFilter;
    }

    // This checks to see if the token is likely malformed
    private boolean checkTokenValue(TokenCardMeta token, boolean allowThroughFilter)
    {
        return allowThroughFilter && token.getNameWeight() < Long.MAX_VALUE;
    }

    private void populateTokens(TokenCardMeta[] tokens, boolean clear)
    {
        items.beginBatchedUpdates();
        if (clear)
        {
            items.clear();
        }

        addSearchTokensLayout();
        addTestNetTips();

        if (managementLauncher != null) addManageTokensLayout();

        for (TokenCardMeta token : tokens)
        {
            updateToken(token, false);
        }

        addManageTokensLayout();

        items.endBatchedUpdates();
    }

    private void addTestNetTips()
    {
        if (!tokensService.isMainNetActive() && !showTestNetTips)
            items.add(new TestNetTipsItem(0));
    }

    public void setTotal(BigDecimal totalInCurrency)
    {
        total = new TotalBalanceSortedItem(totalInCurrency);
        //see if we need an update
        items.beginBatchedUpdates();
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TotalBalanceSortedItem)
            {
                items.remove((TotalBalanceSortedItem) si);
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

    public void setFilterType(TokenFilter filterType)
    {
        this.filterType = filterType;
        gridFlag = filterType == TokenFilter.COLLECTIBLES;
        filterAdapterItems();
    }

    public void showTestNetTips()
    {
        this.showTestNetTips = true;
        notifyDataSetChanged();
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
                    TokenSortedItem tsi = (TokenSortedItem) si;
                    TokenCardMeta token = tsi.value;
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

    }

    public void setDebug()
    {
        debugView = true;
    }

    public void notifyTickerUpdate(List<String> updatedContracts)
    {
        //check through tokens; refresh relevant tickers
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenCardMeta tcm = ((TokenSortedItem) si).value;
                if (updatedContracts.contains(tcm.getAddress()))
                {
                    notifyItemChanged(i); //optimise update - no need to update elements without tickers
                }
            }
        }
    }

    public List<TokenCardMeta> getSelected()
    {
        List<TokenCardMeta> selected = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenCardMeta tcm = ((TokenSortedItem) si).value;
                if (tcm.isEnabled) selected.add(tcm);
            }
        }

        return selected;
    }

    public void showActiveWalletConnectSessions(List<WalletConnectSessionItem> sessions)
    {
        if (sessions.isEmpty())
        {
            removeItem(WalletConnectSessionHolder.VIEW_TYPE);
        }
        else
        {
            items.add(new WalletConnectSessionSortedItem(sessions, 2));
        }
    }

    public void removeItem(int viewType)
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == viewType)
            {
                items.removeItemAt(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
}
