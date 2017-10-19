package com.wallet.crypto.trust.views;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro2;
import com.wallet.crypto.trust.R;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

public class IntroActivity extends AppIntro2 {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance("Private & Secure", null, "Private keys never leave your device.", null, R.mipmap.onboarding_lock, Color.WHITE, Color.BLACK, Color.BLACK));
        addSlide(AppIntroFragment.newInstance("Fully Transparent", null, "Code is open sourced (GPL-3.0 license) and fully audited.", null, R.mipmap.onboarding_open_source, Color.WHITE, Color.BLACK, Color.BLACK));
        addSlide(AppIntroFragment.newInstance("Ultra reliable", null, "The fastest Ethereum wallet experience on mobile.", null, R.mipmap.onboarding_rocket, Color.WHITE, Color.BLACK, Color.BLACK));

        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#3F51B5"));
        //setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done generateButton.
        //showSkipButton(true);
        //setProgressButtonEnabled(false);
        showSkipButton(true);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(true);
        setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);

        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done generateButton.
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
