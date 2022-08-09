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
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import android.widget.TextView;

import com.alphawallet.app.R;

public class Should
{
    public static void shouldSee(String text)
    {
        onView(isRoot()).perform(waitUntil(withSubstring(text), 10 * 60));
    }

    public static void shouldSee(int id)
    {
        onView(isRoot()).perform(waitUntil(withId(id), 10 * 60));
    }

    public static void shouldSeeTitle(String title)
    {
        onView(allOf(instanceOf(TextView.class), withParent(withId(R.id.toolbar))))
                .check(matches(withText(title)));
    }
}
