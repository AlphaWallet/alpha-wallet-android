package com.alphawallet.app;

import org.junit.Test;
import org.web3j.utils.Numeric;

public class AssetGenerationTest
{
    @Test
    public void GenerateAssetSet()
    {
        //03 04 5AF6D740 474252 415247 01 01 04d2
        final int venues = 4  ;
        final int locales = 11 ; //no idea if venues and locales match up ???
        final String[] countries = { "ALB", "ASM", "AUS", "AZE", "GBR", "USA", "NZL", "FRA", "DEU", "GRL", "ISL", "IMN", "PRK", "BTN", "CHN", "RUS" };
        long startTime = 1526126400 ;
        final int matches = 2   ;
        final int categories = 2;
        final int seatCount = 5;

        int currentMatch = 1;

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        int ticketNumber = 1;

        for (int j = 0; j < venues; j++)
        {
            String venuePart = String.format("%02x", j+1);
            String localePart = String.format("%02x", (int)(Math.random()*locales) + 1);
            String timeStr = String.format("%08x", startTime);
            for (int i = 0; i < matches; i++)
            {
                int rand1 = (int)(Math.random()*countries.length);
                int rand2 = (int)(Math.random()*countries.length);
                while (rand1 == rand2) rand2 = (int)(Math.random()*countries.length);

                String countryA = Numeric.toHexString(countries[rand1].getBytes(),0,3,false);
                String countryB = Numeric.toHexString(countries[rand2].getBytes(),0,3,false);
                String match = String.format("%02x", currentMatch++);

                for (int c = 0; c < categories; c++)
                {
                    String catStr = String.format("%02x", c + 1);
                    for (int k = 0; k < seatCount; k++)
                    {
                        String seatNumber = String.format("%04x", ticketNumber);
                        if (!first) sb.append(", ");
                        sb.append("\"0x");
                        sb.append(localePart);  //2
                        sb.append(venuePart);   //2
                        sb.append(timeStr);     //8
                        sb.append(countryA);    //6
                        sb.append(countryB);    //6
                        sb.append(match);       //2
                        sb.append(catStr);      //2
                        sb.append(seatNumber);  //4
                        sb.append("\"");
                                               //32 :)
                        first = false;
                        ticketNumber++;
                    }
                }
            }

            //add 2 days
            startTime += (60 * 60 * 24 * 2);
        }

        sb.append("]");

        System.out.println(sb.toString());
    }


    @Test
    public void GenerateSydEthTicketData()
    {
        final int ticketCount = 256;
        long startTime = 1525341600 ;

        //String venuePart = String.format("%02x", 255);
        //String localePart = String.format("%02x", 255);
        //String timeStr = String.format("%08x", startTime);
        //String match = String.format("%02x", 0);
        //String country = String.format("%06x", 0);


        String city = String.format("%02x", 1);
        String spacer = "00000000000000000000000000";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;

        for (int i = 1; i < 8; i++)
        {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"0x");
            String category = String.format("%02x", 1);
            String number = String.format("%04x", 0);
            sb.append(city);
            sb.append(city);
            sb.append(category);
            sb.append(number);
            sb.append("\"");
        }
        for (int i = 1; i < 8; i++)
        {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"0x");
            String category = String.format("%02x", 2);
            String number = String.format("%04x", 0);
            sb.append(city);
            sb.append(city);
            sb.append(category);
            sb.append(number);
            sb.append("\"");
        }
        for (int i = 1; i < 8; i++)
        {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"0x");
            String category = String.format("%02x", 3);
            String number = String.format("%04x", 0);
            sb.append(city);
            sb.append(city);
            sb.append(category);
            sb.append(number);
            sb.append("\"");
        }
        for (int i = 1; i < 10; i++)
        {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"0x");
            String category = String.format("%02x", 4);
            String number = String.format("%04x", 0);
            sb.append(city);
            sb.append(city);
            sb.append(category);
            sb.append(number);
            sb.append("\"");
        }
//
//        for (int i = 1; i <= ticketCount; i++)
//        {
//            if (!first) sb.append(",");
//            first = false;
//            sb.append("\"0x");
//            //sb.append("\"0x00000000000000000000000000000000");
//            //sb.append(localePart);  //2
//            //sb.append(venuePart);   //2
//            //sb.append(timeStr);     //8
//
//            //String seatNumber = String.format("%04x", i);
//            //sb.append(country);  //6
//            //sb.append(country);  //6
//            //sb.append(match);    //2
//            //sb.append(match);    //2
//            String category = String.format("%02", )
//            sb.append();  //4
//            sb.append("\"");
//        }
        sb.append("]");

        System.out.println(sb.toString());
    }
}

