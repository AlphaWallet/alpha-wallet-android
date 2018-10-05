package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import io.reactivex.disposables.Disposable;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.OpenseaElement;
import io.stormbird.wallet.service.OpenseaService;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaHolder extends BinderViewHolder<OpenseaElement> {

    public static final int VIEW_TYPE = 1302;

    private final TextView titleText;
    private final ImageView image;
    private final TextView statusText;
    private final LinearLayout layoutDetails;
    private final OpenseaService openseaService;

    @Nullable
    private Disposable queryService;

    public OpenseaHolder(int resId, ViewGroup parent, OpenseaService service) {
        super(resId, parent);
        titleText = findViewById(R.id.name);
        image = findViewById(R.id.image_view);
        statusText = findViewById(R.id.status);
        layoutDetails = findViewById(R.id.layout_details);
        openseaService = service;
    }

    @Override
    public void bind(@Nullable OpenseaElement element, @NonNull Bundle addition)
    {
        //for now add title and ERC721 graphic
        String assetName;
        if (element.assetName != null && !element.assetName.equals("null"))
        {
            assetName = element.assetName;
        }
        else
        {
            assetName = "ID# " + String.valueOf(element.tokenId);
        }
        titleText.setText(assetName);

        //now add the graphic
        Glide.with(getContext())
            .load(element.imageURL)
            .into(image);
    }
}
