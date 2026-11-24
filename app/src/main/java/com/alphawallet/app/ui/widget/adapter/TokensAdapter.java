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
import com.alphawallet.app.ui.widget.holder.TokenProvider;
import com.alphawallet.app.ui.widget.holder.TotalBalanceHolder;
import com.alphawallet.app.ui.widget.holder.WalletConnectSessionHolder;
import com.alphawallet.app.ui.widget.holder.WarningHolder;
import com.alphawallet.token.entity.ViewType;
import com.alphawallet.token.entity.ContractAddress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class TokensAdapter extends RecyclerView.Adapter<BinderViewHolder>
{
    private static final int TOKEN_PAGE_SIZE = 25;
    private static final int TOKEN_CACHE_SIZE = TOKEN_PAGE_SIZE * 2;
    private static final int PREFETCH_THRESHOLD = 7;

    private TokenFilter filterType = TokenFilter.ALL;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private final ActivityResultLauncher<Intent> managementLauncher;
    private ContractLocator scrollToken; // designates a token that should be scrolled to

    private String walletAddress;
    private boolean debugView = false;

    private boolean gridFlag;

    protected final TokensAdapterCallback tokensAdapterCallback;
    private final CompositeDisposable prefetchDisposables = new CompositeDisposable();
    private final Set<String> loadingTokenKeys = new HashSet<>();
    private final TokenWindowCache tokenCache = new TokenWindowCache(TOKEN_CACHE_SIZE);
    private int cacheGeneration;

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
                TokenHolder tokenHolder = new TokenHolder(parent, assetService, tokensService, this::getCachedToken);
                tokenHolder.setOnTokenClickListener(tokensAdapterCallback);
                holder = tokenHolder;
                break;

            case TokenGridHolder.VIEW_TYPE:
                TokenGridHolder tokenGridHolder = new TokenGridHolder(R.layout.item_token_grid, parent, assetService, this::getCachedToken);
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
        if (items.get(position) instanceof TokenSortedItem)
        {
            prefetchForPosition(position);
        }
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

    private Token getCachedToken(long chainId, String address)
    {
        return tokenCache.get(chainId, address);
    }

    private void prefetchForPosition(int adapterPosition)
    {
        int tokenOrdinal = tokenOrdinalAt(adapterPosition);
        if (tokenOrdinal < 0) return;

        int pageStart = (tokenOrdinal / TOKEN_PAGE_SIZE) * TOKEN_PAGE_SIZE;
        loadTokenPage(pageStart);
        if ((tokenOrdinal % TOKEN_PAGE_SIZE) >= TOKEN_PAGE_SIZE - PREFETCH_THRESHOLD)
        {
            loadTokenPage(pageStart + TOKEN_PAGE_SIZE);
        }
    }

    private int tokenOrdinalAt(int adapterPosition)
    {
        int tokenOrdinal = -1;
        for (int i = 0; i <= adapterPosition && i < items.size(); i++)
        {
            if (items.get(i) instanceof TokenSortedItem)
            {
                tokenOrdinal++;
            }
        }
        return tokenOrdinal;
    }

    private void loadTokenPage(int pageStart)
    {
        if (tokensService == null || pageStart < 0) return;

        List<ContractAddress> requests = new ArrayList<>();
        Set<String> requestedKeys = new HashSet<>();
        Set<String> affectedTokenIds = new HashSet<>();
        int tokenOrdinal = 0;

        for (int i = 0; i < items.size() && tokenOrdinal < pageStart + TOKEN_PAGE_SIZE; i++)
        {
            if (!(items.get(i) instanceof TokenSortedItem tokenItem)) continue;

            TokenCardMeta meta = tokenItem.value;
            if (tokenOrdinal >= pageStart && meta.group != TokenGroup.ATTESTATION)
            {
                addPrefetchRequest(meta.getContractAddress(), requestedKeys, requests);
                affectedTokenIds.add(meta.tokenId.toLowerCase(Locale.ROOT));

                // This legacy edge case displays the chain currency instead of a
                // malformed account-as-contract row. Prefetch that currency too.
                if (meta.getNameWeight() < 1000L && !meta.isEthereum())
                {
                    addPrefetchRequest(new ContractAddress(meta.getChain(), "eth"), requestedKeys, requests);
                }
            }
            tokenOrdinal++;
        }

        if (requests.isEmpty()) return;

        final int requestGeneration = cacheGeneration;
        loadingTokenKeys.addAll(requestedKeys);
        prefetchDisposables.add(Single.fromCallable(() -> tokensService.getTokens(requests))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        loaded -> onTokensPrefetched(requestGeneration, requestedKeys, affectedTokenIds, loaded),
                        error -> onTokenPrefetchError(requestGeneration, requestedKeys, error)));
    }

    private void addPrefetchRequest(ContractAddress address, Set<String> requestedKeys,
                                    List<ContractAddress> requests)
    {
        String key = address.getAddressKey().toLowerCase(Locale.ROOT);
        if (!tokenCache.contains(key) && !loadingTokenKeys.contains(key) && requestedKeys.add(key))
        {
            requests.add(address);
        }
    }

    private void onTokensPrefetched(int generation, Set<String> requestedKeys,
                                    Set<String> affectedTokenIds, Map<String, Token> loaded)
    {
        if (generation != cacheGeneration) return;

        loadingTokenKeys.removeAll(requestedKeys);
        tokenCache.putAll(loaded);

        for (int i = 0; i < items.size(); i++)
        {
            if (items.get(i) instanceof TokenSortedItem tokenItem
                    && affectedTokenIds.contains(tokenItem.value.tokenId.toLowerCase(Locale.ROOT)))
            {
                notifyItemChanged(i);
            }
        }
    }

    private void onTokenPrefetchError(int generation, Set<String> requestedKeys, Throwable error)
    {
        if (generation == cacheGeneration)
        {
            loadingTokenKeys.removeAll(requestedKeys);
        }
        Timber.e(error, "Unable to prefetch token window");
    }

    private void invalidateCachedToken(TokenCardMeta token)
    {
        if (token == null) return;
        tokenCache.remove(token);
    }

    private void resetTokenCache()
    {
        cacheGeneration++;
        prefetchDisposables.clear();
        loadingTokenKeys.clear();
        tokenCache.clear();
    }

    public void setWalletAddress(String walletAddress)
    {
        if (this.walletAddress == null || !this.walletAddress.equalsIgnoreCase(walletAddress))
        {
            resetTokenCache();
        }
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
        for (TokenCardMeta token : tokens)
        {
            invalidateCachedToken(token);
        }
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
                    invalidateCachedToken(thisToken);
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
                    invalidateCachedToken(tsi.value);
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
                    invalidateCachedToken(thisToken);
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
                    invalidateCachedToken(thisToken);
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
        if (clear)
        {
            resetTokenCache();
        }
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
        if (clear)
        {
            loadTokenPage(0);
        }
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
        resetTokenCache();
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
        resetTokenCache();
        prefetchDisposables.dispose();
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
                    TokenSortedItem tsi = (TokenSortedItem) si;
                    tsi.setFiatValue(tokensService.getTokenFiatValue(tcm.getChain(), tcm.getAddress()));
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
