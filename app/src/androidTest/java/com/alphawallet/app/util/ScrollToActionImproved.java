package com.alphawallet.app.util;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;

import android.graphics.Rect;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.util.HumanReadables;

import org.hamcrest.Matcher;

/**
 * Created by JB on 4/04/2023.
 */
public class ScrollToActionImproved implements ViewAction
{
    @Override
    public Matcher<View> getConstraints() {
        return allOf(
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                isDescendantOfA(anyOf(
                        isAssignableFrom(ScrollView.class),
                        isAssignableFrom(HorizontalScrollView.class),
                        isAssignableFrom(NestedScrollView.class))
                )
        );
    }

    @Override
    public String getDescription()
    {
        return "scroll to view";
    }

    @Override
    public void perform(UiController uiController, View view)
    {
        if (isDisplayingAtLeast(90).matches(view)) {
            //View is already displayed
            return;
        }

        Rect rect = new Rect();
        view.getDrawingRect(rect);

        if (!view.requestRectangleOnScreen(rect, true)) {
            //Scrolling to view was requested, but none of the parents scrolled.
        }
        uiController.loopMainThreadUntilIdle();

        if (!isDisplayingAtLeast(90).matches(view)) {
            throw new PerformException.Builder()
                    .withActionDescription(getDescription())
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(
                            new RuntimeException(
                                    "Scrolling to view was attempted, but the view is not displayed"
                            )
                    )
                    .build();
        }
    }
}