//["0x07015af6d74050524b41555301010001", "0x07015af6d74050524b41555301010002", "0x07015af6d74050524b41555301010003", "0x07015af6d74050524b41555301010004", "0x07015af6d74050524b41555301010005", "0x07015af6d74050524b41555301020006", "0x07015af6d74050524b41555301020007", "0x07015af6d74050524b41555301020008", "0x07015af6d74050524b41555301020009", "0x07015af6d74050524b4155530102000a", "0x07015af6d7404742524e5a4c0201000b", "0x07015af6d7404742524e5a4c0201000c", "0x07015af6d7404742524e5a4c0201000d", "0x07015af6d7404742524e5a4c0201000e", "0x07015af6d7404742524e5a4c0201000f", "0x07015af6d7404742524e5a4c02020010", "0x07015af6d7404742524e5a4c02020011", "0x07015af6d7404742524e5a4c02020012", "0x07015af6d7404742524e5a4c02020013", "0x07015af6d7404742524e5a4c02020014", "0x02025af97a40494d4e47425203010015", "0x02025af97a40494d4e47425203010016", "0x02025af97a40494d4e47425203010017", "0x02025af97a40494d4e47425203010018", "0x02025af97a40494d4e47425203010019", "0x02025af97a40494d4e4742520302001a", "0x02025af97a40494d4e4742520302001b", "0x02025af97a40494d4e4742520302001c", "0x02025af97a40494d4e4742520302001d", "0x02025af97a40494d4e4742520302001e", "0x02025af97a404e5a4c42544e0401001f", "0x02025af97a404e5a4c42544e04010020", "0x02025af97a404e5a4c42544e04010021", "0x02025af97a404e5a4c42544e04010022", "0x02025af97a404e5a4c42544e04010023", "0x02025af97a404e5a4c42544e04020024", "0x02025af97a404e5a4c42544e04020025", "0x02025af97a404e5a4c42544e04020026", "0x02025af97a404e5a4c42544e04020027", "0x02025af97a404e5a4c42544e04020028", "0x0b035afc1d40414c4241534d05010029", "0x0b035afc1d40414c4241534d0501002a", "0x0b035afc1d40414c4241534d0501002b", "0x0b035afc1d40414c4241534d0501002c", "0x0b035afc1d40414c4241534d0501002d", "0x0b035afc1d40414c4241534d0502002e", "0x0b035afc1d40414c4241534d0502002f", "0x0b035afc1d40414c4241534d05020030", "0x0b035afc1d40414c4241534d05020031", "0x0b035afc1d40414c4241534d05020032", "0x0b035afc1d4042544e46524106010033", "0x0b035afc1d4042544e46524106010034", "0x0b035afc1d4042544e46524106010035", "0x0b035afc1d4042544e46524106010036", "0x0b035afc1d4042544e46524106010037", "0x0b035afc1d4042544e46524106020038", "0x0b035afc1d4042544e46524106020039", "0x0b035afc1d4042544e4652410602003a", "0x0b035afc1d4042544e4652410602003b", "0x0b035afc1d4042544e4652410602003c", "0x03045afec04042544e4e5a4c0701003d", "0x03045afec04042544e4e5a4c0701003e", "0x03045afec04042544e4e5a4c0701003f", "0x03045afec04042544e4e5a4c07010040", "0x03045afec04042544e4e5a4c07010041", "0x03045afec04042544e4e5a4c07020042", "0x03045afec04042544e4e5a4c07020043", "0x03045afec04042544e4e5a4c07020044", "0x03045afec04042544e4e5a4c07020045", "0x03045afec04042544e4e5a4c07020046", "0x03045afec04043484e52555308010047", "0x03045afec04043484e52555308010048", "0x03045afec04043484e52555308010049", "0x03045afec04043484e5255530801004a", "0x03045afec04043484e5255530801004b", "0x03045afec04043484e5255530802004c", "0x03045afec04043484e5255530802004d", "0x03045afec04043484e5255530802004e", "0x03045afec04043484e5255530802004f", "0x03045afec04043484e52555308020050"],  "AlphaWallet Test Tickets", "AWTT", "0x007bEe82BDd9e866b2bd114780a47f2261C684E3", "0x007bEe82BDd9e866b2bd114780a47f2261C684E3"
