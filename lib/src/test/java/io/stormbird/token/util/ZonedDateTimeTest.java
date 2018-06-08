package io.stormbird.token.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class ZonedDateTimeTest {
    final long unixTime = 0;

    @Test
    public void DemonstrateBehaviourOfJave8ZonedDateTime() {
        java.time.LocalDateTime time = java.time.LocalDateTime.ofEpochSecond(unixTime, 0, java.time.ZoneOffset.of("+3"));
        java.time.ZoneId moscow = java.time.ZoneId.of("Europe/Moscow");
        java.time.ZonedDateTime timeInMoscow = java.time.ZonedDateTime.of(time, moscow);
        // timeInMoscow is the Epoch
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
        ZonedDateTime moscowTime = new ZonedDateTime(unixTime, TimeZone.getTimeZone("Europe/Moscow"));
        assertEquals(unixTime, moscowTime.toEpochSecond());
        assertEquals(3, moscowTime.getHour());
        assertEquals(0, moscowTime.getMinute());
    }

}
