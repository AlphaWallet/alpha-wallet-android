package com.wallet.crypto.alphawallet;

import org.junit.Test;
import java.io.File;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        ticketAsset = new AssetDefinition(new File(path));
        assertFalse(ticketAsset.fields.isEmpty());
    }

    @Test
    public void AssetDefinitionShouldParse() {
        AssetDefinition ticketAsset;
        String path = "/home/weiwu/StudioProjects/trust-wallet-android/app/src/main/assets/ticket.xml";
        ticketAsset = new AssetDefinition(new File(path));

        Ticket ticket = new Ticket();
        BigInteger ticketID = BigInteger.valueOf(838483);
        ticketAsset.parseField(ticketID, ticket);
        assertTrue(ticket.getFieldText("number").equals("52051"));
    }

}