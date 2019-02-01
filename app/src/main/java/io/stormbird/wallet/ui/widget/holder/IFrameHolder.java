package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 13/12/2018.
 * Stormbird in Singapore
 */

public class IFrameHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1010;

    private final WebView iFrame;
    private final Token token;
    private final LinearLayout detailLayout;
    private final WebView detailFrame;

    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    public IFrameHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        detailLayout = findViewById(R.id.layout_usage_details);
        detailFrame = findViewById(R.id.usage_details);
        iFrame.getSettings().setBuiltInZoomControls(false);
        iFrame.getSettings().setJavaScriptEnabled(true);
        iFrame.getSettings().setDisplayZoomControls(false);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        token = t;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        try
        {
            String getContent = assetDefinition.getIntroductionCode(token.getAddress());
            iFrame.loadData(getContent, "text/html", "utf-8");
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void fillEmpty()
    {
        iFrame.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    public void onClick(View v) {

    }
}
