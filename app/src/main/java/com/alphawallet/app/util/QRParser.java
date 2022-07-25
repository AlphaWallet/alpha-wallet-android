package com.alphawallet.app.util;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.EIP681Type;
import com.alphawallet.app.entity.EthereumProtocolParser;
import com.alphawallet.app.entity.QRResult;
import com.alphawallet.app.ui.widget.entity.ENSHandler;
import com.alphawallet.token.entity.ChainSpec;
import com.alphawallet.token.entity.MagicLinkInfo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import timber.log.Timber;

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

public class QRParser {
    private static QRParser mInstance;
    private static final Pattern findAddress = Pattern.compile("(0x)([0-9a-fA-F]{40})($|\\s)");
    private static final List<ChainSpec> extraChains = new ArrayList<>();

    public static QRParser getInstance(List<ChainSpec> chains) {
        if (mInstance == null) {
            synchronized (QRParser.class) {
                if (mInstance == null) {
                    mInstance = new QRParser();
                }
            }
        }
        extraChains.clear();
        if (chains != null) extraChains.addAll(chains);
        return mInstance;
    }

    private QRParser() { }

    private static String extractAddress(String str)
    {
        String address;
        try
        {
            if (Utils.isAddressValid(str))
            {
                return str;
            }

            String[] split = str.split("[/&@?=]");
            address = split.length > 0 ? split[0] : null;

            //is it a valid ethereum Address?
            if (Utils.isAddressValid(address))
                return address;

            if (ENSHandler.couldBeENS(address))
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

        return (Utils.isAddressValid(address) || ENSHandler.canBeENSName(address)) ? address : null;
    }

    public QRResult parse(String url) {
        if (url == null) return null;
        String[] parts = url.split(":");

        QRResult result = null;

        //Check for import/magic link
        if(checkForMagicLink(url))
        {
            result = new QRResult(url, EIP681Type.MAGIC_LINK);
            return result;
        }

        if (parts.length == 1)
        {
            String address = extractAddress(parts[0]);

            if (address != null) {
                result = new QRResult(address);
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
                result = new QRResult(address);
            }
            else
            {
                try
                {
                    //looks like a URL?
                    new URL(url);
                    result = new QRResult(url, EIP681Type.URL); //resolve to URL
                }
                catch (Exception e)
                {
                    result = new QRResult(url, EIP681Type.OTHER);
                }
            }
        }
        else if (result.type == EIP681Type.OTHER && validAddress(result.getAddress()))
        {
            //promote type
            result.type = EIP681Type.OTHER_PROTOCOL;
        }

        return result;
    }

    public String extractAddressFromQrString(String url) {

        QRResult result = parse(url);

        if (result == null || result.type == EIP681Type.OTHER)
        {
            return null;
        }

        return result.getAddress();
    }

    /**
     * This method checks for Magic/Import token kind of URL which can be redirected to
     * import token screen later.
     * @param data QR Code
     * @return
     * TRUE: The given data is Magic/Import one
     * FALSE: The given data is not Magic/Import one
     */
    private boolean checkForMagicLink(String data)
    {
        try
        {
            long chainId = MagicLinkInfo.identifyChainId(data);

            if (chainId > 0) //see if it's a valid link
            {
                return true;
            }
        }
        catch (Exception e)
        {
            // No action
            Timber.e(e);
        }

        return false;
    }

    private boolean isEmpty(String val)
    {
        return (val == null || val.length() == 0);
    }

    public static boolean validAddress(String address)
    {
        return address != null && ((address.startsWith("0x") && address.length() > 10)
                || (address.contains(".") && address.indexOf(".") <= address.length() - 2));
    }
}
