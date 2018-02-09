package com.wallet.crypto.alphawallet.entity;

/**
 * Created by James on 9/02/2018.
 */

import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.web3j.utils.Convert;

/**
 * This class encodes and decodes the bit field information in a ticket
 * test spec for int16:
 * 16 venues/dates, first 4 bits
 * left with 12 bits, can store 4096 seat/price zones
 * next 5 bits = 32 zones
 * final 7 bits = seat number in zone
 *
 * ALT: this represents index into intermediate database which holds Shangkai tickets
 *
 */
public class TicketDecode
{
//    public final String venue;
//    public final String date;
//    public final String seatZone;
//    public final int seatNumber;
//    public final BigInteger price;
//
//    public TicketDecode(short ticketID)
//    {
//        venue = lookupVenue(ticketID);
//        date = lookupDate(ticketID);
//        seatZone = lookupZone(ticketID);
//        seatNumber = lookupSeatId(ticketID);
//        price = getPrice(ticketID);
//    }

    public static String getVenue(int ticketId)
    {
        int venueID = getVenueID(ticketId);
        String venue = "unknown";
        if (venueID < venues.length)
        {
            venue = venues[venueID];
        }
        return venue;
    }

    public static String getDate(int ticketId)
    {
        int venueID = getVenueID(ticketId);
        String date = "unknown";
        if (venueID < dates.length)
        {
            date = dates[venueID];
        }
        return date;
    }

    public static String getZone(int ticketID)
    {
        char zone = 'A';
        int zoneId = getZoneID(ticketID);
        zone += zoneId;
        return "Zone " + zone;
    }

    public static String getSeatId(short ticketID)
    {
        int bitmask = (1 << 8) - 1;
        int seatId = (ticketID & (bitmask)); //mask with bottom 7 bits
        return "Seat " + seatId;
    }

    //get price in Wei
    public static BigInteger getPrice(int ticketId)
    {
        //get 0.001 eth in wei
        BigInteger milliEth = Convert.toWei("1", Convert.Unit.FINNEY).toBigInteger();
        //this will be price in wei
        double price = 100.0 + ((double)getZoneID(ticketId))/2.0 + ((double)getVenueID(ticketId) / 2.0);
        long dPrice = (long) price;
        BigInteger bPrice = BigInteger.valueOf(dPrice).multiply(milliEth);
        return bPrice;
    }
    public static String getPriceString(int ticketId)
    {
        BigInteger price = getPrice(ticketId);
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        String formatted = df.format(price.intValue());
        return formatted;
    }

    private static int getVenueID(int ticketID)
    {
        return (ticketID >> 12);
    }

    private static int getZoneID(int ticketID)
    {
        int zoneID = (ticketID >> 7);
        int bitmask = (1 << 6) - 1;
        zoneID = (zoneID & (bitmask)); //mask with bottom 5 bits
        return zoneID;
    }


    //hardcoded DB
//    private static final String venues[] = {
//            "Pasir Ris Park",
//            "Moscow Red Square",
//            "Old Changi Hospital",
//            "Port Hacking",
//            "CBA Perth",
//            "Brixton Academy",
//            "'Pitz' Milton Keynes",
//            "Birmingham NEC",
//            "Bristol Bierkeller",
//            "Tweed Heads Twin Towns"
//    };

    private static final String venues[] = {
            "Barclays Pasir Ris Park",
            "RBS Forbidden City",
            "Rabobank Old Changi Hospital",
            "UOB Hacking",
            "CBA Perth",
            "Orange Academy",
            "StGeorge 'Pitz'",
            "NAB NEC",
            "McQuarrie Bierkeller",
            "HSBC Twin Towns"
    };

    private static final String dates[] = {
            "28/02/2018",
            "05/03/2018",
            "20/03/2018",
            "31/03/2018",
            "01/04/2018",
            "20/04/2018",
            "30/04/2018",
            "05/05/2018",
            "15/05/2018",
            "30/05/2018",
    };

    //Utility functions for generation
    //generate a string of ID's for populating a contract
    public static String generateTicketIDList(int venueCount, int zoneCount, int seatCount)
    {
        StringBuilder sb = new StringBuilder();//
        sb.append("[");
        boolean first = true;
        for (int j = 0; j < venueCount; j++)
        {
            int venuePart = j << 12;
            for (int i = 0; i < zoneCount; i++)
            {
                int zonePart = i << 7;
                for (int k = 0; k < seatCount; k++)
                {
                    int value = venuePart + zonePart + k;
                    if (!first) sb.append(", ");
                    sb.append(String.valueOf(value));
                    first = false;
                }
            }
        }

        sb.append("]");

        System.out.println(sb.toString());

        return sb.toString();
    }
}
