package io.stormbird.token.tools;

import io.stormbird.token.entity.*;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.Assert.*;


public class TokenDefinitionTest implements AttributeInterface
{
    Stream<BigInteger> ticketIDs = Stream.of(
            "1015b4a28f000000000000000000000", "1015b4a28f000000000000000000000",
            "1015b4a28f000000000000000000000", "1015b4a28f000000000000000000000"
    ).map(hexstr -> new BigInteger(hexstr, 16));
    File file = new File("src/test/ts/entrytoken.xml");

    @Test
    public void TokenInformationCanBeExtracted() throws IOException, SAXException {
        assertTrue(file.exists());
        TokenDefinition entryToken = new TokenDefinition(new FileInputStream(file), new Locale("en"), null);
        assertFalse(entryToken.attributeTypes.isEmpty());
        for (String contractName : entryToken.contracts.keySet())
        {
            assertNotEquals(0, contractName.length());
        }

        // test contract address extraction
        String holdingContract = entryToken.holdingToken;
        ContractInfo ci = entryToken.contracts.get(holdingContract);

        assertTrue(entryToken.contracts.size() > 0); //we have at least one address

        for (int networkId : ci.addresses.keySet())
        {
            for (String address : ci.addresses.get(networkId))
            {
                assertEquals(40+2, address.length());
            }
        }
    }

    @Test
    public void AttributeTypesShouldParse() throws IOException, SAXException {
        TokenDefinition entryToken = new TokenDefinition(new FileInputStream(file), new Locale("en"), null);
        BigInteger tokenId = new BigInteger("1015b4a28f000000000000000000000", 16);

        //StringBuilder tokenData = new StringBuilder();

        //get first holding token
        String holdingContract = entryToken.holdingToken;
        ContractInfo ci = entryToken.contracts.get(holdingContract); //ropsten
        ContractAddress cAddr = new ContractAddress(3, ci.addresses.get(3).get(0));

        //Have to move this test to app or DMZ test suite as need to supply ethereum interface
        //supplying a web3j interface here will clash either with DMZ's Java or App's Android web3j.
        //Could just supply a direct call to eth via HTTPS or hard code the expected result as a call
        //test fetching street attribute
        /*TokenScriptResult.Attribute streetAttr = entryToken.fetchAttrResult("street", tokenId, cAddr, this).blockingFirst();

        assertNotNull(streetAttr);
        assertNotNull(streetAttr.text);
        assertTrue(streetAttr.text.length() > 0);*/
    }

    @Test
    public void XMLSignatureShouldValidate() throws IOException, SAXException {
        TokenDefinition entryToken = new TokenDefinition(new FileInputStream(file), new Locale("en"), null);
        assertEquals("EntryToken", entryToken.holdingToken);
        // TODO: actually validate XML signature
    }

    @Test(expected = SAXException.class)
    public void BadLocaleShouldThrowException() throws IOException, SAXException {
        TokenDefinition ticketAsset = new TokenDefinition(new FileInputStream(file), new Locale("asdf"), null);
    }



    //No caching or optimisation for tests

    @Override
    public TransactionResult getFunctionResult(ContractAddress contract, AttributeType attr, BigInteger tokenId)
    {
        return new TransactionResult(contract.chainId, contract.address, tokenId, attr);
    }

    @Override
    public TransactionResult storeAuxData(TransactionResult tResult)
    {
        return tResult;
    }

    @Override
    public boolean resolveOptimisedAttr(ContractAddress contract, AttributeType attr, TransactionResult transactionResult)
    {
        return false;
    }
}
