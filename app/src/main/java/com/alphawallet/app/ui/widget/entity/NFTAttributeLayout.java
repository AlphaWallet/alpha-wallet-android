package com.alphawallet.app.ui.widget.entity;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.adapter.TraitsAdapter;
import com.alphawallet.app.widget.TokenInfoCategoryView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by JB on 2/09/2021.
 */
public class NFTAttributeLayout extends LinearLayout {
    private final TokenInfoCategoryView labelAttributes;
    private final RecyclerView recyclerView;

    public NFTAttributeLayout(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        View view = inflate(context, R.layout.item_nft_attributes, this);
        labelAttributes = findViewById(R.id.label_attributes);
        recyclerView = view.findViewById(R.id.recycler_view);
    }

    public void bind(Token token, NFTAsset asset)
    {
        List<OpenSeaAsset.Trait> traits = new ArrayList<>();
        for (Map.Entry<String, String> entry : asset.getAttributes().entrySet())
        {
            traits.add(new OpenSeaAsset.Trait(entry.getKey(), entry.getValue()));
        }
        bind(token, traits, 0);
    }

    public void bind(Token token, List<OpenSeaAsset.Trait> traits, long totalSupply)
    {
        TraitsAdapter adapter = new TraitsAdapter(getContext(), traits, totalSupply);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);
        setAttributeLabel(token.tokenInfo.name, adapter.getItemCount());
    }

    private void setAttributeLabel(String tokenName, int size)
    {
        if (size > 0 && tokenName.equalsIgnoreCase("cryptokitties"))
        {
            labelAttributes.setTitle(getContext().getString(R.string.label_cattributes));
            labelAttributes.setVisibility(View.VISIBLE);
        }
        else if (size > 0)
        {
            labelAttributes.setTitle(getContext().getString(R.string.label_attributes));
            labelAttributes.setVisibility(View.VISIBLE);
        }
        else
        {
            labelAttributes.setVisibility(View.GONE);
        }
    }

    //In case anyone is using this and only wants it to remove the gridviews
    @Override
    public void removeAllViews()
    {
        labelAttributes.setVisibility(View.GONE);
    }
}
