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
import io.stormbird.wallet.web3.Web3TokenView;
import io.stormbird.wallet.web3.entity.Address;
import io.stormbird.wallet.web3.entity.PageReadyCallback;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import io.stormbird.token.util.ZonedDateTime;

import static io.stormbird.wallet.C.Key.TICKET;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener, PageReadyCallback
{
    public static final int VIEW_TYPE = 1011;

    private final WebView iFrame;
    private final Web3TokenView tokenView;
    private final Token token;
    private final LinearLayout iFrameLayout;
    private final boolean iconified;

    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService, boolean iconified)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        iFrameLayout = findViewById(R.id.layout_select_ticket);
        tokenView = findViewById(R.id.token_frame);
        iFrame.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(this);
        assetDefinitionService = assetService;
        token = t;
        tokenView.setChainId(token.tokenInfo.chainId);
        tokenView.setWalletAddress(new Address(token.getWallet()));
        tokenView.setRpcUrl(token.tokenInfo.chainId);
        tokenView.setOnReadyCallback(this);
        this.iconified = iconified;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        try
        {
            if (data.tokenIds.size() == 0) { fillEmpty(); return; }
            String tokenAttrs = buildTokenAttrs(data);
            String viewType = iconified ? "view-iconified" : "view";
            String view = assetDefinitionService.getTokenView(token.getAddress(), viewType);
            String viewData = tokenView.injectWeb3TokenInit(getContext(), view, tokenAttrs);

            tokenView.loadData(viewData, "text/html", "utf-8");

            if (iconified)
            {
                iFrameLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), TokenFunctionActivity.class);
                    intent.putExtra(TICKET, token);
                    intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(data.tokenIds, false));
                    intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    getContext().startActivity(intent);
                });
            }
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private String buildTokenAttrs(TicketRange data) throws Exception
    {
        BigInteger firstTokenId = data.tokenIds.get(0);

        NonFungibleToken nft = assetDefinitionService.getNonFungibleToken(token.getAddress(), firstTokenId);
        StringBuilder attrs = new StringBuilder();
        addPair(attrs, "name", token.getTokenTitle(nft));
        addPair(attrs, "symbol", token.tokenInfo.symbol);
        addPair(attrs, "_count", String.valueOf(data.tokenIds.size()));

        for (String attrKey : nft.getAttributes().keySet())
        {
            NonFungibleToken.Attribute attr = nft.getAttribute(attrKey);
            addPair(attrs, attrKey, attr.text);
        }

        return attrs.toString();
    }

    private void addPair(StringBuilder attrs, String name, String value) throws ParseException
    {
        attrs.append(name);
        attrs.append(": ");

        if (name.equals("time"))
        {
            DateTime dt = DateTimeFactory.getDateTime(value);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm:ssZ");
            String JSDate = dt.format(simpleDateFormat) + "T" + dt.format(simpleTimeFormat);

            value = "{ generalizedTime: \"" + value + "\", date: new Date(\"" + JSDate + "\") }";// ((DateTime) dt).toString();
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

    @Override
    public void onPageLoaded()
    {
        tokenView.callToJS("refresh()");
    }
}
