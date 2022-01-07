package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.KnownContract;
import com.alphawallet.app.entity.TokenManageType;
import com.alphawallet.app.entity.UnknownToken;
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
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.alphawallet.app.entity.TokenManageType.DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.HIDDEN_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_HIDDEN_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_POPULAR_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.POPULAR_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.SHOW_ZERO_BALANCE;
import static com.alphawallet.app.repository.SharedPreferenceRepository.HIDE_ZERO_BALANCE_TOKENS;

public class TokenListAdapter extends RecyclerView.Adapter<BinderViewHolder> implements OnTokenManageClickListener {

    private final Context context;
    private final List<UnknownToken> unknownTokenList;
    private final ItemClickListener listener;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private Disposable disposable;

    int hiddenTokensCount = 0;
    int popularTokensCount = 0;

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
                return o1.compare(o2);
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

        /*
        This list to identify popular tokens
         */
        unknownTokenList = Objects.requireNonNull(readContracts()).getMainNet();

        setupList(tokenList, false);
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

    private void setupList(List<TokenCardMeta> tokens, boolean forFilter)
    {
        hiddenTokensCount = 0;
        popularTokensCount = 0;

        items.clear();
        items.beginBatchedUpdates();

        for (TokenCardMeta tokenCardMeta : tokens)
        {
            TokenSortedItem sortedItem = null;
            if (tokenCardMeta.isEthereum()) continue; //no chain cards
            Token token = tokensService.getToken(tokenCardMeta.getChain(), tokenCardMeta.getAddress());
            tokenCardMeta.isEnabled = token.tokenInfo.isEnabled;

            if (token.tokenInfo.isEnabled)
            {
                sortedItem = new TokenSortedItem(
                        DISPLAY_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
                );
            }
            else if(!isContractPopularToken(token.getAddress()))
            {
                hiddenTokensCount++;
                sortedItem = new TokenSortedItem(
                        HIDDEN_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
                );
            }
            else
            {
                popularTokensCount++;
                sortedItem = new TokenSortedItem(
                        POPULAR_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
                );
            }

            items.add(sortedItem);
        }

        items.add(new ManageTokensLabelSortedItem(
                LABEL_DISPLAY_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.display_tokens)),
                0));

        addHiddenTokenLabel();
        addPopularTokenLabel();

        items.endBatchedUpdates();
    }

    private void addHiddenTokenLabel()
    {
        //if there are no hidden tokens found no need to display label
        if (hiddenTokensCount > 0)
        {
            items.add(new ManageTokensLabelSortedItem(
                    LABEL_HIDDEN_TOKEN,
                    new ManageTokensLabelData(context.getString(R.string.hidden_tokens)),
                    0));
        }
    }

    private void addPopularTokenLabel()
    {
        //if there are no popular tokens found no need to display label
        if (popularTokensCount > 0)
        {
            items.add(new ManageTokensLabelSortedItem(
                    LABEL_POPULAR_TOKEN,
                    new ManageTokensLabelData(context.getString(R.string.popular_tokens)),
                    0));
        }
    }

    //TODO: Deduplicate
    private KnownContract readContracts()
    {
        String jsonString;
        try
        {
            InputStream is = context.getAssets().open("known_contract.json");

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return null;
        }

        return new Gson().fromJson(jsonString, KnownContract.class);
    }

    public boolean isContractPopularToken(String address) {
        for (UnknownToken unknownToken: unknownTokenList)
        {
            if(unknownToken.address.equalsIgnoreCase(address))
            {
                return unknownToken.isPopular;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public BinderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, @TokenManageType.ManageType int viewType) {
        switch (viewType) {
            case SHOW_ZERO_BALANCE:
                TokenListHolder showZeros = new TokenListHolder(R.layout.item_manage_token, viewGroup, assetService, tokensService);
                showZeros.setOnTokenClickListener(this);
                return showZeros;
            case LABEL_DISPLAY_TOKEN:
            case LABEL_HIDDEN_TOKEN:
            case LABEL_POPULAR_TOKEN:
                return new TokenLabelViewHolder(R.layout.layout_manage_tokens_label, viewGroup);
            case DISPLAY_TOKEN:
            case HIDDEN_TOKEN:
            case POPULAR_TOKEN:
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

//        if (position == 0)
//        {
//            handleShowHideEmptyTokens(isChecked);
//            return;
//        }

        TokenCardMeta tcm = (TokenCardMeta)items.get(position).value;

        TokenSortedItem updateItem = new TokenSortedItem(
                DISPLAY_TOKEN, tcm, tcm.getNameWeight()
        );

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

        //need to allow for token click to keep track of quantities
        updateLabels();

        items.endBatchedUpdates();

        notifyDataSetChanged();

        listener.onItemClick(token, isChecked);
    }

    private void updateLabels()
    {
        hiddenTokensCount = 0;
        popularTokensCount = 0;

        int popularTokenLabelIndex = -1;
        int hiddenTokenLabelIndex = -1;

        for (int i = 0; i < items.size(); i++)
        {
            switch (items.get(i).viewType)
            {
                case HIDDEN_TOKEN:
                    hiddenTokensCount++;
                    break;
                case POPULAR_TOKEN:
                    popularTokensCount++;
                    break;
                case LABEL_HIDDEN_TOKEN:
                    hiddenTokenLabelIndex = i;
                    break;
                case LABEL_POPULAR_TOKEN:
                    popularTokenLabelIndex = i;
                    break;
                default:
                    break;
            }
        }

        if (hiddenTokensCount == 0 && hiddenTokenLabelIndex >= 0)
        {
            items.removeItemAt(hiddenTokenLabelIndex);
        }
        if (popularTokensCount == 0 && popularTokenLabelIndex >= 0)
        {
            items.removeItemAt(popularTokenLabelIndex);
        }
    }

    private void handleShowHideEmptyTokens(boolean isChecked)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(HIDE_ZERO_BALANCE_TOKENS, isChecked).apply();
    }

    public void filter(String searchString)
    {
        disposable = tokensService.getAllTokenMetas(searchString)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(metas -> updateList(metas, searchString), error -> { });
    }

    private void updateList(TokenCardMeta[] metas, String searchString)
    {
        /*
        While setting up, when searchString is Empty, it is hard to identify whether it is from filter call or not.
        So when searchString is empty, consider it as a regular update.
         */
        setupList(Arrays.asList(metas), !searchString.isEmpty());
        notifyDataSetChanged();
    }

    public interface ItemClickListener {
        void onItemClick(Token token, boolean enabled);
    }

    public void onDestroy()
    {
        if (disposable != null && !disposable.isDisposed())
        {
            disposable.dispose();
            disposable = null;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    public boolean isTokenPresent(String tokenAddress)
    {
        for (int i = 0; i < items.size(); i++)
        {
            Object si = items.get(i);
            if (si instanceof TokenSortedItem)
            {
                TokenSortedItem tsi = (TokenSortedItem) si;
                TokenCardMeta thisToken = tsi.value;

                if (thisToken.getAddress().equalsIgnoreCase(tokenAddress))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void addToken(TokenCardMeta tokenCardMeta)
    {
        Token token = tokensService.getToken(tokenCardMeta.getChain(), tokenCardMeta.getAddress());
        if (token == null) return;

        tokenCardMeta.isEnabled = token.tokenInfo.isEnabled;
        TokenSortedItem sortedItem = null;

        if (token.tokenInfo.isEnabled)
        {
            sortedItem = new TokenSortedItem(
                    DISPLAY_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
            );
        }
        else if (!isContractPopularToken(token.getAddress()))
        {
            hiddenTokensCount++;
            sortedItem = new TokenSortedItem(
                    HIDDEN_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
            );
        }
        else
        {
            popularTokensCount++;
            sortedItem = new TokenSortedItem(
                    POPULAR_TOKEN, tokenCardMeta, tokenCardMeta.getNameWeight()
            );
        }

        items.beginBatchedUpdates();
        items.add(sortedItem);

        addHiddenTokenLabel();
        addPopularTokenLabel();

        items.endBatchedUpdates();
        notifyDataSetChanged();
    }
}