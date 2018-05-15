package io.stormbird.token.tools;

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


public class TokenDefinitionTest {
    BigInteger[] ticketIDs = {
	BigInteger.valueOf(0x010CCB53), BigInteger.valueOf(0x010CCB54),
	BigInteger.valueOf(0x02020075), BigInteger.valueOf(0x02020076)
    };
    File file = new File("../contracts/TicketingContract.xml");

    @Test
    public void TokenInformationCanBeExtracted() throws IOException, SAXException {
        assertTrue(file.exists());
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), "en");
        assertFalse(ticketAsset.fields.isEmpty());
        assertEquals("Tickets", ticketAsset.tokenName);

        // test contract address extraction
        String contractAddress = ticketAsset.getContractAddress(1);
        assertEquals("0x2Cd6CbC60219B33161F1BF69fbd6c741aD980BBa", contractAddress);

        // test feature extraction
        assertEquals("https://482kdh4npg.execute-api.ap-southeast-1.amazonaws.com/dev/", ticketAsset.marketQueueAPI);
    }

    @Test
    public void FieldDefinitionShouldParse() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), "en");

        NonFungibleToken ticket = new NonFungibleToken(ticketIDs[0], ticketAsset);
        assertEquals("Number", ticket.getAttribute("number").name);
        assertEquals(BigInteger.valueOf(0xCB53), ticket.getAttribute("number").value);
        /* Epoch, the following test only works from Singapore */
        /* Travis isn't in Singapore ... */
        //assertEquals("Thu Jan 01 07:30:00 SGT 1970", ticket.getAttribute("time").text);
        assertEquals(BigInteger.ZERO, ticket.getAttribute("time").value);
    }

    @Test
    public void XMLSignatureShouldValidate() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), "en");
        assertEquals("Shankai", ticketAsset.getKeyName());
        // TODO: actually validate XML signature
    }

}
