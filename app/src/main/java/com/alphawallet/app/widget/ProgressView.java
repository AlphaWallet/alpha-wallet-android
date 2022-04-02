package com.alphawallet.app.widget;

/**
 * Created by James on 6/02/2018.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;

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
        progress = view.findViewById(R.id.progress_v);
        counter = view.findViewById(R.id.textViewProgress);
        context = view.getContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            counter.setZ(1.0f);
            progress.setZ(0.99f);
        }
    }

    public void updateProgress(Integer prog) {
        if (prog < 100) {
            counter.setText(prog + "%");
            progress.setVisibility(VISIBLE);
            counter.setVisibility(VISIBLE);
            setVisibility(VISIBLE);
        } else {
            hide();
        }
    }

//    public void displayToast(String msg)
//    {
//        hide();
//        Toast.makeText(context, msg, Toast.LENGTH_SHORT);
//    }

    public void hide() {
        hideAllComponents();
        setVisibility(GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setZ(1.0f);
        }
    }

    private void hideAllComponents() {
        progress.setVisibility(GONE);
        counter.setVisibility(GONE);
        setVisibility(VISIBLE);
    }

    public void showEmpty(View view) {
        hideAllComponents();
    }

    public void setWhiteCircle()
    {
        int colour = ContextCompat.getColor(context, R.color.surface);
        setTint(colour, false);
    }

    private void setTint(@ColorInt int color,
                                boolean skipIndeterminate) {
        ColorStateList sl = ColorStateList.valueOf(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progress.setProgressTintList(sl);
            progress.setSecondaryProgressTintList(sl);
            if (!skipIndeterminate)
                progress.setIndeterminateTintList(sl);
        } else {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mode = PorterDuff.Mode.MULTIPLY;
            }
            if (!skipIndeterminate && progress.getIndeterminateDrawable() != null)
                progress.getIndeterminateDrawable().setColorFilter(color, mode);
            if (progress.getProgressDrawable() != null)
                progress.getProgressDrawable().setColorFilter(color, mode);
        }
    }
}
