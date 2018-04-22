package io.stormbird.token.tools;

import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class TokenDefinitionTest {
    File file = new File("../xml/ticket.xml");

    @Test
    public void TokenDefinitionCanBeCreated() throws IOException, SAXException {
        assertTrue(file.exists());
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), "en");
        assertFalse(ticketAsset.fields.isEmpty());
    }

}
