package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
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
    private ItemClickListener listener;
    protected final AssetDefinitionService assetService;
    protected final TokensService tokensService;
    private final int POPULAR_TOKEN_DISPLAY_COUNT = 5;

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

        /*
        This list to identify popular tokens
         */
        unknownTokenList = Objects.requireNonNull(readContracts()).getMainNet();

        Log.d("Home","**************");
        for(UnknownToken unknownToken : unknownTokenList)
        {
            Log.d("Home","Address : " + unknownToken.address + " isPopular : " + unknownToken.isPopular);
        }
        Log.d("Home","**************");

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
        int hiddenTokensCount = 0;
        int popularTokensCount = 0;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        items.clear();
        items.beginBatchedUpdates();

        for (TokenCardMeta tokenCardMeta : tokens)
        {
            TokenSortedItem sortedItem = null;
            Token token = tokensService.getToken(tokenCardMeta.getChain(), tokenCardMeta.getAddress());
            tokenCardMeta.isEnabled = token.tokenInfo.isEnabled;

            if (token.tokenInfo.isEnabled)
            {
                sortedItem = new TokenSortedItem(
                        DISPLAY_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            else if(!isContractPopularToken(token.getAddress()))
            {
                hiddenTokensCount++;
                sortedItem = new TokenSortedItem(
                        HIDDEN_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                );
            }
            else
            {
                popularTokensCount++;
                if (forFilter || popularTokensCount <= POPULAR_TOKEN_DISPLAY_COUNT)
                {
                    sortedItem = new TokenSortedItem(
                            POPULAR_TOKEN, tokenCardMeta, tokenCardMeta.nameWeight
                    );
                }
            }

            if (sortedItem != null)
            {
                items.add(sortedItem);
            }
        }

        if (!forFilter)
        {
            TokenCardMeta tcmZero = new TokenCardMeta(0, "", context.getString(R.string.zero_balance_tokens_off), 0, 0, null);
            tcmZero.isEnabled = pref.getBoolean(HIDE_ZERO_BALANCE_TOKENS, false);
            items.add(new TokenSortedItem(
                    SHOW_ZERO_BALANCE,
                    tcmZero, 0));
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

        //if there are no popular tokens found no need to display label
        if (popularTokensCount > 0)
        {
            items.add(new ManageTokensLabelSortedItem(
                    LABEL_POPULAR_TOKEN,
                    new ManageTokensLabelData(context.getString(R.string.popular_tokens)),
                    0));
        }

        items.endBatchedUpdates();
    }

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

            jsonString = new String(buffer, "UTF-8");
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
                Log.d("Home","Popular Address : " + address);
                return unknownToken.isPopular;
            }
        }
        Log.d("Home","Not Popular Address : " + address);
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

        if (position == 0)
        {
            handleShowHideEmptyTokens(isChecked);
            return;
        }

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

    private void handleShowHideEmptyTokens(boolean isChecked)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(HIDE_ZERO_BALANCE_TOKENS, isChecked).apply();
    }

    public void filter(String searchString)
    {
        tokensService.getAllTokenMetas(searchString)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(metas -> updateList(metas, searchString), error -> { })
                .isDisposed();
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

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }
}
