package com.alphawallet.app.util;

import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

public class GetTextAction implements ViewAction {

    private CharSequence text;

    @Override public Matcher<View> getConstraints() {
        return isAssignableFrom(TextView.class);
    }

    @Override public String getDescription() {
        return "get text";
    }

    @Override public void perform(UiController uiController, View view) {
        TextView textView = (TextView) view;
        text = textView.getText();
    }

    public CharSequence getText() {
        return text;
    }
}