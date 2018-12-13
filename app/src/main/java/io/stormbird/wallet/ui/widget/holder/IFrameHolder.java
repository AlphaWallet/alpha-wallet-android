package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 13/12/2018.
 * Stormbird in Singapore
 */

public class IFrameHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1010;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    public final WebView iFrame;

    private String test = "<html>\n" +
            "<head>\n" +
            "<style>html { font-family: Helvetica; display: inline-block; margin: 0px auto; text-align: center;}\n" +
            ".stormbut { background-color: #195B6A; border: none; color: white; padding: 16px 40px;\n" +
            "text-decoration: none; font-size: 30px; margin: 2px; cursor: pointer; visibility: visible; }\n" +
            "</style></head>\n" +
            "<body><h1>Stormbird Door Server</h1>\n" +
            "<p>Challenge: 0x07533b690c48b495b8 771b6a85fa723ae0bceef8376a8519fa23ad87b788c885</p>\n" +
            "<p id=\"signLayer\"><button id=\"signButton\" class=\"stormbut\">Open Door</button></p>\n" +
            "<div id=\"sig\"></div>\n" +
            "</body></html>";

    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor
    public Token token;

    public IFrameHolder(int resId, ViewGroup parent, Token token, AssetDefinitionService assetService)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        iFrame.getSettings().setBuiltInZoomControls(false);
        iFrame.getSettings().setJavaScriptEnabled(true);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition)
    {
        this.token = data;
        try
        {
            iFrame.loadData(test, "text/html", "utf-8");
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
