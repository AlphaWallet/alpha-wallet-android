package com.alphawallet.app;


import static androidx.test.espresso.Espresso.setFailureHandler;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.alphawallet.app.steps.Steps.closeSecurityWarning;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.app.util.CustomFailureHandler;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public abstract class BaseE2ETest
{
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            setFailureHandler(new CustomFailureHandler(description.getMethodName(), getInstrumentation().getTargetContext()));
        }
    };

    @Rule
    public ActivityScenarioRule<SplashActivity> activityScenarioRule
            = new ActivityScenarioRule<>(SplashActivity.class);

    @Before
    public void setUp() throws UiObjectNotFoundException
    {
        dismissANRSystemDialog();
        closeSecurityWarning();
    }

    private void dismissANRSystemDialog() throws UiObjectNotFoundException
    {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        UiObject waitButton = device.findObject(new UiSelector().textContains("wait"));
        if (waitButton.exists()) {
            waitButton.click();
        }
    }
}
