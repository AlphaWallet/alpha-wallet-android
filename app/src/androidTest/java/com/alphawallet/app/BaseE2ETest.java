package com.alphawallet.app;


import static androidx.test.espresso.Espresso.setFailureHandler;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.app.util.CustomFailureHandler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class BaseE2ETest
{
    @Before
    public void setUp() {
        setFailureHandler(new CustomFailureHandler(InstrumentationRegistry.getInstrumentation().getTargetContext()));
    }

    @Rule
    public ActivityScenarioRule<SplashActivity> activityScenarioRule
            = new ActivityScenarioRule<>(SplashActivity.class);
}
