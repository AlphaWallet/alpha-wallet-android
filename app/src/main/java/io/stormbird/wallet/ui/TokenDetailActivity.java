package io.stormbird.wallet.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Attribute;
import io.stormbird.wallet.entity.OpenseaElement;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.util.KittyUtils;

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
    private GridLayout grid;

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

        if (getIntent() != null) {
            OpenseaElement element = getIntent().getExtras().getParcelable("element");
            Token token = getIntent().getExtras().getParcelable("token");
            title.setText(String.format("%s %s", token.getFullBalance(), token.getFullName()));
            setupPage(element);
        } else {
            finish();
        }
    }

    private void setupPage(OpenseaElement element) {
        setImage(element);
        setDetails(element);
        setNameAndDesc(element);
        setExternalLink(element);
        setTraits(element);
    }

    private void setTraits(OpenseaElement element) {
        for (ERC721Attribute trait : element.traits) {
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
    }

    private void setExternalLink(OpenseaElement element) {
        openExternal.setText("Open on ");
        openExternal.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(element.externalLink));
            startActivity(intent);
        });
    }

    private void setNameAndDesc(OpenseaElement element) {
        if (element.name != null && !element.name.equals("null")) {
            name.setText(element.name);
        } else {
            name.setText(String.format("ID# %s", String.valueOf(element.tokenId)));
        }
        desc.setText(element.description);
    }

    private void setDetails(OpenseaElement element) {
        id.setText(String.valueOf(element.tokenId));
        if (element.getTraitFromType("generation") != null) {
            generation.setText(String.format("Gen %s",
                    element.getTraitFromType("generation").getValue()));
        } else {
            generation.setVisibility(View.GONE);
        }
        if (element.getTraitFromType("cooldown_index") != null) {
            cooldown.setText(String.format("%s Cooldown",
                    KittyUtils.parseCooldownIndex(element.getTraitFromType("cooldown_index").getValue())));
        } else if (element.getTraitFromType("cooldown") != null) { // Non-CK
            cooldown.setText(String.format("%s Cooldown",
                    element.getTraitFromType("cooldown").getValue()));
        } else {
            cooldown.setVisibility(View.GONE);
        }
    }

    private void setImage(OpenseaElement element) {
        layoutImage.setBackgroundResource(R.drawable.background_round_default);
        GradientDrawable drawable = (GradientDrawable) layoutImage.getBackground();

        if (element.backgroundColor != null && !element.backgroundColor.equals("null")) {
            int color = Color.parseColor("#" + element.backgroundColor);
            drawable.setColor(color);
        }

        Glide.with(this)
                .load(element.imageUrl)
                .into(image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_qr, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
