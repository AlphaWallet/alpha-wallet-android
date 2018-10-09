package io.stormbird.wallet.ui.widget.holder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.ui.TokenDetailActivity;
import io.stormbird.wallet.entity.opensea.Asset;
import io.stormbird.wallet.util.KittyUtils;

/**
 * Created by James on 3/10/2018.
 * Stormbird in Singapore
 */
public class OpenseaHolder extends BinderViewHolder<Asset> {
    public static final int VIEW_TYPE = 1302;
    private final Token token;
    private final TextView titleText;
    private final ImageView image;
    private final TextView generation;
    private final TextView cooldown;
    private final TextView statusText;
    private final LinearLayout layoutToken;

    public OpenseaHolder(int resId, ViewGroup parent, Token token) {
        super(resId, parent);
        titleText = findViewById(R.id.name);
        image = findViewById(R.id.image_view);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        statusText = findViewById(R.id.status);
        layoutToken = findViewById(R.id.layout_token);
        this.token = token;
    }

    @Override
    public void bind(@Nullable Asset asset, @NonNull Bundle addition) {
        String assetName;
        if (asset.getName() != null && !asset.getName().equals("null")) {
            assetName = asset.getName();
        } else {
            assetName = "ID# " + String.valueOf(asset.getTokenId());
        }
        titleText.setText(assetName);

        if (asset.getTraitFromType("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("generation").getValue()));
        } else if (asset.getTraitFromType("gen") != null){
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("gen").getValue()));
        } else {
            generation.setVisibility(View.GONE);
        }

        if (asset.getTraitFromType("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(
                            asset.getTraitFromType("cooldown_index").getValue())));
        } else if (asset.getTraitFromType("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    asset.getTraitFromType("cooldown").getValue()));
        } else {
            cooldown.setVisibility(View.GONE);
        }

        Glide.with(getContext())
                .load(asset.getImagePreviewUrl())
                .into(image);

        layoutToken.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TokenDetailActivity.class);
            intent.putExtra("asset", asset);
            intent.putExtra("token", token);
            getContext().startActivity(intent);
        });
    }

    private void setStatus(C.TokenStatus status) {
        if (status == C.TokenStatus.PENDING) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setBackgroundResource(R.drawable.background_status_pending);
            statusText.setText(R.string.status_pending);
        } else if (status == C.TokenStatus.INCOMPLETE) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setBackgroundResource(R.drawable.background_status_incomplete);
            statusText.setText(R.string.status_incomplete_data);
        } else {
            statusText.setVisibility(View.GONE);
        }
    }
}
