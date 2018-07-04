package io.stormbird.token.util;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ZonedDateTimeTest {
    final long unixTime = 0;
    final String ISO8601 = "1970-01-01T03:00+03:00";
    final String GeneralizedTime = "19700101030000+0300";

    @Test
    public void DemonstrateBehaviourOfJave8ZonedDateTime() {
        java.time.LocalDateTime time = java.time.LocalDateTime.ofEpochSecond(unixTime, 0, java.time.ZoneOffset.of("+3"));
        java.time.ZoneId moscow = java.time.ZoneId.of("Europe/Moscow");
        java.time.ZonedDateTime timeInMoscow = java.time.ZonedDateTime.of(time, moscow);
        assertEquals(ISO8601, timeInMoscow.toString().substring(0, ISO8601.length()));
        // timeInMoscow is at the Epoch
        assertEquals(unixTime, timeInMoscow.toEpochSecond());
        // yet the hour value read from it is based on Moscow time.
        assertEquals(3, timeInMoscow.getHour());
    }

    @Test
    public void OurZonedDatedTimeShouldBehaveAlike() {
        // what time was it in Moscow at Epoch?
        Date epoch = new Date(0);
        SimpleDateFormat format = new SimpleDateFormat("hh:mm", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
        assertEquals("03:00", format.format(epoch));

        // okay let's verify this with the ZonedDateTime
        ZonedDateTime timeInMoscow = new ZonedDateTime(unixTime, TimeZone.getTimeZone("Europe/Moscow"));
        assertEquals(ISO8601, timeInMoscow.toString());
        assertEquals(unixTime, timeInMoscow.toEpochSecond());
        assertEquals(3, timeInMoscow.getHour());
        assertEquals(0, timeInMoscow.getMinute());
    }

    @Test
    public void ZonedDateTimeCanBeCreatedFromGeneralizedTime() throws ParseException {
        ZonedDateTime timeInMoscow = new ZonedDateTime(GeneralizedTime);
        assertEquals(ISO8601, timeInMoscow.toString());
        assertEquals(unixTime, timeInMoscow.toEpochSecond());
        assertEquals(3, timeInMoscow.getHour());
        assertEquals(0, timeInMoscow.getMinute());

        ZonedDateTime timeInMoscow2 = new ZonedDateTime("19700101030101+0300");
        assertEquals("1970-01-01T03:01+03:00", timeInMoscow2.toString());
        assertEquals(61, timeInMoscow2.toEpochSecond());
        assertEquals(3, timeInMoscow2.getHour());
        assertEquals(1, timeInMoscow2.getMinute());
    }

}
