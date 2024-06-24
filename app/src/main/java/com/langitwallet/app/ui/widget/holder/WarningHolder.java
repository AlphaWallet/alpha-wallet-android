package com.langitwallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.langitwallet.app.R;
import com.google.android.material.card.MaterialCardView;
import com.langitwallet.app.ui.widget.entity.WarningData;

/**
 * Created by James on 18/07/2019.
 * Stormbird in Sydney
 */
public class WarningHolder extends BinderViewHolder<WarningData>
{
    public static final int VIEW_TYPE = 1015;
    private final TextView title;
    private final TextView detail;
    private final MaterialCardView layoutBackground;
    private final ImageView closeButton;
    private final Button backupButton;

    public WarningHolder(int res_id, ViewGroup parent)
    {
        super(res_id, parent);
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        layoutBackground = findViewById(R.id.card_backup);
        backupButton = findViewById(R.id.button_backup);
        closeButton = findViewById(R.id.btn_close);
    }

    @Override
    public void bind(@Nullable WarningData data, @NonNull Bundle addition)
    {
        title.setText(data.title);
        detail.setText(data.detail);
        layoutBackground.setCardBackgroundColor(ContextCompat.getColor(getContext(), data.colour));
        backupButton.setText(data.buttonText);
        backupButton.setBackgroundColor(data.buttonColour);
        backupButton.setOnClickListener(v -> data.callback.backUpClick(data.wallet));
        closeButton.setOnClickListener(v -> data.callback.remindMeLater(data.wallet));
    }
}
