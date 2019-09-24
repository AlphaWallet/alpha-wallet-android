package com.alphawallet.app.ui.widget.holder;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.HelpItem;

public class HelpHolder extends BinderViewHolder<HelpItem> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1989;

    private final RelativeLayout questionLayout;
    private final LinearLayout answerLayout;
    private final TextView questionText;
    private final TextView answerText;


    public HelpHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        questionLayout = findViewById(R.id.layout_question);
        answerLayout = findViewById(R.id.layout_answer);
        questionText = findViewById(R.id.text_question);
        answerText = findViewById(R.id.text_answer);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable HelpItem helpItem, @NonNull Bundle addition) {
        questionText.setText(helpItem.getQuestion());

        answerText.setText(helpItem.getAnswer());

        LinearLayout container = findViewById(R.id.item_help);

        container.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    }

    @Override
    public void onClick(View v) {
        if (answerLayout.getVisibility() == View.GONE) {
            answerLayout.setVisibility(View.VISIBLE);
        } else {
            answerLayout.setVisibility(View.GONE);
        }
    }
}
