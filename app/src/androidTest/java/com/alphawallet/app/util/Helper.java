package com.alphawallet.app.util;

import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;

public class Helper {
    private static final int DEFAULT_TIMEOUT_IN_MILLIS = 60000;

    public static ViewAction waitUntil(final int viewId, final Matcher<View> matcher) {
        return waitUntil(allOf(withId(viewId), matcher));
    }

    public static ViewAction waitUntil(Matcher<View> matcher) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait for view matches " + matcher.toString() + " during " + DEFAULT_TIMEOUT_IN_MILLIS + " millis.";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + DEFAULT_TIMEOUT_IN_MILLIS;

                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
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

    public static ViewAction getText(final int id) {
        return new ViewAction() {
            private CharSequence text;

            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "get text";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView textView = (TextView) view;
                text = textView.getText();
            }

            public CharSequence getText() {
                return text;
            }
        };
    }
}
