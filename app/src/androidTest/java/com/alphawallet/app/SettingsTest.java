package com.alphawallet.app;

import com.alphawallet.app.ui.SplashActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityScenarioRule
            = new ActivityScenarioRule<>(SplashActivity.class);

    @Before
    public void setUp() throws Exception {
        ensureAppStarted();
        gotoSettingsPage();
    }

    private void gotoSettingsPage() {
        onView(withText("CREATE A NEW WALLET")).perform(click());
        onView(withText("CLOSE")).perform(click());
        onView(withId(R.id.nav_settings)).perform(click());
    }

    private void ensureAppStarted() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test
    public void should_select_active_network() {
        ViewInteraction selectActiveNetworks = onView(withText("Select Active Networks"));
        selectActiveNetworks.perform(scrollTo(), click());
        onView(withId(R.id.mainnet_switch)).perform(click());
        onView(withText(R.string.action_enable_testnet)).perform(click());
        onView(withId(R.id.mainnet_switch)).check(matches(isNotChecked()));
        onView(withId(R.id.testnet_switch)).check(matches(isChecked()));
        onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(0, click()));
        pressBack();
        selectActiveNetworks.perform(click());
        onView(withId(R.id.testnet_switch)).check(matches(isChecked()));
    }
}