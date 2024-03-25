package com.alphawallet.app.ui.widget.adapter;

import android.content.Intent;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.CustomViewSettings;
import com.alphawallet.app.entity.TokenFilter;
import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.entity.walletconnect.WalletConnectSessionItem;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.entity.ChainItem;
import com.alphawallet.app.ui.widget.entity.HeaderItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.ui.widget.entity.ManageTokensSearchItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
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
import com.alphawallet.app.ui.widget.holder.TokenGridHolder;
import com.alphawallet.app.ui.widget.holder.TokenHolder;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WalletConnectSessionHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.token.entity.ViewType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    protected final TokensAdapterCallback tokensAdapterCallback;

    protected final SortedList<SortedItem> items = new SortedList<>(SortedItem.class, new SortedList.Callback<>()
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

    private boolean searchBarAdded;
    private boolean manageTokenLayoutAdded;

    public TokensAdapter(TokensAdapterCallback tokensAdapterCallback, AssetDefinitionService aService, TokensService tService,
                         ActivityResultLauncher<Intent> launcher)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
        this.assetService = aService;
        this.tokensService = tService;
        this.managementLauncher = launcher;
    }

    protected TokensAdapter(TokensAdapterCallback tokensAdapterCallback, AssetDefinitionService aService)
    {
        this.tokensAdapterCallback = tokensAdapterCallback;
        this.assetService = aService;
        this.tokensService = null;
        this.managementLauncher = null;
    }

    @Override
    public long getItemId(int position)
    {
        return items.get(position).hashCode();
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

            case SearchTokensHolder.VIEW_TYPE:
                holder = new SearchTokensHolder(R.layout.layout_manage_token_search, parent, tokensAdapterCallback);
                break;

            case WarningHolder.VIEW_TYPE:
                holder = new WarningHolder(R.layout.item_warning, parent);
                break;

            case WalletConnectSessionHolder.VIEW_TYPE:
                holder = new WalletConnectSessionHolder(R.layout.item_wallet_connect_sessions, parent);
                break;

            case AssetInstanceScriptHolder.VIEW_TYPE:
                holder = new AssetInstanceScriptHolder(R.layout.item_ticket, parent, null, assetService, ViewType.VIEW);
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
        if (walletAddress != null && !walletAddress.isEmpty() && !searchBarAdded)
        {
            items.add(new ManageTokensSearchItem(new ManageTokensData(walletAddress, managementLauncher), -1));
            searchBarAdded = true;
        }
    }

    private void addManageTokensLayout()
    {
        //only show buy button if filtering all or assets
        if (walletAddress != null && !walletAddress.isEmpty()
                && (filterType == TokenFilter.ALL || filterType == TokenFilter.ASSETS) && !manageTokenLayoutAdded)
        {
            items.add(new ManageTokensSortedItem(new ManageTokensData(walletAddress, managementLauncher)));
            manageTokenLayoutAdded = true;
        }
    }

    public void addWarning(WarningData data)
    {
        items.add(new WarningSortedItem(data, 1));
    }

    public void setTokens(TokenCardMeta[] tokens)
    {
        populateTokens(tokens, true);
    }

    public void updateTokenMetas(TokenCardMeta[] tokens)
    {
        populateTokens(tokens, false);
    }

    /**
     * Update a single item in the recycler view
     *
     * @param token
     */
    public void updateToken(TokenCardMeta token)
    {
        if (canDisplayToken(token))
        {
            //does this token already exist with a different weight (ie name has changed)?
            removeMatchingTokenDifferentWeight(token);
            if (gridFlag)
            {
                items.add(new TokenSortedItem(TokenGridHolder.VIEW_TYPE, token, token.getNameWeight()));
                return;
            }

            TokenSortedItem tsi = new TokenSortedItem(TokenHolder.VIEW_TYPE, token, token.getNameWeight());
            tsi.setFiatValue(tokensService.getTokenFiatValue(token.getChain(), token.getAddress()));
            if (debugView) tsi.debug();
            int index = findItem(tsi);
            if (index > -1)
            {
                items.updateItemAt(index, tsi);
            }
            else
            {
                SortedItem<?> headerItem = new HeaderItem(token.group);
                items.add(tsi);
                items.add(headerItem);

                SortedItem<?> chainItem = new ChainItem(token.getChain(), token.group);
                if (doesNotExist(chainItem))
                {
                    items.add(chainItem);
                }
            }
        }
        else
        {
            removeToken(token);
        }
    }

    private boolean doesNotExist(SortedItem<?> token)
    {
        return findItem(token) == -1;
    }

    private int findItem(SortedItem<?> tsi)
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).areItemsTheSame(tsi))
            {
                return i;
            }
        }
        return -1;
    }

    private void removeMatchingTokenDifferentWeight(TokenCardMeta token)
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i) instanceof TokenSortedItem tsi)
            {
                if (tsi.value.equals(token))
                {
                    if (tsi.value.getNameWeight() != token.getNameWeight())
                    {
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
            if (si instanceof TokenSortedItem tsi && tsi.value.tokenId != null)
            {
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.equalsIgnoreCase(token.tokenId))
                {
                    items.remove(tsi);
                    break;
                }
            }
        }
    }

    public SortedItem<TokenCardMeta> removeToken(String removalKey)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem tsi && tsi.value.tokenId != null)
            {
                if (tsi.value.tokenId.toLowerCase(Locale.ROOT).startsWith(removalKey))
                {
                    items.remove(tsi);
                    return tsi;
                }
            }
        }
        return null;
    }

    public SortedItem<TokenCardMeta> removeEntry(String tokenId)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem tsi && tsi.value.tokenId != null)
            {
                TokenCardMeta thisToken = tsi.value;

                if (thisToken.tokenId.equals(tokenId))
                {
                    items.remove(tsi);
                    return tsi;
                }
            }
        }
        return null;
    }

    public SortedItem<TokenCardMeta> removeToken(Token token)
    {
        String tokenKey = token.getDatabaseKey().toLowerCase(Locale.ROOT);
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem tsi && tsi.value.tokenId != null)
            {
                TokenCardMeta thisToken = tsi.value;
                if (thisToken.tokenId.toLowerCase(Locale.ROOT).startsWith(tokenKey))
                {
                    items.remove(tsi);
                    return tsi;
                }
            }
        }
        return null;
    }

    private boolean canDisplayToken(TokenCardMeta token)
    {
        if (token == null || token.balance == null) return false;
        if (token.balance.equals("-2"))
        {
            return false;
        }

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
                allowThroughFilter = allowThroughFilter && token.group == TokenGroup.ATTESTATION;
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
            searchBarAdded = false;
            manageTokenLayoutAdded = false;
        }

        addSearchTokensLayout();

        if (managementLauncher != null) addManageTokensLayout();

        for (TokenCardMeta token : tokens)
        {
            updateToken(token);
        }

        addManageTokensLayout();

        items.endBatchedUpdates();
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
            if (si instanceof TokenSortedItem tsi)
            {
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

    public void clear()
    {
        items.beginBatchedUpdates();
        items.clear();
        items.endBatchedUpdates();
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
                if (si instanceof TokenSortedItem tsi)
                {
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
        checkWalletConnect();
    }

    public void removeItem(int viewType)
    {
        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i).viewType == viewType)
            {
                items.removeItemAt(i);
                break;
            }
        }
    }

    public void addToken(SortedItem<TokenCardMeta> token)
    {
        items.add(token);
    }

    public void checkWalletConnect()
    {
        //activate WC logo in search bar if we have active WC sessions
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof ManageTokensSearchItem manageTokensSearchItem && manageTokensSearchItem.view instanceof SearchTokensHolder sth)
            {
                if (tokensAdapterCallback.hasWCSession())
                {
                    sth.enableWalletConnect();
                }
                else
                {
                    sth.hideWalletConnect();
                }

                break;
            }
        }
    }
}
