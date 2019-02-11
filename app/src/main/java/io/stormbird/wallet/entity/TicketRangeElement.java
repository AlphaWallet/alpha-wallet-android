package io.stormbird.wallet.entity;

import java.math.BigInteger;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.service.AssetDefinitionService;

public class TicketRangeElement
{
    public BigInteger id;
    public short category;
    public int ticketNumber;
    public short venue;
    public short match;
    public long time = 0;

    public TicketRangeElement(AssetDefinitionService assetService, Token token, BigInteger v)
    {
        NonFungibleToken nft = assetService.getNonFungibleToken(token.getAddress(), v);
        if (nft != null)
        {
            if (nft.getAttribute("numero") != null) ticketNumber = nft.getAttribute("numero").value.intValue();
            if (nft.getAttribute("category") != null) category = (short) nft.getAttribute("category").value.intValue();
            if (nft.getAttribute("match") != null) match = (short) nft.getAttribute("match").value.intValue();
            if (nft.getAttribute("venue") != null) venue = (short) nft.getAttribute("venue").value.intValue();
            if (nft.getAttribute("time") != null)
            {
                try
                {
                    DateTime eventTime = DateTimeFactory.getDateTime(nft.getAttribute("time"));
                    time = eventTime.toEpochSecond();
                }
                catch (ParseException e)
                {
                    time = 0;
                }
            }
        }
    }

    public static void sortElements(List<TicketRangeElement> elementList)
    {
        Collections.sort(elementList, (e1, e2) -> {
            long w1 = ((long)e1.venue<<32) + ((long)e1.match<<24) + ((long)e1.category<<16) + e1.ticketNumber;
            long w2 = ((long)e2.venue<<32) + ((long)e2.match<<24) + ((long)e2.category<<16) + e2.ticketNumber;
            if (e1.time != 0 && e2.time != 0)
            {
                w1 = e1.time;
                w2 = e2.time;
            }
            if (w1 > w2) return 1;
            if (w1 < w2) return -1;
            return 0;
        });
    }
}
