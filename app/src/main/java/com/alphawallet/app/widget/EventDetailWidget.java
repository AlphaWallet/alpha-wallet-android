package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AssetDefinitionService;

/**
 * Created by JB on 8/12/2020.
 */
public class EventDetailWidget extends LinearLayout
{
    private final TextView title;
    private final TextView symbol;
    private final TextView detail;
    private final LinearLayout holdingView;
    private final TokenIcon icon;

    public EventDetailWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_default_event, this);
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        symbol = findViewById(R.id.text_title_symbol);
        holdingView = findViewById(R.id.layout_default_event);
        icon = findViewById(R.id.token_icon);

        if (isInEditMode()) holdingView.setVisibility(View.VISIBLE);
    }

    public void setupView(RealmAuxData data, Token token, AssetDefinitionService svs, String eventAmount)
    {
        holdingView.setVisibility(View.VISIBLE);
        icon.bindData(token, svs);
        title.setText(data.getTitle(getContext()));
        symbol.setText(token.getSymbol());
        int resourceId;
        switch (data.getFunctionId())
        {
            case "sent":
                resourceId = R.string.default_to;
                break;
            case "received":
                resourceId = R.string.default_from;
                break;
            case "approvalObtained":
                resourceId = R.string.default_approved;
                break;
            case "ownerApproved":
                detail.setText(getContext().getString(R.string.default_approve, eventAmount, data.getDetailAddress()));
                return;
            default:
                resourceId = R.string.default_to;
                break;
        }

        detail.setText(getContext().getString(resourceId, eventAmount, token.getSymbol(), data.getDetailAddress()));
    }

    public void setupTransactionView(Transaction tx, Token token, AssetDefinitionService svs, String supplimentalInfo)
    {
        holdingView.setVisibility(View.VISIBLE);
        icon.bindData(token, svs);
        symbol.setText(token.getSymbol());
        title.setVisibility(View.GONE);
        if (supplimentalInfo.charAt(0) == '-')
        {
            detail.setText(getContext().getString(R.string.default_to, supplimentalInfo.substring(2), "", token.getFullName()));
        }
        else
        {
            detail.setText(getContext().getString(R.string.default_from, supplimentalInfo.substring(2), "", token.getFullName()));
        }
    }
}

