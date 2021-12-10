package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.SearchToolbarCallback;

/**
 * Created by JB on 9/12/2021.
 */
public class SearchToolbar extends FrameLayout
{
    private final View back;
    private final EditText searchText;
    private final View clearText;
    private SearchToolbarCallback searchCallback;

    private final Handler delayHandler = new Handler(Looper.getMainLooper());

    public SearchToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(getContext(), R.layout.input_toolbar, this);
        back = findViewById(R.id.st_backArrow);
        searchText = findViewById(R.id.st_editText);
        clearText = findViewById(R.id.st_clear);

        init();
    }

    public SearchToolbar(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(getContext(), R.layout.input_toolbar, this);
        back = findViewById(R.id.st_backArrow);
        searchText = findViewById(R.id.st_editText);
        clearText = findViewById(R.id.st_clear);

        init();
    }

    public void setSearchCallback(SearchToolbarCallback cb)
    {
        searchCallback = cb;
        back.setOnClickListener(v -> {
            cb.backPressed();
        });

        searchCallback.searchText("");
        searchText.requestFocus();
    }

    private void init()
    {
        //draw focus
        searchText.addTextChangedListener(textWatcher);
        clearText.setOnClickListener(v -> {
            searchText.setText("");
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void afterTextChanged(final Editable s) {
            delayHandler.removeCallbacksAndMessages(null);
            delayHandler.postDelayed(() -> {
                if (searchCallback != null) searchCallback.searchText(s.toString());
            }, 750);
        }
    };
}
