package com.alphawallet.app.util;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience StringBuilder that builds styled messages to display data in human readable form to the user
 *
 * Created by JB on 28/08/2020.
 */
public class StyledStringBuilder extends SpannableStringBuilder
{
    private List<SpanType> spanners = new ArrayList<>();
    private int startIndex;
    private int startGroup = -1;

    @Override
    public SpannableStringBuilder append(CharSequence text) {
        startIndex = this.length();
        SpannableStringBuilder replace = super.append(text);
        return replace;
    }

    public SpannableStringBuilder setStyle(StyleSpan style)
    {
        int useStartIndex = startGroup != -1 ? startGroup : startIndex;
        spanners.add(new SpanType(useStartIndex, this.length(), style));
        startGroup = -1;
        return this;
    }

    public SpannableStringBuilder startStyleGroup()
    {
        startGroup = this.length();
        return this;
    }

    public void applyStyles()
    {
        for (SpanType s : spanners)
        {
            setSpan(s.style, s.begin, s.end, Spanned.SPAN_POINT_POINT);
        }
    }

    private static class SpanType
    {
        int begin;
        int end;
        StyleSpan style;

        public SpanType(int begin, int end, StyleSpan style)
        {
            this.begin = begin;
            this.end = end;
            this.style = style;
        }
    }
}
