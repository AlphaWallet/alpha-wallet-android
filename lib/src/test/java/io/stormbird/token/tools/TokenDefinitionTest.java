package io.stormbird.token.tools;

import io.stormbird.token.entity.NonFungibleToken;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;

import static org.junit.Assert.*;


public class TokenDefinitionTest {
    BigInteger[] ticketIDs = {
	BigInteger.valueOf(0x010CCB53), BigInteger.valueOf(0x010CCB54),
	BigInteger.valueOf(0x02020075), BigInteger.valueOf(0x02020076)
    };
    File file = new File("../contracts/TicketingContract.xml");

    @Test
    public void TokenInformationCanBeExtracted() throws IOException, SAXException {
        assertTrue(file.exists());
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("en"));
        assertFalse(ticketAsset.fields.isEmpty());
        assertNotEquals(0, ticketAsset.tokenName.length());

        // test contract address extraction
        String contractAddress = ticketAsset.getContractAddress(1);
        assertEquals(40+2, contractAddress.length());

        // test feature extraction
        assertEquals("https://", ticketAsset.marketQueueAPI.substring(0,8));
    }

    @Test
    public void FieldDefinitionShouldParse() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("en"));

        NonFungibleToken ticket = new NonFungibleToken(ticketIDs[0], ticketAsset);
        assertEquals("№", ticket.getAttribute("numero").name);
        assertEquals(BigInteger.valueOf(0xCB53), ticket.getAttribute("numero").value);

        /* Epoch, the following test only works from Singapore */
        /* Travis isn't in Singapore ... */
        //assertEquals("Thu Jan 01 07:30:00 SGT 1970", ticket.getAttribute("time").text);
        assertEquals(BigInteger.ZERO, ticket.getAttribute("time").value);
    }

    @Test
    public void XMLSignatureShouldValidate() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("en"));
        assertEquals("Shankai", ticketAsset.getKeyName());
        // TODO: actually validate XML signature
    }

    @Test(expected = SAXException.class)
    public void BadLocaleShouldThrowException() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("asdf"));
    }
}
