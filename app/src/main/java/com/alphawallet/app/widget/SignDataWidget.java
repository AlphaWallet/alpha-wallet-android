package com.alphawallet.app.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.alphawallet.token.entity.Signable;

/**
 * Created by JB on 8/01/2021.
 */
public class SignDataWidget extends LinearLayout
{
    private final TextView textSignDetails;
    private final TextView textSignDetailsMax;
    private final LinearLayout layoutHolder;
    private final ImageView moreArrow;
    private final ScrollView scrollView;
    private final TextView messageTitle;
    private ActionSheetInterface sheetInterface;
    private Signable signable;

    public SignDataWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_sign_data, this);
        textSignDetails = findViewById(R.id.text_sign_data);
        textSignDetailsMax = findViewById(R.id.text_sign_data_max);
        layoutHolder = findViewById(R.id.layout_holder);
        moreArrow = findViewById(R.id.image_more);
        scrollView = findViewById(R.id.scroll_view);
        messageTitle = findViewById(R.id.text_message_title);
    }

    public void setupSignData(Signable message)
    {
        this.signable = message;
        textSignDetails.setText(message.getUserMessage());
        textSignDetailsMax.setText(message.getUserMessage());

        layoutHolder.setOnClickListener(v -> {
            if (textSignDetails.getVisibility() == View.VISIBLE)
            {
                textSignDetails.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                messageTitle.setVisibility(View.GONE);
                moreArrow.setImageResource(R.drawable.ic_expand_less_black);
                if (sheetInterface != null) sheetInterface.lockDragging(true);
            }
            else
            {
                textSignDetails.setVisibility(View.VISIBLE);
                messageTitle.setVisibility(View.VISIBLE);
                scrollView.setVisibility(View.GONE);
                moreArrow.setImageResource(R.drawable.ic_expand_more);
                if (sheetInterface != null) sheetInterface.lockDragging(false);
            }
        });
    }

    public void setLockCallback(ActionSheetInterface asIf)
    {
        sheetInterface = asIf;
    }

    public Signable getSignable()
    {
        return signable;
    }
}
