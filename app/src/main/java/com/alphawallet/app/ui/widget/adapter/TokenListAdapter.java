package com.alphawallet.app.ui.widget.adapter;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelData;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelSortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TokenListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private LayoutInflater inflater;
    private Context context;
    private ItemClickListener listener;
    private final List<Token> tokens;
    private ArrayList<Object> filteredData = new ArrayList<>();
    protected final AssetDefinitionService assetService;

    public TokenListAdapter(Context context, AssetDefinitionService aService, Token[] tokens, ItemClickListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.assetService = aService;
        this.tokens = Arrays.asList(tokens);
        this.filteredData.addAll(filterTokens(Arrays.asList(tokens), true));
        this.listener = listener;
    }

    private List<Object> filterTokens(List<Token> tokens, boolean performSorting) {
        ArrayList<TokenSortedItem> displayTokensList = new ArrayList<>();
        ArrayList<TokenSortedItem> hiddenTokensList = new ArrayList<>();

        for (Token token : tokens)
        {
            if (token.tokenInfo.name != null)
            {
                if (!token.isEthereum() && !displayTokensList.contains(token) && !hiddenTokensList.contains(token))
                {
                    if (token.tokenInfo.isEnabled)
                    {
                        displayTokensList.add(new TokenSortedItem(TokenViewHolder.VIEW_TYPE, token, token.getNameWeight()));
                    }
                    else
                    {
                        hiddenTokensList.add(new TokenSortedItem(TokenViewHolder.VIEW_TYPE, token, token.getNameWeight()));
                    }
                }
            }
        }

        if (performSorting)
        {
            return sortAndRefreshList(displayTokensList, hiddenTokensList);
        }
        else
        {
            ArrayList<Object> tokensList = new ArrayList<>();
            if (displayTokensList.size() > 0)
            {
                tokensList.addAll(displayTokensList);
            }
            if (hiddenTokensList.size() > 0)
            {
                tokensList.addAll(hiddenTokensList);
            }
            return tokensList;
        }
    }

    private ArrayList<Object> sortAndRefreshList(ArrayList<TokenSortedItem> displayTokensList,
                                                 ArrayList<TokenSortedItem> hiddenTokensList) {
        ArrayList<Object> tokensList = new ArrayList<>();

        if (displayTokensList.size() > 0)
        {
            addDisplayTokensLayout(tokensList);
            //Sort the list based on the weight
            sortElements(displayTokensList);
            tokensList.addAll(displayTokensList);
        }
        if (hiddenTokensList.size() > 0)
        {
            addHiddenTokensLayout(tokensList);
            sortElements(hiddenTokensList);
            tokensList.addAll(hiddenTokensList);
        }

        return tokensList;
    }

    private void sortElements(List<TokenSortedItem> elementList)
    {
        Collections.sort(elementList, (e1, e2) -> {
            long w1 = e1.weight;
            long w2 = e2.weight;
            return Long.compare(w1, w2);
        });
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        switch (viewType)
        {
            case LabelViewHolder.VIEW_TYPE:
                View view = inflater.inflate(R.layout.layout_manage_tokens_label, viewGroup, false);
                return new LabelViewHolder(view);
            case TokenViewHolder.VIEW_TYPE:
            default:
                view = inflater.inflate(R.layout.item_manage_token, viewGroup, false);
                return new TokenViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == TokenViewHolder.VIEW_TYPE)
        {
            TokenSortedItem token = (TokenSortedItem) filteredData.get(position);
            TokenViewHolder tokenViewHolder = (TokenViewHolder) viewHolder;
            viewHolder.setIsRecyclable(false);
            tokenViewHolder.tokenName.setText(token.value.getFullName());
            tokenViewHolder.switchEnabled.setChecked(token.value.tokenInfo.isEnabled);
            tokenViewHolder.switchEnabled.setTag(new Integer(position));
            tokenViewHolder.switchEnabled.setOnCheckedChangeListener((v, b) ->
            {
                listener.onItemClick(((TokenSortedItem) filteredData.get(position)).value, b);
                filteredData.clear();
                filteredData.addAll(filterTokens(tokens, true));
                notifyDataSetChanged();
            });
            tokenViewHolder.textIcon.setBackgroundTintList(ContextCompat.getColorStateList(context, Utils.getChainColour(token.value.tokenInfo.chainId)));
            displayTokenIcon(tokenViewHolder.viewTarget, token.value);
        }
        else if (getItemViewType(position) == LabelViewHolder.VIEW_TYPE)
        {
            ManageTokensLabelSortedItem labelData = (ManageTokensLabelSortedItem) filteredData.get(position);
            LabelViewHolder labelViewHolder = (LabelViewHolder) viewHolder;
            labelViewHolder.textTitle.setText(labelData.value.title);
        }
    }

    private void displayTokenIcon(CustomViewTarget viewTarget, Token token)
    {
        int chainIcon = EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId);

        IconItem iconItem = assetService.fetchIconForToken(token);
        String tokenName = token.getFullName(assetService, token.getTicketCount());
        viewTarget.getView().setTag(tokenName);

        Glide.with(context.getApplicationContext())
                .load(iconItem.getUrl())
                .signature(iconItem.getSignature())
                .onlyRetrieveFromCache(iconItem.isFetchFromCache()) //reduce URL checking, only check once per session
                .apply(new RequestOptions().circleCrop())
                .apply(new RequestOptions().placeholder(chainIcon))
                .dontAnimate()
                .into(viewTarget);
    }

    @Override
    public int getItemCount() {
        return filteredData.size();
    }

    public class TokenViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        RelativeLayout layout;
        ImageView icon;
        TextView tokenName;
        TextView textIcon;
        Switch switchEnabled;
        static final int VIEW_TYPE = 3000;
        private final CustomViewTarget viewTarget;

        TokenViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout_list_item);
            icon = itemView.findViewById(R.id.icon);
            tokenName = itemView.findViewById(R.id.name);
            textIcon = itemView.findViewById(R.id.text_icon);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            layout.setOnClickListener(this);

            viewTarget = new CustomViewTarget<ImageView, BitmapDrawable>(icon) {
                @Override
                protected void onResourceCleared(@Nullable Drawable placeholder) { }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable)
                {
                    setupTextIcon();
                }

                @Override
                public void onResourceReady(@NotNull BitmapDrawable bitmap, Transition<? super BitmapDrawable> transition)
                {
                    textIcon.setVisibility(View.GONE);
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(bitmap);
                }
            };
        }

        private void setupTextIcon()
        {
            icon.setVisibility(View.INVISIBLE);
            textIcon.setVisibility(View.VISIBLE);
            textIcon.setText(viewTarget.getView().getTag().toString());
        }


        @Override
        public void onClick(View v) {
            switchEnabled.setChecked(!switchEnabled.isChecked());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            listener.onItemClick((Token) filteredData.get(getAdapterPosition()), isChecked);
        }
    }

    public class LabelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textTitle;
        TextView textIgnore;
        public static final int VIEW_TYPE = 3001;

        LabelViewHolder(View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textIgnore = itemView.findViewById(R.id.text_ignore);
            textIgnore.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //TODO Implement Ignore click
        }
    }

    public interface ItemClickListener {
        void onItemClick(Token token, boolean enabled);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < filteredData.size())
        {
            if (filteredData.get(position) instanceof ManageTokensLabelSortedItem)
            {
                return LabelViewHolder.VIEW_TYPE;
            }
            else
            {
                return TokenViewHolder.VIEW_TYPE;
            }
        }
        else
        {
            return 0;
        }
    }

    private void addDisplayTokensLayout(ArrayList<Object> filteredList) {
        filteredList.add(new ManageTokensLabelSortedItem(new ManageTokensLabelData(context.getString(R.string.display_tokens)),0));
    }

    private void addHiddenTokensLayout(ArrayList<Object> filteredList) {
        filteredList.add(new ManageTokensLabelSortedItem(new ManageTokensLabelData(context.getString(R.string.hidden_tokens)), 1));
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty())
                {
                    filteredData.clear();
                    filteredData.addAll(filterTokens(tokens, true));
                }
                else
                {
                    ArrayList<Token> filteredList = new ArrayList<>();
                    for (Object row : filterTokens(tokens, false))
                    {
                        if (row instanceof TokenSortedItem)
                        {
                            TokenSortedItem token = (TokenSortedItem) row;
                            if (token.value.getFullName().toLowerCase().contains(charString.toLowerCase()))
                            {
                                filteredList.add(((TokenSortedItem) row).value);
                            }
                        }
                    }

                    filteredData.clear();
                    filteredData.addAll(filterTokens(filteredList, true));
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredData;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredData = (ArrayList<Object>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
