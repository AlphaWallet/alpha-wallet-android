package io.awallet.crypto.alphawallet.repository.entity;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

/**
 * Created by weiwu on 1/3/18.  Each NonFungibleToken is a
 * non-fungible token identified by a byte32 tokenID (other forms of
 * IDs may be added if tests proves that they can be more efficient).
 */

public class NonFungibleToken {
    public BigInteger id;

    public static final class Attribute {
        public final String id;
        public String name;
        public String text;
        public final BigInteger value;
        public Attribute(String attributeId, String name, BigInteger value, String text) {
            this.id = attributeId;
            this.name = name;
            this.text = text;
            this.value = value;
        }
    }

    protected HashMap<String, Attribute> attributes;

    public Attribute getAttribute(String attributeId) {
        return attributes.get(attributeId);
    }

    public void setAttribute(String attributeId, Attribute attribute) {
        attributes.put(attributeId, attribute);
    }

    public NonFungibleToken(BigInteger tokenId, AssetDefinition ad){
        this(tokenId);
        ad.parseField(tokenId, this);
    }

    public NonFungibleToken(BigInteger tokenId) {
        id = tokenId;
        attributes = new HashMap();
    }

    public String getDate(String format)
    {
        long dateUTC = getAttribute("time").value.longValue();
        Date dateFormat = new java.util.Date(dateUTC * 1000L);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(format, Locale.ENGLISH); //TODO: Get locale
        return dateFormatter.format(dateFormat.getTime());
    }

    public String getRangeStr(TicketRange data)
    {
        int ticketStart = getAttribute("number").value.intValue();
        String ticketRange = String.valueOf(ticketStart);
        if (data.tokenIds != null)
        {
            int lastValue = ticketStart + (data.tokenIds.size() - 1);
            if (data.tokenIds.size() > 1)
            {
                ticketRange = ticketRange + "-" + lastValue;
            }
        }

        return ticketRange;
    }
}
