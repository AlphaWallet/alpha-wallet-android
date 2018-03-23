package io.awallet.crypto.alphawallet.entity;

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

    public static String getName()
    {
        return "World Series Baseball";
    }

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

    public static String getZone(int ticketId)
    {
        char zone = 'A';
        int zoneId = getZoneID(ticketId);
        zone += zoneId;
        return "Zone " + zone;
    }

    public static char getZoneChar(int ticketId)
    {
        char zone = 'A';
        int zoneId = getZoneID(ticketId);
        zone += zoneId;
        return zone;
    }

    public static String getSeatId(int ticketId)
    {
        int modifier = 1;
        if (getZoneID(ticketId) == 0 && getVenueID(ticketId) == 0) modifier = 0;
        int bitmask = (1 << 7) - 1;
        int seatId = (ticketId & (bitmask)) + modifier; //mask with bottom 7 bits, add 1 except for the first run of tickets (because id 0 is a special case)
        return "Seat " + seatId;
    }

    public static int getSeatIdInt(int ticketId)
    {
        int modifier = 1;
        if (getZoneID(ticketId) == 0 && getVenueID(ticketId) == 0) modifier = 0;
        int bitmask = (1 << 7) - 1;
        return (ticketId & (bitmask)) + modifier; //mask with bottom 7 bits, add 1 except for the first run of tickets (because id 0 is a special case)
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
        int bitmask = (1 << 5) - 1;
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
            "Pasir Ris Park",
            "Bukit Panjang",
            "Old Changi Hospital",
            "Port Hacking",
            "Wogga Wogga",
            "Orange Academy",
            "Woolongabba",
            "Darling Harbour Plaza",
            "McQuarrie Park",
            "Tampines Mall"
    };

    private static final String dates[] = {
            "20 Mar 2018",
            "31 Mar 2018",
            "01 Apr 2018",
            "20 Apr 2018",
            "30 Apr 2018",
            "05 May 2018",
            "15 May 2018",
            "30 May 2018",
            "05 Jun 2018",
            "28 Jun 2018",
    };

    private static String issuer = "Shengkai";

    //Utility functions for generation
    //generate a string of ID's for populating a contract
    public static String generateTicketIDList(int venueCount, int zoneCount, int seatCount)
    {
        StringBuilder sb = new StringBuilder();
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

    public static String getIssuer() {
        return issuer;
    }
}
