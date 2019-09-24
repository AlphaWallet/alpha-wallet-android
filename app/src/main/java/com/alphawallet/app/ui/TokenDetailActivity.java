package com.alphawallet.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.alphawallet.app.util.KittyUtils;

import com.alphawallet.app.R;

import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.opensea.Asset;
import com.alphawallet.app.entity.opensea.Trait;

import static com.alphawallet.app.C.EXTRA_STATE;
import static com.alphawallet.app.C.EXTRA_TOKENID_LIST;
import static com.alphawallet.app.C.Key.TICKET;
import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.ui.TransferTicketDetailActivity.TRANSFER_TO_ADDRESS;

public class TokenDetailActivity extends BaseActivity {
    private ImageView image;
    private LinearLayout layoutImage;
    private TextView title;
    private TextView name;
    private TextView desc;
    private TextView id;
    private TextView generation;
    private TextView cooldown;
    private TextView openExternal;
    private TextView labelAttributes;
    private GridLayout grid;
    private Token token;
    private Asset asset;

    private void initViews() {
        title = findViewById(R.id.title);
        image = findViewById(R.id.image);
        layoutImage = findViewById(R.id.layout_image);
        name = findViewById(R.id.name);
        desc = findViewById(R.id.description);
        id = findViewById(R.id.id);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        openExternal = findViewById(R.id.open_external);
        labelAttributes = findViewById(R.id.label_attributes);
        grid = findViewById(R.id.grid);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);
        initViews();
        toolbar();
        setTitle(R.string.empty);

        if (getIntent() != null && getIntent().getExtras() != null) {
            asset = getIntent().getExtras().getParcelable("asset");
            token = getIntent().getExtras().getParcelable("token");
            title.setText(String.format("%s %s", "1", token.getFullName()));
            setupPage();
        } else {
            finish();
        }
    }

    private void setupPage() {
        setImage(asset);
        setDetails(asset);
        setNameAndDesc(asset);
        setExternalLink(asset);
        setTraits(asset);

//        if (token instanceof ERC721Token)
//        {
//            findViewById(R.id.button_transfer).setVisibility(View.GONE);
//        }

        findViewById(R.id.button_transfer).setOnClickListener(v -> {
            openTransferDirectDialog();
        });
    }

    private void setTraits(Asset asset) {
        if (asset.getTraits() != null && !asset.getTraits().isEmpty()) {
            if (asset.getAssetContract().getName().equals("CryptoKitties")) {
                labelAttributes.setText(R.string.label_cattributes);
            } else {
                labelAttributes.setText(R.string.label_attributes);
            }
            for (Trait trait : asset.getTraits()) {
                View attributeView = View.inflate(this, R.layout.item_attribute, null);
                TextView traitType = attributeView.findViewById(R.id.trait);
                TextView traitValue = attributeView.findViewById(R.id.value);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED, 1f),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f));
                attributeView.setLayoutParams(params);
                traitType.setText(trait.getTraitType());
                traitValue.setText(trait.getValue());
                grid.addView(attributeView);
            }
        } else {
            labelAttributes.setVisibility(View.GONE);
        }
    }

    private void setExternalLink(Asset asset) {
        if (asset.getExternalLink() != null && !asset.getExternalLink().equals("null")) {
            openExternal.setText(getString(R.string.open_on_external_link,
                    asset.getAssetContract().getName()));

            openExternal.setOnClickListener(v -> {
                Intent intent = new Intent(TokenDetailActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("url", asset.getExternalLink());
                startActivity(intent);
            });
        } else {
            openExternal.setVisibility(View.GONE);
        }
    }

    private void setNameAndDesc(Asset asset) {
        name.setText(asset.getName());
        desc.setText(asset.getDescription());
    }

    private void setDetails(Asset asset) {
        id.setText(asset.getTokenId());
        if (asset.getTraitFromType("generation") != null) {
            generation.setText(String.format("Gen %s",
                    asset.getTraitFromType("generation").getValue()));
        } else if (asset.getTraitFromType("gen") != null) {
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
    }

    private void setImage(Asset asset) {
        layoutImage.setBackgroundResource(R.drawable.background_round_default);
        GradientDrawable drawable = (GradientDrawable) layoutImage.getBackground();

        if (asset.getBackgroundColor() != null && !asset.getBackgroundColor().equals("null")) {
            int color = Color.parseColor("#" + asset.getBackgroundColor());
            drawable.setColor(color);
        } else {
            drawable.setColor(getResources().getColor(R.color.transparent));
        }

        Glide.with(this)
                .load(asset.getImagePreviewUrl())
                .into(image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void openTransferDirectDialog()
    {
        Intent intent = new Intent(this, TransferTicketDetailActivity.class);
        intent.putExtra(WALLET, new Wallet(token.getWallet()));
        intent.putExtra(TICKET, token);
        intent.putExtra(EXTRA_TOKENID_LIST, asset.getTokenId());
        intent.putExtra(EXTRA_STATE, TRANSFER_TO_ADDRESS);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }
}
