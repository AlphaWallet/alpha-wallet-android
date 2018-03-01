package com.wallet.crypto.alphawallet;

import org.junit.Test;
import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import com.wallet.crypto.alphawallet.repository.AssetDefinition;
import com.wallet.crypto.alphawallet.repository.entity.NonFungibleToken;

public class AssetDefinitionTest {

    class Ticket implements NonFungibleToken {
        Map<String,String> properties;
        Map<String,String> propertyNames;

        Ticket() {
            properties = new HashMap<>();
            propertyNames = new HashMap<>();
        }

        @Override
        public void setField(String id, String name, String value) {
            properties.put(id, value);
            propertyNames.put(id, name);
        }

        @Override
        public String getFieldName(String id) {
            return propertyNames.get(id);
        }

        @Override
        public String getFieldText(String id) {
            return properties.get(id);
        }
    }

    @Test
    public void AssetDefinitionShouldLoad() {
        AssetDefinition ticketAsset;
        String path = "/home/weiwu/StudioProjects/trust-wallet-android/app/src/main/assets/ticket.xml";
        ticketAsset = new AssetDefinition(new File(path), "en");
        assertFalse(ticketAsset.fields.isEmpty());
    }

    @Test
    public void AssetDefinitionShouldParse() {
        AssetDefinition ticketAsset;
        String path = "/home/weiwu/StudioProjects/trust-wallet-android/app/src/main/assets/ticket.xml";
        ticketAsset = new AssetDefinition(new File(path), "en");
        assertFalse(ticketAsset.fields.isEmpty());

        Ticket ticket = new Ticket();
        ticketAsset.parseField(BigInteger.valueOf(838483), ticket);
        assertEquals("Number", ticket.getFieldName("number"));
        assertEquals(Integer.valueOf(838483 % 65536).toString(), ticket.getFieldText("number"));
        /* Epoch */
        assertEquals("Thu Jan 01 07:30:00 SGT 1970", ticket.getFieldText("time"));
    }

}