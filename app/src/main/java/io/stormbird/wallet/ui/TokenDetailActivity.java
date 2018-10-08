package io.stormbird.wallet.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Iterator;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ERC721Attribute;
import io.stormbird.wallet.entity.OpenseaElement;
import io.stormbird.wallet.util.KittyUtils;

public class TokenDetailActivity extends BaseActivity {
    private ImageView image;
    private LinearLayout layoutImage;
    private LinearLayout layoutDetails;
    private LinearLayout layoutName;
    private TextView name;
    private TextView desc;
    private TextView id;
    private TextView generation;
    private TextView cooldown;
    private GridLayout grid;

    private void initViews() {
        image = findViewById(R.id.image);
        layoutImage = findViewById(R.id.layout_image);
        layoutDetails = findViewById(R.id.layout_details);
        layoutName = findViewById(R.id.layout_name_desc);
        name = findViewById(R.id.name);
        desc = findViewById(R.id.description);
        id = findViewById(R.id.id);
        generation = findViewById(R.id.generation);
        cooldown = findViewById(R.id.cooldown);
        grid = findViewById(R.id.grid);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_detail);
        initViews();

        if (getIntent() != null) {
            OpenseaElement element = getIntent().getExtras().getParcelable("element");
            setupPage(element);
        } else {
            finish();
        }
    }

    private void setupPage(OpenseaElement element) {
        layoutImage.setBackgroundResource(R.drawable.background_round_default);
        GradientDrawable drawable = (GradientDrawable) layoutImage.getBackground();

        if (element.backgroundColor != null && !element.backgroundColor.equals("null")) {
            int color = Color.parseColor("#" + element.backgroundColor);
            drawable.setColor(color);
        }

        Glide.with(this)
                .load(element.imageUrl)
                .into(image);

        id.setText(String.valueOf(element.tokenId));

        ERC721Attribute gen = element.traits.get("generation");
        if (gen != null) {
            generation.setText(String.format("Gen %s", gen.attributeValue));
        }

        ERC721Attribute cooldownIndex = element.traits.get("cooldown_index");
        if (cooldownIndex != null) {
            cooldown.setText(String.format("%s Cooldown", KittyUtils.parseCooldownIndex(cooldownIndex.attributeValue)));
        }

        if (element.name != null && !element.name.equals("null")){
            name.setText(element.name);
        } else {
            name.setText(String.format("ID# %s", String.valueOf(element.tokenId)));
        }

        desc.setText(element.description);

        Iterator it = element.traits.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry) it.next();
            String key = pair.getKey().toString();
            ERC721Attribute value = (ERC721Attribute) pair.getValue();

            View view = View.inflate(this, R.layout.item_attribute, null);
            TextView trait = view.findViewById(R.id.trait);
            TextView traitValue = view.findViewById(R.id.value);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            view.setLayoutParams(params);

            trait.setText(key);
            traitValue.setText(value.attributeValue);

            grid.addView(view);

            it.remove(); // avoids a ConcurrentModificationException
        }
    }
}
