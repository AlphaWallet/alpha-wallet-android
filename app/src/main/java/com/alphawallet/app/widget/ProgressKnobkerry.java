package com.alphawallet.app.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.alphawallet.app.R;

import static android.view.animation.Animation.INFINITE;

/**
 * Created by JB on 28/11/2020.
 */
public class ProgressKnobkerry extends RelativeLayout
{
    private final ProgressBar spinner;
    private final ProgressBar spinnerKnob;
    private final ProgressBar indeterminate;
    private final ImageView progressComplete;
    private final Context context;

    public ProgressKnobkerry(Context ctx, AttributeSet attrs)
    {
        super(ctx, attrs);
        inflate(ctx, R.layout.item_progress_knobkerry, this);
        spinner = (ProgressBar) findViewById(R.id._progress_bar_main);
        spinnerKnob = (ProgressBar) findViewById(R.id._progress_bar_knob);
        indeterminate = (ProgressBar) findViewById(R.id._progress_bar_waiting);
        progressComplete = (ImageView) findViewById(R.id._progress_complete);
        context = ctx;
    }

    public void startAnimation(long secondsToComplete)
    {
        spinner.setVisibility(View.VISIBLE);
        spinnerKnob.setVisibility(View.VISIBLE);
        indeterminate.setVisibility(View.GONE);
        ObjectAnimator animation = ObjectAnimator.ofInt(spinner, "progress", 0, 500);
        animation.setDuration(secondsToComplete * DateUtils.SECOND_IN_MILLIS);
        animation.setInterpolator(new LinearInterpolator());
        animation.start();

        Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_knob_anim);
        rotation.setDuration(secondsToComplete * DateUtils.SECOND_IN_MILLIS);
        rotation.setRepeatCount(INFINITE);
        spinnerKnob.startAnimation(rotation);
    }

    public void waitCycle()
    {
        spinner.setVisibility(View.GONE);
        spinnerKnob.setVisibility(View.GONE);
        indeterminate.setVisibility(View.VISIBLE);

        Animation rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_knob_anim);
        rotation.setDuration(2 * DateUtils.SECOND_IN_MILLIS);
        rotation.setRepeatCount(INFINITE);
        indeterminate.startAnimation(rotation);
    }

    public void setComplete(boolean succeeded)
    {
        indeterminate.clearAnimation();
        spinnerKnob.clearAnimation();
        spinner.clearAnimation();

        if (succeeded)
        {
            progressComplete.setImageResource(R.drawable.ic_correct);
        }
        else
        {
            progressComplete.setImageResource(R.drawable.ic_tx_fail);
        }

        progressComplete.setAlpha(0.0f);
        progressComplete.animate().alpha(1.0f).setDuration(500).setListener(new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation)
            {
                spinner.setVisibility(View.GONE);
                spinnerKnob.setVisibility(View.GONE);
                indeterminate.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
        progressComplete.setVisibility(View.VISIBLE);
    }
}
