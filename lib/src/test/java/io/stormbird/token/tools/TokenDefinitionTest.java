package io.stormbird.token.tools;

import io.stormbird.token.entity.NonFungibleToken;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.Assert.*;


public class TokenDefinitionTest {
    Stream<BigInteger> ticketIDs = Stream.of(
            // time: 5B2282F0, TPE vs DEN match: 01, category 0C
            "01015B2282F054504544454E010BCB53", "01015B2282F054504544454E010BCB54",
            // time: 5B23D470, VIE vs SIN match: 02, category 02
            "01015B2282F056494553494E0202CB53", "01015B2282F056494553494E0202CB54"
    ).map(hexstr -> new BigInteger(hexstr, 16));
    File file = new File("src/test/tbml/TicketingContract.xml");

    @Test
    public void TokenInformationCanBeExtracted() throws IOException, SAXException {
        assertTrue(file.exists());
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("en"));
        assertFalse(ticketAsset.attributeTypes.isEmpty());
        assertNotEquals(0, ticketAsset.tokenName.length());

        // test contract address extraction
        assertTrue(ticketAsset.addresses.size() > 0); //we have at least one address
        for (String address : ticketAsset.addresses.keySet())
        {
            assertEquals(40+2, address.length());
        }

        // test feature extraction
        assertEquals("https://", ticketAsset.marketQueueAPI.substring(0,8));
    }

    @Test
    public void AttributeTypesShouldParse() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("en"));

        // the following test case only work with a very specific xml file.
        ticketIDs.map(ticketID -> new NonFungibleToken(ticketID, ticketAsset)).forEach(ticket -> {
            assertTrue(BigInteger.valueOf(0xCB53).compareTo(ticket.getAttribute("numero").value) < 1);
            assertTrue(BigInteger.valueOf(0xCB54).compareTo(ticket.getAttribute("numero").value) > -1);
            String nameCheck = ticket.getAttribute("numero").name;
            final String nameConst = "\u2116";
            assertEquals(nameConst, nameCheck);
            assertEquals("20180614180000+0300", ticket.getAttribute("time").text);
        });
        /* Epoch, the following test only works from Singapore */
        /* Travis isn't in Singapore ... */
        //assertEquals("Thu Jan 01 07:30:00 SGT 1970", ticket.getAttribute("time").text);
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
