package com.alphawallet.token.util;

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
        //assertEquals(ISO8601, timeInMoscow.toString()); //TODO: ZonedDatTime.toString isn't implemented - needs to explicitly overloaded for this test to work
        assertEquals(unixTime, timeInMoscow.toEpochSecond());
        assertEquals(3, timeInMoscow.getHour());
        assertEquals(0, timeInMoscow.getMinute());
    }

    @Test
    public void ZonedDateTimeCanBeCreatedFromGeneralizedTime() throws ParseException {
        DateTime timeInMoscow = DateTimeFactory.getDateTime(GeneralizedTime);
        //assertEquals(ISO8601, timeInMoscow.toString()); //TODO: ZonedDatTime.toString isn't implemented - needs to explicitly overloaded for this test to work
        assertEquals(unixTime, timeInMoscow.toEpochSecond());
        assertEquals(3, timeInMoscow.getHour());
        assertEquals(0, timeInMoscow.getMinute());

        DateTime timeInMoscow2 = DateTimeFactory.getDateTime("19700101030101+0300");
        //assertEquals("1970-01-01T03:01+03:00", timeInMoscow2.toString()); //TODO: ZonedDatTime.toString isn't implemented - needs to explicitly overloaded for this test to work
        assertEquals(61, timeInMoscow2.toEpochSecond());
        assertEquals(3, timeInMoscow2.getHour());
        assertEquals(1, timeInMoscow2.getMinute());

        DateTime timeInAzores = DateTimeFactory.getDateTime("19700101030000-0100");
        //assertEquals("1970-01-01T03:00-01:00", timeInAzores.toString()); //TODO: ZonedDatTime.toString isn't implemented - needs to explicitly overloaded for this test to work
        assertEquals(14400, timeInAzores.toEpochSecond()); //this time is relatively 4 hrs from Moscow
        assertEquals(3, timeInAzores.getHour());
        assertEquals(0, timeInAzores.getMinute());

        // ADDING MORE TESTS: READ VERY CAREFULLY
        //
        // IF YOU ADD MORE TESTS HERE, AND YOU USE 'assertEquals("1970 ...", timeInTimbuktu.toString());'
        // THEN THE TEST _WILL FAIL_. IF YOU WANT TO USE THIS NOTATION THEN PLEASE ADD A toString() OVERRIDE.
        // NEXT PERSON WHO DOES THIS WITHOUT WRITING THE OVERRIDE WILL GET USED FOR BOXING PRACTICE IN LIU OF BOB
        // PHILLIP WILL REVOKE YOUR LOVE TOKENS AND NOT ISSUE YOU ANY MORE. AND HE'LL TURN HIS BACK ON YOU.
    }
}
