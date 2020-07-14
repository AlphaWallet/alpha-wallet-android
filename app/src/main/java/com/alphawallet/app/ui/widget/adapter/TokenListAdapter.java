package com.alphawallet.app.ui.widget.adapter;

import android.annotation.SuppressLint;
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
import com.alphawallet.app.entity.TokenManageType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelData;
import com.alphawallet.app.ui.widget.entity.ManageTokensLabelSortedItem;
import com.alphawallet.app.ui.widget.entity.SortedItem;
import com.alphawallet.app.ui.widget.entity.TokenSortedItem;
import com.alphawallet.app.util.Utils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.alphawallet.app.entity.TokenManageType.DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.HIDDEN_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_DISPLAY_TOKEN;
import static com.alphawallet.app.entity.TokenManageType.LABEL_HIDDEN_TOKEN;

public class TokenListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private LayoutInflater inflater;
    private final Context context;
    private ItemClickListener listener;
    protected final AssetDefinitionService assetService;

    /*
    Below is Backup/Main list based on the Tokens provided.
    We need this Duplicate list to provide Filter/Search functionality
     */
    private final ArrayList<SortedItem> backupItems;
    //This is duplicate list to show on the screen
    private ArrayList<SortedItem> items;

    public TokenListAdapter(Context context, AssetDefinitionService aService, Token[] tokens, ItemClickListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        this.assetService = aService;

        List<Token> tokenList = filterTokens(Arrays.asList(tokens));

        backupItems = new ArrayList<>();
        items = new ArrayList<>();
        backupItems.addAll(setupList(tokenList));
        items.addAll(backupItems);
    }

    private List<Token> filterTokens(List<Token> tokens) {
        ArrayList<Token> filteredList = new ArrayList<>();
        for (Token t : tokens)
        {
            if (t.tokenInfo.name != null)
            {
                if (!t.isEthereum() && !filteredList.contains(t))
                {
                    filteredList.add(t);
                }
            }
        }

        return filteredList;
    }

    private ArrayList<SortedItem> setupList(List<Token> tokens)
    {
        ArrayList<SortedItem> sortedItems = new ArrayList<>();
        for (Token token : tokens)
        {
            TokenSortedItem sortedItem;
            if (token.tokenInfo.isEnabled)
            {
                sortedItem = new TokenSortedItem(
                        DISPLAY_TOKEN, token, token.getNameWeight()
                );
            }
            else
            {
                sortedItem = new TokenSortedItem(
                        HIDDEN_TOKEN, token, token.getNameWeight()
                );
            }
            sortedItems.add(sortedItem);
        }
        sortedItems.add(new ManageTokensLabelSortedItem(
                LABEL_DISPLAY_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.display_tokens)),
                0));
        sortedItems.add(new ManageTokensLabelSortedItem(
                LABEL_HIDDEN_TOKEN,
                new ManageTokensLabelData(context.getString(R.string.hidden_tokens)),
                0));

        Collections.sort(sortedItems, compareByWeight);

        return  sortedItems;
    }

    /*
    Below comparision is like
    First, check for the ViewType which could be any of @TokenManageType
    Second, if type is similar, check for the weight given to the Token
     */
    Comparator<SortedItem> compareByWeight = (o1, o2) -> {
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
    };

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, @TokenManageType.ManageType int viewType) {
        switch (viewType) {
            case LABEL_DISPLAY_TOKEN:
            case LABEL_HIDDEN_TOKEN:
                View view = inflater.inflate(R.layout.layout_manage_tokens_label, viewGroup, false);
                return new LabelViewHolder(view);
            case DISPLAY_TOKEN:
            case HIDDEN_TOKEN:
            default:
                view = inflater.inflate(R.layout.item_manage_token, viewGroup, false);
                return new ViewHolder(view);
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        @TokenManageType.ManageType
        int type = getItemViewType(position);

        if (type == LABEL_DISPLAY_TOKEN || type == LABEL_HIDDEN_TOKEN)
        {
            ManageTokensLabelData labelData = (ManageTokensLabelData) items.get(position).value;
            LabelViewHolder labelViewHolder = (LabelViewHolder) viewHolder;
            labelViewHolder.textTitle.setText(labelData.title);
        }
        else if (type == DISPLAY_TOKEN || type == HIDDEN_TOKEN)
        {
            Token token = (Token) items.get(position).value;
            ViewHolder tokenViewHolder = (ViewHolder) viewHolder;
            viewHolder.setIsRecyclable(false);
            tokenViewHolder.tokenName.setText(token.getFullName(assetService, 1));
            tokenViewHolder.chainId = token.tokenInfo.chainId;
            tokenViewHolder.displayTokenIcon(token);
            tokenViewHolder.switchEnabled.setChecked(token.tokenInfo.isEnabled);
            tokenViewHolder.switchEnabled.setTag(new Integer(position));
            tokenViewHolder.switchEnabled.setOnCheckedChangeListener((v, b) -> {
                int pos = Integer.parseInt(v.getTag().toString());
                listener.onItemClick((Token) items.get(pos).value, b);

                if (type == DISPLAY_TOKEN)
                {
                    items.get(pos).viewType = HIDDEN_TOKEN;
                    token.tokenInfo.isEnabled = false;
                }
                else
                {
                    items.get(pos).viewType = DISPLAY_TOKEN;
                    token.tokenInfo.isEnabled = true;
                }
                Collections.sort(items, compareByWeight);
                notifyDataSetChanged();
            });

            if (type == DISPLAY_TOKEN)
            {
                tokenViewHolder.overlay.setVisibility(View.GONE);
            }
            else
            {
                tokenViewHolder.overlay.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
        RelativeLayout layout;
        TextView tokenName;
        Switch switchEnabled;
        final ImageView icon;
        final TextView textIcon;
        final View overlay;
        int chainId;

        private final CustomViewTarget viewTarget;

        ViewHolder(View itemView) {
            super(itemView);
            layout = itemView.findViewById(R.id.layout_list_item);
            tokenName = itemView.findViewById(R.id.name);
            switchEnabled = itemView.findViewById(R.id.switch_enabled);
            layout.setOnClickListener(this);
            icon = itemView.findViewById(R.id.icon);
            textIcon = itemView.findViewById(R.id.text_icon);
            overlay = itemView.findViewById(R.id.view_overlay);

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

        private void displayTokenIcon(Token token)
        {
            int chainIcon = EthereumNetworkRepository.getChainLogo(token.tokenInfo.chainId);

            IconItem iconItem = assetService.fetchIconForToken(token);
            String tokenName = token.getFullName(assetService, token.getTicketCount());
            viewTarget.getView().setTag(tokenName);

            Glide.with(context.getApplicationContext())
                    .load(iconItem.getUrl())
                    .signature(iconItem.getSignature())
                    .onlyRetrieveFromCache(iconItem.onlyFetchFromCache()) //reduce URL checking, only check once per session
                    .apply(new RequestOptions().circleCrop())
                    .apply(new RequestOptions().placeholder(chainIcon))
                    .dontAnimate()
                    .into(viewTarget);
        }

        private void setupTextIcon() {
            icon.setVisibility(View.GONE);
            textIcon.setVisibility(View.VISIBLE);
            textIcon.setBackgroundTintList(ContextCompat.getColorStateList(context, Utils.getChainColour(chainId)));
            textIcon.setText(Utils.getIconisedText(tokenName.getText().toString()));
        }

        @Override
        public void onClick(View v) {
            switchEnabled.setChecked(!switchEnabled.isChecked());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            listener.onItemClick((Token) items.get(getAdapterPosition()).value, isChecked);
        }
    }

    public class LabelViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textTitle;
        TextView textIgnore;

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
        return items.get(position).viewType;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String searchString = charSequence.toString();
                if (searchString.isEmpty())
                {
                    items.clear();
                    items.addAll(backupItems);
                }
                else
                {
                    ArrayList<SortedItem> tokenList = new ArrayList<>(backupItems);
                    ArrayList<SortedItem> filteredList = new ArrayList<>();
                    for (SortedItem row : tokenList)
                    {
                        if (row instanceof TokenSortedItem)
                        {
                            TokenSortedItem token = (TokenSortedItem) row;
                            if (token.value.getFullName(assetService, 1).toLowerCase().contains(searchString.toLowerCase()))
                            {
                                filteredList.add(row);
                            }
                        }
                        else
                        {
                            filteredList.add(row);
                        }
                    }

                    items.clear();
                    items.addAll(filteredList);
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = items;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                items = (ArrayList<SortedItem>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}
