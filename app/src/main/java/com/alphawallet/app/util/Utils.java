package com.alphawallet.app.util;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.webkit.URLUtil;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;

import org.web3j.crypto.WalletUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String ISOLATE_NUMERIC = "(0?x?[0-9a-fA-F]+)";

    public static int dp2px(Context context, int dp) {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }

    public static String formatUrl(String url) {
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
            return url;
        } else {
            if (isValidUrl(url)) {
                return C.HTTPS_PREFIX + url;
            } else {
                return C.GOOGLE_SEARCH_PREFIX + url;
            }
        }
    }

    public static boolean isValidUrl(String url) {
        Pattern p = Patterns.WEB_URL;
        Matcher m = p.matcher(url.toLowerCase());
        return m.matches();
    }

    public static boolean isAlNum(String testStr)
    {
        boolean result = false;
        if (testStr != null && testStr.length() > 0)
        {
            result = true;
            for (int i = 0; i < testStr.length(); i++)
            {
                char c = testStr.charAt(i);
                if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && !(c == '+') && !(c == ',') && !(c == ';'))
                {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    private static String getFirstWord(String text) {
        int index = text.indexOf(' ');
        if (index > -1) {
            return text.substring(0, index).trim();
        } else {
            return text;
        }
    }

    public static String getIconisedText(String text)
    {
        if (TextUtils.isEmpty(text)) return "";
        String firstWord = getFirstWord(text);
        switch (firstWord.length()) {
            case 0:
                return "";
            case 1:
            case 2:
            case 3:
            case 4:
                return firstWord.toUpperCase();
            default:
                return firstWord.substring(0, 4).toUpperCase();
        }
    }

    public static int getChainColour(int chainId)
    {
        switch (chainId)
        {
            case EthereumNetworkRepository.MAINNET_ID:
                return R.color.mainnet;
            case EthereumNetworkRepository.CLASSIC_ID:
                return R.color.classic;
            case EthereumNetworkRepository.POA_ID:
                return R.color.poa;
            case EthereumNetworkRepository.KOVAN_ID:
                return R.color.kovan;
            case EthereumNetworkRepository.ROPSTEN_ID:
                return R.color.ropsten;
            case EthereumNetworkRepository.SOKOL_ID:
                return R.color.sokol;
            case EthereumNetworkRepository.RINKEBY_ID:
                return R.color.rinkeby;
            case EthereumNetworkRepository.GOERLI_ID:
                return R.color.goerli;
            case EthereumNetworkRepository.XDAI_ID:
                return R.color.xdai;
            case EthereumNetworkRepository.ARTIS_SIGMA1_ID:
                return R.color.artis_sigma1;
            case EthereumNetworkRepository.ARTIS_TAU1_ID:
                return R.color.artis_tau1;
            default:
                return R.color.mine;
        }
    }

    public static void setChainColour(View view, int chainId)
    {
        switch (chainId)
        {
            case EthereumNetworkRepository.MAINNET_ID:
                view.setBackgroundResource(R.drawable.background_mainnet);
                break;
            case EthereumNetworkRepository.CLASSIC_ID:
                view.setBackgroundResource(R.drawable.background_classic);
                break;
            case EthereumNetworkRepository.POA_ID:
                view.setBackgroundResource(R.drawable.background_poa);
                break;
            case EthereumNetworkRepository.KOVAN_ID:
                view.setBackgroundResource(R.drawable.background_kovan);
                break;
            case EthereumNetworkRepository.ROPSTEN_ID:
                view.setBackgroundResource(R.drawable.background_ropsten);
                break;
            case EthereumNetworkRepository.SOKOL_ID:
                view.setBackgroundResource(R.drawable.background_sokol);
                break;
            case EthereumNetworkRepository.RINKEBY_ID:
                view.setBackgroundResource(R.drawable.background_rinkeby);
                break;
            case EthereumNetworkRepository.GOERLI_ID:
                view.setBackgroundResource(R.drawable.background_goerli);
                break;
            case EthereumNetworkRepository.XDAI_ID:
                view.setBackgroundResource(R.drawable.background_xdai);
                break;
            case EthereumNetworkRepository.ARTIS_SIGMA1_ID:
                view.setBackgroundResource(R.drawable.background_artis_sigma1);
                break;
            case EthereumNetworkRepository.ARTIS_TAU1_ID:
                view.setBackgroundResource(R.drawable.background_artis_tau1);
                break;
            default:
                EthereumNetworkRepository.setChainColour(view, chainId);
                break;
        }
    }

    public static void setChainCircle(View view, int chainId)
    {
        switch (chainId)
        {
            case EthereumNetworkRepository.CLASSIC_ID:
                view.setBackgroundResource(R.drawable.item_etc_circle);
                break;
            case EthereumNetworkRepository.POA_ID:
                view.setBackgroundResource(R.drawable.item_poa_circle);
                break;
            case EthereumNetworkRepository.KOVAN_ID:
                view.setBackgroundResource(R.drawable.item_kovan_circle);
                break;
            case EthereumNetworkRepository.ROPSTEN_ID:
                view.setBackgroundResource(R.drawable.item_ropsten_circle);
                break;
            case EthereumNetworkRepository.SOKOL_ID:
                view.setBackgroundResource(R.drawable.item_sokol_circle);
                break;
            case EthereumNetworkRepository.RINKEBY_ID:
                view.setBackgroundResource(R.drawable.item_rinkeby_circle);
                break;
            case EthereumNetworkRepository.GOERLI_ID:
                view.setBackgroundResource(R.drawable.item_goerli_circle);
                break;
            case EthereumNetworkRepository.XDAI_ID:
                view.setBackgroundResource(R.drawable.item_xdai_circle);
                break;
            case EthereumNetworkRepository.MAINNET_ID:
                view.setBackgroundResource(R.drawable.item_eth_circle);
                break;
            default:
                EthereumNetworkRepository.setChainCircle(view, chainId);
                break;

        }
    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain != null) {
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } else {
            return "";
        }
    }

    public static String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public static boolean copyFile(String source, String dest)
    {
        try
        {
            FileChannel s = new FileInputStream(source).getChannel();
            FileChannel d = new FileOutputStream(dest).getChannel();
            d.transferFrom(s, 0, s.size());
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isAddressValid(String address)
    {
        return address != null && address.length() > 0 && WalletUtils.isValidAddress(address);
    }

    public static String intArrayToString(int[] values)
    {
        StringBuilder store = new StringBuilder();
        boolean firstValue = true;
        for (int network : values)
        {
            if (!firstValue) store.append(",");
            store.append(network);
            firstValue = false;
        }

        return store.toString();
    }

    public static List<Integer> intListToArray(String list)
    {
        List<Integer> idList = new ArrayList<>();
        //convert to array
        String[] split = list.split(",");
        for (String s : split)
        {
            Integer value;
            try
            {
                value = Integer.valueOf(s);
                idList.add(value);
            }
            catch (NumberFormatException e)
            {
                //empty
            }
        }

        return idList;
    }

    public static int[] bigIntegerListToIntList(List<BigInteger> ticketSendIndexList)
    {
        int[] indexList = new int[ticketSendIndexList.size()];
        for (int i = 0; i < ticketSendIndexList.size(); i++) indexList[i] = ticketSendIndexList.get(i).intValue();
        return indexList;
    }

    public static boolean isHex(String hexStr)
    {
        if (hexStr == null) return false;
        for (Character c : hexStr.toCharArray())
        {
            if ((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
            {
                return true;
            }
        }

        return false;
    }

    public static String isolateNumeric(String valueFromInput)
    {
        try
        {
            Matcher regexResult = Pattern.compile(ISOLATE_NUMERIC).matcher(valueFromInput);
            if (regexResult.find())
            {
                if (regexResult.groupCount() >= 1)
                {
                    valueFromInput = regexResult.group(0);
                }
            }
        }
        catch (Exception e)
        {
            // Silent fail - no action; just return input; this function is only to clean junk from a number
        }

        return valueFromInput;
    }

    public static String formatAddress(String address) {
        String result = "";
        String firstSix = address.substring(0, 6);
        String lastSix = address.substring(address.length()-4);
        StringBuilder formatted = new StringBuilder(result);
        return formatted.append(firstSix).append("...").append(lastSix).toString().toLowerCase();
    }

    /**
     * Just enough for diagnosis of most errors
     * @param s String to be HTML escaped
     * @return escaped string
     */
    public static String escapeHTML(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c)
            {
                case '"':
                    out.append("&quot;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    public static String convertTimePeriodInSeconds(long pendingTimeInSeconds, Context ctx)
    {
        long days = pendingTimeInSeconds/(60*60*24);
        pendingTimeInSeconds -= (days*60*60*24);
        long hours = pendingTimeInSeconds/(60*60);
        pendingTimeInSeconds -= (hours*60*60);
        long minutes = pendingTimeInSeconds/60;
        long seconds = pendingTimeInSeconds%60;

        StringBuilder sb = new StringBuilder();
        int timePoints = 0;

        if (days > 0)
        {
            timePoints = 2;
            if (days == 1)
            {
                sb.append(ctx.getString(R.string.day_single));
            }
            else
            {
                sb.append(ctx.getString(R.string.day_plural, String.valueOf(days)));
            }
        }

        if (hours > 0)
        {
            if (timePoints == 0)
            {
                timePoints = 1;
            }
            else
            {
                sb.append(", ");
            }

            if (hours == 1)
            {
                sb.append(ctx.getString(R.string.hour_single));
            }
            else
            {
                sb.append(ctx.getString(R.string.hour_plural, String.valueOf(hours)));
            }
        }

        if (minutes > 0 && timePoints < 2)
        {
            if (timePoints != 0)
            {
                sb.append(", ");
            }
            timePoints++;
            if (minutes == 1)
            {
                sb.append(ctx.getString(R.string.minute_single));
            }
            else
            {
                sb.append(ctx.getString(R.string.minute_plural, String.valueOf(minutes)));
            }
        }

        if (seconds > 0 && timePoints < 2)
        {
            if (timePoints != 0)
            {
                sb.append(", ");
            }
            if (seconds == 1)
            {
                sb.append(ctx.getString(R.string.second_single));
            }
            else
            {
                sb.append(ctx.getString(R.string.second_plural, String.valueOf(seconds)));
            }
        }

        return sb.toString();
    }

    public static long randomId() {
        return new Date().getTime();
    }
}
