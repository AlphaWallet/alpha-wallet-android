package com.alphawallet.app.ui.widget.holder;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.HelpItem;
import com.alphawallet.app.ui.StaticViewer;

import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class HelpHolder extends BinderViewHolder<HelpItem> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1989;

    private final RelativeLayout questionLayout;
    private final LinearLayout answerLayout;
    private final TextView questionText;
    private final TextView answerText;
    private final WebView webView;
    private int rawResource;


    public HelpHolder(int resId, ViewGroup parent, WebView w) {
        super(resId, parent);
        questionLayout = findViewById(R.id.layout_question);
        answerLayout = findViewById(R.id.layout_answer);
        questionText = findViewById(R.id.text_question);
        answerText = findViewById(R.id.text_answer);
        webView = w;
        itemView.setOnClickListener(this);
        rawResource = 0;
    }

    @Override
    public void bind(@Nullable HelpItem helpItem, @NonNull Bundle addition) {
        questionText.setText(helpItem.getQuestion());
        if (helpItem.getResource() > 0) rawResource = helpItem.getResource();
        else answerText.setText(helpItem.getAnswer());

        answerLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);

        LinearLayout container = findViewById(R.id.item_help);

        container.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    }

    @Override
    public void onClick(View v) {
        if (rawResource > 0)
        {
            String base64Text = getResource(rawResource);
            openViewer(base64Text, questionText.getText().toString());
        }
        else if (answerText.getText().toString().contains(".html"))
        {
            //intent to open web page
            webView.setVisibility(View.VISIBLE);
            String url = "file:///android_asset/" + answerText.getText().toString();
            webView.loadUrl(url);
        }
        else
        {
            if (answerLayout.getVisibility() == View.GONE)
            {
                answerLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                answerLayout.setVisibility(View.GONE);
            }
        }
    }

    private void openViewer(String base64Text, String title)
    {
        Intent intent = new Intent(getContext(), StaticViewer.class);
        intent.putExtra(C.EXTRA_STATE, base64Text);
        intent.putExtra(C.EXTRA_PAGE_TITLE, title);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    private String getResource(@RawRes int rawRes) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = getContext().getResources().openRawResource(rawRes);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            Timber.tag("READ_JS_TAG").d( ex, "Ex");
        }
        return Base64.encodeToString(buffer, Base64.DEFAULT);
    }
}
