package io.stormbird.token.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/*
 * by Weiwu, 2018. Modeled after Java8's ZonedDateTime, intended to be
 * replaced by Java8's ZonedDateTime as soon as Android 8.0 gets popular
 */
public class ZonedDateTime {
    private long time;
    private TimeZone timezone;
    private final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");


    /* For anyone deleting this class to use Java8 ZonedDateTime:
     *
     * A LITTLE DEVIL LIES IN THE DETAIL HERE
     *
     * In Java8, ZonedDateTime.of(LocalDateTime time, ZoneID id) works
     * by taking the year, month, day, hour, minute, second tuple from
     * LocalDateTime, stripping off the timezone information, then
     * treat it as if the tuple is in ZoneID. i.e. no timezone offset applied
     * unless later toEpochSecond() is used.
     *
     * In this ZonedDateTime which uses a constructor instead of a
     * static of() method, unixTime always represent Unix Time, that
     * is, the number of seconds since Epoch as the Epoch happens at UTC.
     * Apparently timezone offset is applied in format()
     */
    public ZonedDateTime(long unixTime, TimeZone timezone) {
        this.time = unixTime * 1000L;
        this.timezone = timezone;
    }

    /* EVERY FUNCTION BELOW ARE SET OUT IN JAVA8 */

    public long toEpochSecond() {
        return time/1000L;
    }

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

    public String toString() {
        return format(ISO8601);
    }

    public String format(DateFormat format) {
        format.setTimeZone(timezone);
        return format.format(new Date(time));
    }
}
