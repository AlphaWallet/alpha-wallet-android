package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.opensea.Asset;

/**
 * Created by James on 12/11/2018.
 * Stormbird in Singapore
 */
public class OpenseaSelectHolder extends OpenseaHolder
{
    private final AppCompatRadioButton select;

    public OpenseaSelectHolder(int resId, ViewGroup parent, Token token)
    {
        super(resId, parent, token);
        select = findViewById(R.id.radioBox);
    }

    @Override
    public void bind(@Nullable Asset asset, @NonNull Bundle addition)
    {
        super.bind(asset, addition);
        select.setVisibility(View.VISIBLE);
    }
}
