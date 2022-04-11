package com.alphawallet.app.assertions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static com.alphawallet.app.util.Helper.waitUntil;

public class Should
{
    public static void shouldSee(String text)
    {
        onView(isRoot()).perform(waitUntil(withSubstring(text), 10 * 60));
    }
}
