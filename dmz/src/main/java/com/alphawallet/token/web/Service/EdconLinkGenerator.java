package com.alphawallet.token.web.Service;

import com.alphawallet.token.entity.SalesOrderMalformed;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.token.tools.ParseMagicLink;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import static com.alphawallet.token.web.Service.SpawnableLinkGenerator.bytesFromSignature;

public class EdconLinkGenerator {

    private static final String contractAddress = "0xF6b8DD8Ba9996bEaE6Ad0eE3481F1E9cF080A9eB";
    private static ParseMagicLink parseMagicLink = new ParseMagicLink(new CryptoFunctions(), null);
    //TODO set private key & chain id
    private static final BigInteger privateKey = new BigInteger("0", 16);
    private static final long chainId = 100;

    // Time todo put in right format & set each time
    private static final String date = "20200403090000+0300";
    // Cities
    private static final long VIENNA = 1;

    // Venues
    private static final long BLOCKCHAIN_HALL = 1;

    // Numero or sequence number TODO set each time
    private static long numero = 0;

    // Categories
    private static final long A = 1;
    private static final long B = 2;
    private static final long C = 3;
    private static final long D = 4;
    private static final long E = 5;

    // Ticket expiry
    private static long expiry = (System.currentTimeMillis() + 5000000000L) / 1000L;;


    public static void main(String[] args) throws SalesOrderMalformed {
        int rounds = 50;
        while(rounds > 0) {
            new com.alphawallet.token.web.Service.EdconLinkGenerator(date, VIENNA, BLOCKCHAIN_HALL, 1);
            rounds--;
        }
    }

    private EdconLinkGenerator(
            String date,
            long city,
            long venue,
            int quantity
    ) throws SalesOrderMalformed {
        // Set values here
        List<BigInteger> tokens = setTokenIds(date, city, venue, A, quantity);
        createSpawnableLink(tokens);
    }

    private void createSpawnableLink(List<BigInteger> tokens) throws SalesOrderMalformed {
        byte[] message = parseMagicLink.getSpawnableBytes(tokens, contractAddress, BigInteger.ZERO, expiry);
        byte[] signature = signMagicLink(message);
        byte[] linkData = ParseMagicLink.generateSpawnableLeadingLinkBytes(tokens, contractAddress, BigInteger.ZERO, expiry);
        String link = parseMagicLink.completeUniversalLink(chainId, linkData, signature);
        System.out.println(link);
        System.out.println();
    }

    private List<BigInteger> setTokenIds(String date, long city, long venue, long category, int quantity)
    {
        List<BigInteger> tokens = new ArrayList<>();
        while(quantity > 0) {
            String tokenId = "";
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(Numeric.toBigInt(date.getBytes()), 38);
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(city), 2);
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(venue), 2);
            //Padding
            tokenId += "00000000000000";
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(category), 2);
            // Must increment the numero to change the token id as 721 can only map one token to one address
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(BigInteger.valueOf(numero++), 4);
            //pad the final zeros on
            tokenId += Numeric.toHexStringNoPrefixZeroPadded(BigInteger.ZERO, 2);
            BigInteger token = new BigInteger(tokenId, 16);
            tokens.add(token);
            quantity--;
        }
        return tokens;
    }

    private byte[] signMagicLink(byte[] signData) {
        ECKeyPair ecKeyPair  = ECKeyPair.create(privateKey);
        Sign.SignatureData signatureData = Sign.signMessage(signData, ecKeyPair);
        return bytesFromSignature(signatureData);
    }

}
