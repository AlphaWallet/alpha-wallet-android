package com.alphawallet.app.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
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
    private final TextView previewText;
    private final TextView messageText;
    private final LinearLayout layoutHolder;
    private final ImageView moreArrow;
    private final ScrollView scrollView;
    private ActionSheetInterface sheetInterface;
    private Signable signable;
    private ScrollListener listener;
    private boolean isScrollToBottomRequired;

    public SignDataWidget(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_sign_data, this);
        previewText = findViewById(R.id.text_preview);
        messageText = findViewById(R.id.text_message);
        layoutHolder = findViewById(R.id.layout_holder);
        moreArrow = findViewById(R.id.image_more);
        scrollView = findViewById(R.id.scroll_view);
        TextView messageTitle = findViewById(R.id.text_message_title);
        boolean noTitle = getAttribute(context, attrs);
        if (noTitle)
        {
            messageTitle.setText("");
            messageTitle.setVisibility(GONE);
        }
    }

    private void requireScroll()
    {
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if (scrollView.canScrollVertically(1) || scrollView.canScrollVertically(-1))
                {
                    scrollView.getViewTreeObserver()
                        .addOnScrollChangedListener(() -> {
                            if (scrollView.getChildAt(0).getBottom()
                                == (scrollView.getHeight() + scrollView.getScrollY()))
                            {
                                listener.hasScrolledToBottom();
                            }
                        });
                }
                else
                {
                    listener.hasScrolledToBottom();
                }
                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private boolean getAttribute(Context context, AttributeSet attrs)
    {
        TypedArray a = context.getTheme().obtainStyledAttributes(
            attrs,
            R.styleable.SignDataWidget,
            0, 0
        );

        return a.getBoolean(R.styleable.SignDataWidget_noTitle, false);
    }

    public void setupSignData(Signable signable, ScrollListener listener)
    {
        this.listener = listener;
        isScrollToBottomRequired = true;
        setupSignData(signable);
    }

    public void setupSignData(Signable signable)
    {
        this.signable = signable;
        String message = signable.getUserMessage().toString();
        previewText.setText(message);
        messageText.setText(message);

        layoutHolder.setOnClickListener(v -> {
            if (previewText.getVisibility() == View.VISIBLE)
            {
                previewText.setVisibility(View.INVISIBLE);
                scrollView.setVisibility(View.VISIBLE);
                scrollView.setEnabled(true);
                moreArrow.setImageResource(R.drawable.ic_expand_less_black);
                if (sheetInterface != null) sheetInterface.lockDragging(true);

                if (isScrollToBottomRequired)
                {
                    requireScroll();
                }
            }
            else
            {
                previewText.setVisibility(View.VISIBLE);
                scrollView.setVisibility(View.GONE);
                scrollView.setEnabled(false);
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

    public interface ScrollListener
    {
        void hasScrolledToBottom();
    }
}