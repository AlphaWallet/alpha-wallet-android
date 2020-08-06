package com.alphawallet.app.ui.widget.holder;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.OnEventClickListener;
import com.alphawallet.app.ui.widget.entity.IconItem;
import com.alphawallet.app.ui.widget.entity.StatusType;
import com.alphawallet.app.util.BalanceUtils;
import com.alphawallet.app.util.LocaleUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.widget.TokenIcon;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.alphawallet.app.ui.widget.holder.TransactionHolder.DEFAULT_ADDRESS_ADDITIONAL;
import static com.alphawallet.app.ui.widget.holder.TransactionHolder.TRANSACTION_BALANCE_PRECISION;

/**
 * Created by JB on 28/07/2020.
 */
public class EventHolder extends BinderViewHolder<EventMeta> implements View.OnClickListener
{
    public static final int VIEW_TYPE = 2016;

    private final TokenIcon icon;
    private final TextView title;
    private final TextView value;
    private final TextView detail;
    private final TextView timeStamp;
    private final AssetDefinitionService assetDefinition;
    private Token token;

    private final OnEventClickListener onEventClickListener;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final TokensService tokensService;

    private String eventKey;

    public EventHolder(int resId, ViewGroup parent, TokensService service, FetchTransactionsInteract interact,
                       AssetDefinitionService svs, OnEventClickListener eventClickListener)
    {
        super(resId, parent);
        icon = findViewById(R.id.token_icon);
        title = findViewById(R.id.title);
        value = findViewById(R.id.value);
        detail = findViewById(R.id.detail);
        timeStamp = findViewById(R.id.time_stamp);
        itemView.setOnClickListener(this);
        assetDefinition = svs;

        onEventClickListener = eventClickListener;
        fetchTransactionsInteract = interact;
        tokensService = service;
    }

    @Override
    public void bind(@Nullable EventMeta data, @NonNull Bundle addition)
    {
        String walletAddress = addition.getString(DEFAULT_ADDRESS_ADDITIONAL);
        //pull event details from DB
        eventKey = TokensRealmSource.eventKey(data.hash, data.eventName);

        RealmAuxData eventData = fetchTransactionsInteract.fetchEvent(walletAddress, eventKey);
        Transaction tx = fetchTransactionsInteract.fetchCached(walletAddress, data.hash);
        token = tokensService.getToken(eventData.getChainId(), eventData.getTokenAddress());
        String sym = token != null ? token.getSymbol() : getContext().getString(R.string.eth);
        icon.bindData(token, assetDefinition);

        Map<String, EventResult> resultMap = getEventResultMap(eventData.getResult());
        String transactionValue = getEventAmount(eventData, resultMap, tx);

        if (TextUtils.isEmpty(transactionValue))
        {
            value.setVisibility(View.GONE);
        }
        else
        {
            value.setText(getString(R.string.valueSymbol, transactionValue, sym));
        }

        title.setText(getTitle(eventData, sym));
        detail.setText(getDetail(eventData, resultMap));

        //timestamp
        timeStamp.setText(localiseUnixTime(eventData.getResultTime()));
    }

    private String getEventAmount(RealmAuxData eventData, Map<String, EventResult> resultMap, Transaction tx)
    {
        int decimals = token != null ? token.tokenInfo.decimals : C.ETHER_DECIMALS;
        String value = "";
        switch (eventData.getFunctionId())
        {
            case "received":
            case "sent":
                value = BalanceUtils.getScaledValueFixed(new BigDecimal(resultMap.get("amount").value),
                        decimals, 4);
                break;
            case "approvalObtained":
            case "ownerApproved":
                value = BalanceUtils.getScaledValueFixed(new BigDecimal(resultMap.get("value").value),
                        decimals, 4);
                break;
            default:
                if (token != null && tx != null)
                {
                    value = token.isEthereum() ? token.getTransactionValue(tx, 4) : tx.getOperationResult(token, 4);
                }
                break;
        }

        return value;
    }

    private String getDetail(RealmAuxData eventData, Map<String, EventResult> resultMap)
    {
        //TODO: pick up item-view
        //catch standard Token events
        switch (eventData.getFunctionId())
        {
            case "sent":
                icon.setStatusIcon(StatusType.SENT);
                return getString(R.string.sent_to, resultMap.get("to").value);
            case "received":
                icon.setStatusIcon(StatusType.RECEIVE);
                return getString(R.string.from, resultMap.get("from").value);
            case "ownerApproved":
                return getString(R.string.approval_granted_to, resultMap.get("spender").value);
            case "approvalObtained":
                return getString(R.string.approval_obtained_from, resultMap.get("owner").value);
            default:
                //use name of event
                return eventData.getEventName();
        }
    }

    private String getTitle(RealmAuxData eventData, String sym)
    {
        //TODO: pick up item-view
        //catch standard Token events
        switch (eventData.getFunctionId())
        {
            case "sent":
                return getString(R.string.activity_sent, sym);
            case "received":
                return getString(R.string.activity_received, sym);
            case "ownerApproved":
                return getString(R.string.activity_approved, sym);
            case "approvalObtained":
                return getString(R.string.activity_approval_granted, sym);
            default:
                //display non indexed value
                //getString(R.string.valueSymbol, transactionValue, sym)
                return getString(R.string.valueSymbol, eventData.getFunctionId(), sym);
        }
    }

    private Map<String, EventResult> getEventResultMap(String result)
    {
        String[] split = result.split(",");
        resultState state = resultState.NAME;
        Map<String, EventResult> resultMap = new HashMap<>();
        String name = null;
        String type = null;
        for (String r : split)
        {
            switch (state)
            {
                case NAME:
                    name = r;
                    state = resultState.TYPE;
                    break;
                case TYPE:
                    type = r;
                    state = resultState.RESULT;
                    break;
                case RESULT:
                    if (name != null && type != null)
                    {
                        resultMap.put(name, new EventResult(type, r));
                        name = null;
                        type = null;
                    }
                    state = resultState.NAME;
                    break;
            }
        }

        return resultMap;
    }

    private enum resultState {
        NAME,
        TYPE,
        RESULT
    }

    private static class EventResult
    {
        public final String type;
        public final String value;

        public EventResult(String t, String v)
        {
            type = t;
            value = v;
        }
    }

    @Override
    public void onClick(View view) {
        if (onEventClickListener != null) {
            onEventClickListener.onEventClick(view, eventKey);
        }
    }

    private String localiseUnixTime(long timeStampInSec)
    {
        Date date = new java.util.Date(timeStampInSec* DateUtils.SECOND_IN_MILLIS);
        DateFormat timeFormat = java.text.DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(getContext()));
        return timeFormat.format(date);
    }
}
