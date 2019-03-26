package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.TokenFunctionActivity;
import io.stormbird.wallet.web3.JsInjectorClient;

import java.math.BigInteger;
import java.text.ParseException;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    public static final int VIEW_TYPE = 1011;

    private final WebView iFrame;
    private final Token token;
    private final LinearLayout detailLayout;
    private final LinearLayout iFrameLayout;
    private final WebView detailFrame;

    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        detailLayout = findViewById(R.id.layout_usage_details);
        iFrameLayout = findViewById(R.id.layout_select_ticket);
        detailFrame = findViewById(R.id.usage_details);
        iFrame.getSettings().setBuiltInZoomControls(false);
        iFrame.getSettings().setJavaScriptEnabled(true);
        iFrame.getSettings().setDisplayZoomControls(false);
        itemView.setOnClickListener(this);
        assetDefinitionService = assetService;
        token = t;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        try
        {
            int index = -1;
            for (int i = 0; i < data.tokenIds.size(); i++)
            {
                if (!data.tokenIds.get(i).equals(BigInteger.ZERO))
                {
                    index = i;
                    break;
                }
            }

            if (index == -1)
            {
                fillEmpty();
                return;
            }

            NonFungibleToken nft = assetDefinitionService.getNonFungibleToken(token.getAddress(), data.tokenIds.get(index));
            StringBuilder attrs = new StringBuilder();
            addPair(attrs, "name", token.getTokenTitle(nft));
            addPair(attrs, "symbol", token.tokenInfo.symbol);
            addPair(attrs, "_count", String.valueOf(data.tokenIds.size()));

            for (String attrKey : nft.getAttributes().keySet())
            {
                NonFungibleToken.Attribute attr = nft.getAttribute(attrKey);
                addPair(attrs, attrKey, attr.text);
            }

            String buildToken = "<script> const currentTokenInstance = {\n" +
                    attrs.toString() + //insert token definition
                    "}\n\n" +
                    "class TokenScriptDef {\n" +
                    "        constructor(walletAddress, tokenDef) {\n" +
                    "          this.address = walletAddress;\n" +
                    "          this.token = tokenDef;\n" +
                    "        }\n" +
                    "      }\n\n" +
                    "      const web3 = new TokenScriptDef(" + token.getWallet() + ", currentTokenInstance)\n" +
                    "</script>";

            String view = assetDefinitionService.getTokenView(token.getAddress(), "view");

            String viewData = JsInjectorClient.injectJS(view, buildToken);

            iFrame.loadData(viewData, "text/html", "utf-8");

            iFrameLayout.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TokenFunctionActivity.class);
                intent.putExtra(TICKET, token);
                intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(data.tokenIds, false));
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                getContext().startActivity(intent);
            });
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void addPair(StringBuilder attrs, String name, String value) throws ParseException
    {
        attrs.append(name);
        attrs.append(": ");

        if (name.equals("time"))
        {
            DateTime dt = DateTimeFactory.getDateTime(value);
            value = "{ venue: new Date(" + dt.toEpoch() + ") }";// ((DateTime) dt).toString();
            attrs.append(value);
        }
        else
        {
            attrs.append("\"");
            attrs.append(value);
            attrs.append("\"");
        }

        attrs.append(",\n");
    }

    private void fillEmpty()
    {
        iFrame.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    @Override
    public void onClick(View v) {

    }
}
