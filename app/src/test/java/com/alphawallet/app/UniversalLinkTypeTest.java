package com.alphawallet.app;

import org.junit.Test;

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.token.entity.MagicLinkData;
import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.ParseMagicLink;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by James on 9/02/2019.
 * Stormbird in Singapore
 */
public class UniversalLinkTypeTest
{
    private static final ParseMagicLink parser = new ParseMagicLink(new CryptoFunctions(), null);

    /**
     * these values give the key format, ie
     * 4 bytes - price in MicroEth (Szabo)
     * 4 bytes - expiry (Unix time stamp - unsigned, gives higher range)
     * 20 bytes - contract addr
     * variable length indicies, multiple of single byte with MSB specifying extension to
     *
     */
    final BigDecimal DROP_VALUE      = Convert.toWei("0.01", Convert.Unit.ETHER);  //Currency drop value in Wei
    final long NONCE            = 13;
    final String CONTRACT_ADDR  = "0x4e4a970a03d0b24877244ac0b233575c201d3f44";

    //roll a new key
    ECKeyPair testKey = ECKeyPair.create("Test Key".getBytes());

    private long expireTomorrow;

    @Test
    public void GenerateCurrencyLink() throws SalesOrderMalformed
    {
        //generate link
        BigInteger szaboAmount = com.alphawallet.token.tools.Convert.fromWei(DROP_VALUE, com.alphawallet.token.tools.Convert.Unit.SZABO).abs().toBigInteger();
        expireTomorrow = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        expireTomorrow = expireTomorrow / 1000; //convert to seconds

        //This generates the 'message' part of the transaction
        //--- this is bitwise identical to message the 'formMessage' internal function in the Ethereum contract generates.
        byte[] tradeBytes = parser.getCurrencyBytes(CONTRACT_ADDR, szaboAmount, expireTomorrow, NONCE);

        //sign the tradeBytes with the testKey generated above
        byte[] signature = getSignature(tradeBytes);

        //add the currency link designation on the front
        byte[] linkMessage = ParseMagicLink.generateCurrencyLink(tradeBytes);

        //now complete the link by adding the signature on the end
        String universalLink = parser.completeUniversalLink(1,linkMessage, signature);

        System.out.println(universalLink);

        //now ensure we can extract all the information correctly
        CheckCurrencyLink(universalLink);
    }

    //Check the link is recoverable according to the spec
    //Spec is:
    //0xXX -> Link spec first byte (0x04 for currency drop)
    //0x  XXXXXXXXXXXXXXXX -> 8 bytes message 'XDAIDROP'
    //0x                  XXXXXXXX -> 4 bytes nonce
    //0x                          XXXXXXXX -> 4 bytes link value in szabo
    //0x                                  XXXXXXXX -> 4 bytes expiry
    //0x                                          XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX -> 20 bytes contract address
    //0x                                                                                  XXX ... -> 65 byte signature
    private void CheckCurrencyLink(String universalLink)
    {
        try
        {
            BigInteger szaboAmount = com.alphawallet.token.tools.Convert.fromWei(DROP_VALUE, com.alphawallet.token.tools.Convert.Unit.SZABO).abs().toBigInteger();

            MagicLinkData data = parser.parseUniversalLink(universalLink);
            String recoveredPrefix = new String(data.prefix);
            assertEquals(recoveredPrefix, "XDAIDROP");
            assertEquals(data.nonce.longValue(), NONCE);
            assertEquals(data.amount, szaboAmount);
            assertEquals(data.expiry, expireTomorrow);

            //check signature
            String ownerAddress = "0x" + ecRecoverAddress(); // get testKey address
            String recoveredOriginatorAddress = parser.getOwnerKey(data);
            assertEquals(ownerAddress, recoveredOriginatorAddress);
        }
        catch (SalesOrderMalformed salesOrderMalformed)
        {
            salesOrderMalformed.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private byte[] getSignature(byte[] message) throws SalesOrderMalformed {
        Sign.SignatureData sigData = Sign.signMessage(message, testKey);

        byte[] sig = new byte[65];

        try {
            System.arraycopy(sigData.getR(), 0, sig, 0, 32);
            System.arraycopy(sigData.getS(), 0, sig, 32, 32);
            System.arraycopy(sigData.getV(), 0, sig, 64, 1);
        } catch (IndexOutOfBoundsException e) {
            throw new SalesOrderMalformed("Signature shorter than expected 256");
        }

        return sig;
    }

    private String ecRecoverAddress(byte[] data, Sign.SignatureData signature) //get the hex string address from the sig and data
    {
        String address = "";
        try
        {
            BigInteger recoveredKey = Sign.signedMessageToKey(data, signature); //get embedded address
            address = Keys.getAddress(recoveredKey);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return address;
    }

    private String ecRecoverAddress() throws Exception
    {
        String testSigMsg = "obtain public key";
        Sign.SignatureData testSig = Sign.signMessage(testSigMsg.getBytes(), testKey);
        BigInteger recoveredKey = Sign.signedMessageToKey(testSigMsg.getBytes(), testSig);

        return Keys.getAddress(recoveredKey);
    }
}
