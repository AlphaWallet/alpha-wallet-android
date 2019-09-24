package com.alphawallet.app.util;

import com.alphawallet.app.entity.EthereumProtocolParser;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.ui.widget.entity.ENSHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alphawallet.app.entity.EthereumProtocolParser.ADDRESS_LENGTH;
import static org.web3j.crypto.WalletUtils.isValidAddress;

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
    private static final Pattern findAddress = Pattern.compile("(0x)([0-9a-fA-F]{40})($|\\s)");

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

    private static String extractAddress(String str) {
        String address;
        try {
            if (!isValidAddress(str))
            {
                Matcher matcher = findAddress.matcher(str);
                if (matcher.find())
                {
                    str = matcher.group(1) + matcher.group(2);
                }
            }

            if (isValidAddress(str))
            {
                address = str.substring(0, ADDRESS_LENGTH);
            }
            else
            {
                String[] split = str.split("[/&@?=]");
                address = split.length > 0 ? split[0] : null;
            }
        } catch (StringIndexOutOfBoundsException ex) {
            return null;
        }

        return (isValidAddress(address) || ENSHandler.canBeENSName(address)) ? address : null;
    }

    public QrUrlResult parse(String url) {
        if (url == null) return null;
        String[] parts = url.split(":");

        QrUrlResult result = null;

        if (parts.length == 1)
        {
            String address = extractAddress(parts[0]);

            if (address != null) {
                result = new QrUrlResult(address);
            }
        }
        else if (parts.length == 2)
        {
            EthereumProtocolParser parser = new EthereumProtocolParser();
            result = parser.readProtocol(parts[0], parts[1]);
        }

        //it's not a recognised protocol, try garbled address (eg copy/paste from telegram).
        if (result == null)
        {
            String address = extractAddress(url);
            if (address != null) {
                result = new QrUrlResult(address);
            }
        }

        return result;
    }

    public String extractAddressFromQrString(String url) {

        QrUrlResult result = parse(url);

        if (result == null) {
            return null;
        }

        return result.getAddress();
    }
}
