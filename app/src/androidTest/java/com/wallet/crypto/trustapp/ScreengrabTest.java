package com.wallet.crypto.trustapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.wallet.crypto.trustapp.views.TransactionListActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

//import tools.fastlane.screengrab.Screengrab;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ScreengrabTest {

    @Rule
    public ActivityTestRule<TransactionListActivity> activityRule = new ActivityTestRule<>(TransactionListActivity.class);

    @Test
    public void takeTestScreenshot() throws Exception {
        //Screengrab.screenshot("test_screenshot");

        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
    }
}
