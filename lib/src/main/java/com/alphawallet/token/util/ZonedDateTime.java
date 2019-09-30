package com.alphawallet.token.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
/*
 * by Weiwu, 2018. Modeled after Java8's ZonedDateTime, intended to be
 * replaced by Java8's ZonedDateTime as soon as Android 8.0 gets popular
 */
public class ZonedDateTime extends DateTime
{
    //private final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX");


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
    ZonedDateTime(long unixTime, TimeZone timezone) {
        this.time = unixTime * 1000L;
        this.timezone = timezone;
        isZoned = true;
    }

    ZonedDateTime(String time, Matcher m) throws ParseException, IllegalArgumentException
    {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyyMMddHHmmssZZZZ");
        this.timezone = TimeZone.getTimeZone("GMT"+m.group(1));
        isoFormat.setTimeZone(this.timezone);
        Date date = isoFormat.parse(time);
        this.time = date.getTime();
        isZoned = true;
    }
}
