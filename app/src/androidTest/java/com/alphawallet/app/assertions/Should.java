package com.alphawallet.app.assertions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.util.Helper.waitUntil;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.widget.TextView;

import com.alphawallet.app.R;

public class Should
{
    private static final int TIMEOUT_IN_SECONDS = 5 * 60;

    public static void shouldSee(String text)
    {
        onView(isRoot()).perform(waitUntil(withSubstring(text), TIMEOUT_IN_SECONDS));
    }

    public static void shouldNotSee(String text)
    {
        onView(isRoot()).perform(waitUntil(not(withSubstring(text)), TIMEOUT_IN_SECONDS));
    }

    public static void shouldNotSee(int id)
    {
        onView(isRoot()).perform(waitUntil(not(withId(id)), TIMEOUT_IN_SECONDS));
    }

    public static void shouldSee(int id)
    {
        onView(isRoot()).perform(waitUntil(withId(id), 10 * 60));
    }
}
