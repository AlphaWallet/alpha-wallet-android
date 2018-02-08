package com.wallet.crypto.alphawallet.widget;

/**
 * Created by James on 6/02/2018.
 */

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.wallet.crypto.alphawallet.R;

public class ProgressView extends RelativeLayout {
    private ProgressBar progress;
    private TextView counter;
    private Context context;

    public ProgressView(@NonNull Context context) {
        super(context);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_progress_view, this, false);
        addView(view);
        progress = view.findViewById(R.id.progress);
        counter = view.findViewById(R.id.textViewProgress);
        context = view.getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            counter.setZ(1.0f);
            progress.setZ(0.0f);
        }
    }

    public void updateProgress(Integer prog) {
        if (prog < 100) {
            counter.setText(String.valueOf(prog) + "%");
            progress.setVisibility(VISIBLE);
            counter.setVisibility(VISIBLE);
            setVisibility(VISIBLE);
        } else {
            hide();
        }
    }

    public void displayToast(String msg)
    {
        hide();
        Toast.makeText(context, msg, Toast.LENGTH_SHORT);
    }

    public void hide() {
        hideAllComponents();
        setVisibility(GONE);
    }

    private void hideAllComponents() {
        progress.setVisibility(GONE);
        counter.setVisibility(GONE);
        setVisibility(VISIBLE);
    }

    public void showEmpty(View view) {
        hideAllComponents();
    }
}
