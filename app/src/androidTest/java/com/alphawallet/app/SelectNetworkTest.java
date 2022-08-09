package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.widget.TextView;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.alphawallet.app.ui.SplashActivity;

import org.junit.Rule;
import org.junit.Test;

public class SelectNetworkTest {
    @Rule
    public ActivityScenarioRule<SplashActivity> activityScenarioRule
            = new ActivityScenarioRule<>(SplashActivity.class);
    
    @Test
    public void should_select_network_when_first_create_wallet() {
        onView(withText(R.string.ok)).perform(ViewActions.click());
        onView(withId(R.id.button_create)).perform(ViewActions.click());
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar))))
                .check(matches(withText("Select Active Networks")));
    }
}
