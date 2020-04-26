package com.alphawallet.app.util;

import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.EthereumProtocolParser;
import com.alphawallet.app.entity.QrUrlResult;
import com.alphawallet.app.ui.widget.entity.ENSHandler;

import java.net.URL;
import java.util.regex.Pattern;

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

    private static String extractAddress(String str)
    {
        String address;
        try
        {
            if (isValidAddress(str))
            {
                return str;
            }

            String[] split = str.split("[/&@?=]");
            address = split.length > 0 ? split[0] : null;

            //is it a valid ethereum Address?
            if (address == null || isValidAddress(address))
                return address;

            if (couldBeENS(address))
            {
                return address;
            }
            else
            {
                address = "";
            }
        }
        catch (StringIndexOutOfBoundsException ex)
        {
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
            else
            {
                try
                {
                    //looks like a URL?
                    new URL(url);
                    result = new QrUrlResult(url, EIP681Type.URL); //resolve to URL
                }
                catch (Exception e)
                {
                    result = new QrUrlResult(url, EIP681Type.OTHER);
                }
            }
        }

        return result;
    }

    public String extractAddressFromQrString(String url) {

        QrUrlResult result = parse(url);

        if (result == null || result.type == EIP681Type.OTHER) {
            return null;
        }

        return result.getAddress();
    }

    private static boolean couldBeENS(String address)
    {
        String[] split = address.split("[.]");
        if (split.length > 1)
        {
            String extension = split[split.length - 1];
            switch (extension)
            {
                case "eth":
                case "xyz":
                    return true;
                default:
                    break;
            }
        }

        return false;
    }
}
