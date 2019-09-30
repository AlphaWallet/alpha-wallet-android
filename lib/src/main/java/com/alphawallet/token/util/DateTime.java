package com.alphawallet.token.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by James on 11/02/2019.
 * Stormbird in Singapore
 */
public abstract class DateTime
{
    protected long time;
    protected TimeZone timezone;
    protected boolean isZoned = false;

    public int getHour() {
        /* you can't just do this:
        return new Date(time + offset).getHours() - 1;
        because Date applies the local (the JRE's) timezone
         */
        SimpleDateFormat format = new SimpleDateFormat("H");
        return Integer.valueOf(format(format));
    }

    public int getMinute() {
        SimpleDateFormat format = new SimpleDateFormat("m");
        return Integer.valueOf(format(format));
    }

    public String format(DateFormat format) {
        format.setTimeZone(timezone);
        return format.format(new Date(time));
    }

    public boolean isZoned()
    {
        return isZoned;
    }

    /* EVERY FUNCTION BELOW ARE SET OUT IN JAVA8 */

    public long toEpochSecond() {
        return time/1000L;
    }

    public long toEpoch()
    {
        return time;
    }
}
