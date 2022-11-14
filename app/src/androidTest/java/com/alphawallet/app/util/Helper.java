package com.alphawallet.app.util;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.AllOf.allOf;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;

import android.view.inputmethod.InputMethodManager;

import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.concurrent.TimeoutException;
import com.alphawallet.app.R;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.concurrent.TimeoutException;

public class Helper
{
    private static final int DEFAULT_TIMEOUT_IN_SECONDS = 30;

    public static ViewAction waitUntil(final int viewId, final Matcher<View> matcher)
    {
        return waitUntil(allOf(withId(viewId), matcher), DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static ViewAction waitUntil(final int viewId, final Matcher<View> matcher, int timeoutInSeconds)
    {
        return waitUntil(allOf(withId(viewId), matcher), timeoutInSeconds);
    }

    public static ViewAction waitUntil(Matcher<View> matcher)
    {
        return waitUntil(matcher, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public static ViewAction waitUntil(Matcher<View> matcher, int timeoutInSeconds)
    {
        return new ViewAction()
        {
            @Override
            public Matcher<View> getConstraints()
            {
                return isRoot();
            }

            @Override
            public String getDescription()
            {
                return "wait for view matches " + matcher.toString() + " during " + timeoutInSeconds + " seconds.";
            }

            @Override
            public void perform(final UiController uiController, final View view)
            {
                uiController.loopMainThreadUntilIdle();
                final long startTime = System.currentTimeMillis();
                final long endTime = startTime + timeoutInSeconds * 1000L;

                do
                {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view.getRootView()))
                    {
                        if (matcher.matches(child))
                        {
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

    public static void wait(int seconds)
    {
        onView(isRoot()).perform(new ViewAction()
        {
            @Override
            public Matcher<View> getConstraints()
            {
                return isRoot();
            }

            @Override
            public String getDescription()
            {
                return "wait " + seconds + " seconds.";
            }

            @Override
            public void perform(final UiController uiController, final View view)
            {
                uiController.loopMainThreadUntilIdle();
                uiController.loopMainThreadForAtLeast(seconds * 1000L);
            }
        });
    }

    public static void click(Matcher<View> matcher, int timeoutInSeconds)
    {
        onView(isRoot()).perform(Helper.waitUntil(Matchers.allOf(matcher, isDisplayed()), timeoutInSeconds));
        onView(matcher).perform(ViewActions.click(doNothing())); // if click executed as long press, do nothing and retry clicking
    }

    public static void click(Matcher<View> matcher)
    {
//        Helper.wait(1); //slight pause
        onView(isRoot()).perform(Helper.waitUntil(Matchers.allOf(matcher, isDisplayed())));
        onView(matcher).perform(ViewActions.click(doNothing())); // if click executed as long press, do nothing and retry clicking
    }

    private static ViewAction doNothing()
    {
        return new ViewAction()
        {
            @Override
            public Matcher<View> getConstraints()
            {
                return isDisplayed();
            }

            @Override
            public String getDescription()
            {
                return "Do nothing.";
            }

            @Override
            public void perform(UiController uiController, View view)
            {
            }
        };
    }

    public static void clickListItem(int list, Matcher matcher)
    {
        for (int i = 0; i < 50; i++)
        {
            try
            {
                click(matcher, 0);
                return;
            }
            catch (Exception e)
            {
                scrollDown(list);
            }
        }
        throw new RuntimeException("Can not find " + matcher.toString());
    }

    private static void scrollDown(int list)
    {
        onView(withId(list)).perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN));
        Helper.wait(1);
    }

    public static void waitForLoadingComplete(String title)
    {
        waitUntilShown(title);
        waitUntilDismissed(title);
    }

    private static void waitUntilDismissed(String title)
    {
        while (true)
        {
            try
            {
                onView(withSubstring(title)).inRoot(isDialog()).check(matches(not(ViewMatchers.isDisplayed())));
            }
            catch (Error e)
            {
                // Dialog still showing
                wait(1);
            }
            catch (Exception e)
            {
                // Dialog dismissed
                break;
            }
        }
    }

    private static void waitUntilShown(String title)
    {
        while (true)
        {
            try
            {
                onView(withSubstring(title)).inRoot(isDialog()).check(matches(ViewMatchers.isDisplayed()));
                break;
            }
            catch (Error | Exception e)
            {
                wait(1);
            }
        }
    }

    public static boolean isSoftKeyboardShown(Context context)
    {
        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
        return imm.isAcceptingText();
    }

    public static void waitUntilLoaded()
    {
        waitStart();
        waitComplete();
    }

    private static void waitComplete()
    {
        for (int i = 0; i < DEFAULT_TIMEOUT_IN_SECONDS; i++)
        {
            try
            {
                onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())));
                break;
            }
            catch (Error | Exception e)
            {
                Helper.wait(1);
            }
        }
    }

    private static void waitStart()
    {
        for (int i = 0; i < DEFAULT_TIMEOUT_IN_SECONDS; i++)
        {
            try
            {
                onView(withId(R.id.progressBar)).check(matches(isDisplayed()));
                break;
            }
            catch (Error | Exception e)
            {
                Helper.wait(1);
            }
        }
    }
}
