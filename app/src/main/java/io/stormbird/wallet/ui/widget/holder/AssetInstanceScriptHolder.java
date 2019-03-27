package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.JsInjectorClient;
import io.stormbird.wallet.web3.entity.Address;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<String> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1011;

    private final WebView iFrame;
    private final Token token;
    private final LinearLayout detailLayout;
    private final WebView detailFrame;

    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService)
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
    public void bind(@Nullable String data, @NonNull Bundle addition)
    {
        String buildToken = "<script> const web3.tokens.data.currentInstance = {\n" +
                "    name: \"Reserve Token\",\n" +
                "    symbol: \"RSRV\",\n" +
                "    _count: 1,\n" +
                "    category: 1,\n" +
                "    venue: \"Pallazo Versache\",\n" +
                "    countryA: \"SG\",\n" +
                "    countryB: \"MY\",\n" +
                "    match: \"11\",\n" +
                "    locality: \"Singapore\",\n" +
                "    time: { locale: new Date(), venue: new Date() }\n" +
                "}</script>";

        //prep data
        data = JsInjectorClient.injectJS(data, buildToken);

        try
        {
            iFrame.loadData(data, "text/html", "utf-8");
            //iFrame.loadData(data, "text/html", "utf-8");
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

    private String loadInitJs(Context context) {
        String initSrc = loadFile(context, R.raw.init);
        return String.format(initSrc, token.getWallet(), "", token.tokenInfo.chainId);
    }

    private String loadFile(Context context, @RawRes int rawRes) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = context.getResources().openRawResource(rawRes);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            Log.d("READ_JS_TAG", "Ex", ex);
        }
        return new String(buffer);
    }

    @Override
    public void onClick(View v) {

    }
}
