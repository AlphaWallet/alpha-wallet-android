package io.stormbird.wallet.util;

import io.stormbird.wallet.entity.EthereumProtocolParser;
import io.stormbird.wallet.entity.QrUrlResult;

/**
 * Created by marat on 10/11/17.
 * Parses out protocol, address and parameters from a URL originating in QR codes used by wallets &
 * token exchanges.
 *
 * Examples:
 * - Trust wallet
 * - HITBTC
 * - MEW
 * - JAXX
 * - Plain Ethereum address
 */

public class QRURLParser {
    private static QRURLParser mInstance;

    public static QRURLParser getInstance() {
        if (mInstance == null) {
            synchronized (QRURLParser.class) {
                if (mInstance == null) {
                    mInstance = new QRURLParser();
                }
            }
        }
        return mInstance;
    }

    private QRURLParser() { }

    // Be lenient and allow invalid characters in address
    private static boolean isValidAddress(String address) {
        return address.length() > 5; // could be ENS address
    }

    private static String extractAddress(String str) {
        String address;
        try {
            address = str.toLowerCase();
        } catch (StringIndexOutOfBoundsException ex) {
            return null;
        }

        return isValidAddress(address) ? address : null;
    }

    public QrUrlResult parse(String url) {
        String[] parts = url.split(":");

        if (parts.length == 1)
        {
            String address = extractAddress(parts[0]);

            if (address != null) {
                return new QrUrlResult(address.toLowerCase());
            }
        }

        if (parts.length == 2)
        {
            EthereumProtocolParser parser = new EthereumProtocolParser();
            return parser.readProtocol(parts[0], parts[1]);
        }

        return null;
    }

    public String extractAddressFromQrString(String url) {

        QrUrlResult result = parse(url);

        if (result == null) {
            return null;
        }

        return result.getAddress();
    }
}
