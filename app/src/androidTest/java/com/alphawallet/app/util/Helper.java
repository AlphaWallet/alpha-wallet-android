package com.alphawallet.app.util;

import android.view.View;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.concurrent.TimeoutException;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

public class Helper {
    private static final int DEFAULT_TIMEOUT_IN_SECONDS = 10;

    public static ViewAction waitUntil(final int viewId, final Matcher<View> matcher) {
        return waitUntil(allOf(withId(viewId), matcher), DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static ViewAction waitUntil(final int viewId, final Matcher<View> matcher, int timeoutInSeconds) {
        return waitUntil(allOf(withId(viewId), matcher), timeoutInSeconds);
    }

    public static ViewAction waitUntil(Matcher<View> matcher) {
        return waitUntil(matcher, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static ViewAction waitUntil(Matcher<View> matcher, int timeoutInSeconds) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for view matches " + matcher.toString() + " during " + timeoutInSeconds + " seconds.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + timeoutInSeconds * 1000L;

                do {

                    for (View child : TreeIterables.breadthFirstViewTraversal(view.getRootView())) {
                        if (matcher.matches(child)) {
                            return;
                        }
                    }

                    uiController.loopMainThreadForAtLeast(50);
                }
                while (System.currentTimeMillis() < endTime);

                // timeout happens
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    public static void wait(int seconds) {
        onView(isRoot()).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait " + seconds + " seconds.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                uiController.loopMainThreadForAtLeast(seconds * 1000L);
            }
        });
    }

    public static void click(Matcher<View> matcher) {
//        Helper.wait(1); //slight pause
        onView(isRoot()).perform(Helper.waitUntil(Matchers.allOf(matcher, isDisplayed())));
        onView(matcher).perform(ViewActions.click(doNothing())); // if click executed as long press, do nothing and retry clicking
    }

    private static ViewAction doNothing() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Do nothing.";
            }

            @Override
            public void perform(UiController uiController, View view) {
            }
        };
    }
}
