package com.alphawallet.token.util;

import com.alphawallet.token.entity.NonFungibleToken;

import java.text.ParseException;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by James on 11/02/2019.
 * Stormbird in Singapore
 */
public abstract class DateTimeFactory
{
    public static DateTime getDateTime(long unixTime, TimeZone timezone)
    {
        return new ZonedDateTime(unixTime, timezone);
    }

    public static DateTime getDateTime(NonFungibleToken.Attribute timeAttr) throws ParseException, IllegalArgumentException
    {
        String eventTimeText = timeAttr.text;
        if (eventTimeText != null)
        {
            //there was a specific timezone set in the XML definition file, use this
            return initZonedTime(eventTimeText);
        }
        else
        {
            //No timezone specified, assume time in GMT
            return new GeneralDateTime(timeAttr);
        }
    }

    public static DateTime getDateTime(String time) throws ParseException, IllegalArgumentException
    {
        return initZonedTime(time);
    }

    public static DateTime getCurrentTime()
    {
        return new GeneralDateTime(String.valueOf(System.currentTimeMillis()));
    }

    private static DateTime initZonedTime(String time) throws ParseException, IllegalArgumentException
    {
        Pattern p = Pattern.compile("(\\+\\d{4}|\\-\\d{4})");
        Matcher m = p.matcher(time);
        if (m.find())
        {
            return new ZonedDateTime(time, m);
        }
        else if (isNumeric(time))
        {
            return new GeneralDateTime(time);
        }
        else
        {
            //drop through, use current time
            return new GeneralDateTime(String.valueOf(System.currentTimeMillis()));
        }
    }

    private static boolean isNumeric(String testStr)
    {
        boolean result = false;
        if (testStr != null && testStr.length() > 0)
        {
            result = true;
            for (int i = 0; i < testStr.length(); i++)
            {
                char c = testStr.charAt(i);
                if (!Character.isDigit(c))
                {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }
}
