package com.alphawallet.app.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.PageReadyCallback;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.ui.TokenFunctionActivity;

import static com.alphawallet.app.C.Key.TICKET;

/**
 * Created by James on 31/05/2019.
 * Stormbird in Sydney
 */
public class TokenscriptViewHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener, PageReadyCallback
{
    public static final int VIEW_TYPE = 1013;

    private final Web3TokenView tokenView;
    private final Token token;
    private final boolean iconified;
    private final ProgressBar waitSpinner;
    private final AppCompatRadioButton select;
    private final LinearLayout frameLayout;

    private BigInteger tokenId;

    private StringBuilder attrs;

    private final AssetDefinitionService assetDefinitionService; //need to cache this locally, unless we cache every string we need in the constructor

    public TokenscriptViewHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService, boolean iconified)
    {
        super(resId, parent);
        tokenView = findViewById(R.id.token_frame);
        waitSpinner = findViewById(R.id.progress_element);
        frameLayout = findViewById(R.id.layout_webwrapper);
        select = findViewById(R.id.radioBox);
        tokenView.setVisibility(View.VISIBLE);
        itemView.setOnClickListener(this);
        select.setVisibility(View.GONE);
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
            waitSpinner.setVisibility(View.VISIBLE);
            tokenView.setVisibility(View.GONE);
            getAttrs(data);
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void displayTicket(String tokenAttrs, TicketRange data)
    {
        waitSpinner.setVisibility(View.GONE);
        tokenView.setVisibility(View.VISIBLE);

        String viewType = iconified ? "view-iconified" : "view";
        String view = assetDefinitionService.getTokenView(token.tokenInfo.chainId, token.getAddress(), viewType);
        String style = assetDefinitionService.getTokenView(token.tokenInfo.chainId, token.getAddress(), "style");
        String viewData = tokenView.injectWeb3TokenInit(getContext(), view, tokenAttrs);
        viewData = tokenView.injectStyleData(viewData, style); //style injected last so it comes first

        String base64 = Base64.encodeToString(viewData.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        tokenView.loadData(base64, "text/html; charset=utf-8", "base64");

        if (iconified)
        {
            frameLayout.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TokenFunctionActivity.class);
                intent.putExtra(TICKET, token);
                intent.putExtra(C.EXTRA_TOKEN_ID, token.intArrayToString(data.tokenIds, false));
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                getContext().startActivity(intent);
            });
        }
    }

    private void getAttrs(TicketRange data) throws Exception
    {
        tokenId = data.tokenIds.get(0);
        attrs = assetDefinitionService.getTokenAttrs(token, tokenId, data.tokenIds.size());
        assetDefinitionService.resolveAttrs(token, tokenId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onAttr, this::onError, () -> displayTicket(attrs.toString(), data))
                .isDisposed();
    }

    private void onError(Throwable throwable)
    {
        throwable.printStackTrace();
        //fill attrs from database
        TokenScriptResult tsr = assetDefinitionService.getTokenScriptResult(token, tokenId);
        for (TokenScriptResult.Attribute attr : tsr.getAttributes().values())
        {
            TokenScriptResult.addPair(attrs, attr.id, attr.text);
        }
    }

    private void onAttr(TokenScriptResult.Attribute attribute)
    {
        //add to string
        TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
    }

    private void fillEmpty()
    {
        tokenView.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
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

