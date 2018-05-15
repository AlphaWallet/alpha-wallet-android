package io.awallet.crypto.alphawallet;

import io.stormbird.token.entity.NonFungibleToken;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.awallet.crypto.alphawallet.repository.AssetDefinition;

public class AssetDefinitionTest {
    BigInteger[] ticketIDs = {
	BigInteger.valueOf(0x010CCB53), BigInteger.valueOf(0x010CCB54),
	BigInteger.valueOf(0x02020075), BigInteger.valueOf(0x02020076)
    };
    File file = new File("src/main/assets/TicketingContract.xml");

    @Test
    public void AssetDefinitionShouldParse() throws IOException, SAXException {
        assertTrue(file.exists());
        AssetDefinition ticketAsset = new AssetDefinition(new FileInputStream(file), "en");
        assertFalse(ticketAsset.fields.isEmpty());

        NonFungibleToken ticket = new NonFungibleToken(ticketIDs[0], ticketAsset);
        assertEquals("Number", ticket.getAttribute("number").name);
        assertEquals(BigInteger.valueOf(0xCB53), ticket.getAttribute("number").value);
        /* Epoch, the following test only works from Singapore */
        /* Travis isn't in Singapore ... */
        //assertEquals("Thu Jan 01 07:30:00 SGT 1970", ticket.getAttribute("time").text);
        assertEquals(BigInteger.ZERO, ticket.getAttribute("time").value);
    }

}
