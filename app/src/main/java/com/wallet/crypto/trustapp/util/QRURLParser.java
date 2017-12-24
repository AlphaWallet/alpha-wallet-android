package com.wallet.crypto.trustapp.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int ADDRESS_LENGTH = 42;
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

    public class QrUrlResult {
        private String protocol;
        private String address;
        private Map<String, String> parameters;

        QrUrlResult(String protocol, String address, Map<String, String> parameters) {
            this.protocol = protocol;
            this.address = address;
            this.parameters = parameters;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getAddress() {
            return address;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }
    }

    // Be lenient and allow invalid characters in address
    private static boolean isValidAddress(String address) {
        return address.length() == ADDRESS_LENGTH;
    }

    private static String extractAddress(String str) {
        String address;
        try {
            address = str.substring(0, ADDRESS_LENGTH).toLowerCase();
        } catch (StringIndexOutOfBoundsException ex) {
            return null;
        }

        return isValidAddress(address) ? address : null;
    }

    public QrUrlResult parse(String url) {
        String[] parts = url.split(":");

        if (parts.length == 1) {
            String address = extractAddress(parts[0]);

            if (address != null) {
                return new QrUrlResult("", address.toLowerCase(), new HashMap<String, String>());
            }
        }

        if (parts.length == 2) {
            String protocol = parts[0];
			String address = extractAddress(parts[1]);
            if (address != null) {
				Map<String, String> params = new HashMap<>();
				String[] afterProtocol = parts[1].split("\\?");
				if (afterProtocol.length == 2) {
					String paramString = afterProtocol[1];
					List<String> paramParts = Arrays.asList(paramString.split("&"));
					params = parseParamsFromParamParts(paramParts);
				}

				return new QrUrlResult(protocol, address, params);
            }
        }

        return null;
    }

    private static Map<String,String> parseParamsFromParamParts(List<String> paramParts) {
        Map<String, String> params = new HashMap<>();

        if (paramParts.isEmpty()) {
            return params;
        }

        for (String pairStr : paramParts) {
            String[] pair = pairStr.split("=");
            if (pair.length < 2) {
            	break;
            }
            params.put(pair[0], pair[1]);
        }
        return params;
    }

    public String extractAddressFromQrString(String url) {

        QrUrlResult result = parse(url);

        if (result == null) {
            return null;
        }

        return result.getAddress();
    }

}
