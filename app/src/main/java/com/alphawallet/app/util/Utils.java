package com.alphawallet.app.util;

import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.webkit.URLUtil;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.fragment.app.FragmentActivity;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.EasAttestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.pattern.Patterns;
import com.alphawallet.token.entity.ProviderTypedData;
import com.alphawallet.token.entity.Signable;
import com.google.gson.Gson;
import com.google.zxing.client.android.Intents;
import com.google.zxing.common.StringUtils;
import com.journeyapps.barcodescanner.ScanOptions;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.StructuredDataEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import timber.log.Timber;

public class Utils
{
    private static final String ISOLATE_NUMERIC = "(0?x?[0-9a-fA-F]+)";
    private static final String ICON_REPO_ADDRESS_TOKEN = "[TOKEN]";
    private static final String CHAIN_REPO_ADDRESS_TOKEN = "[CHAIN]";
    private static final String TOKEN_LOGO = "/logo.png";
    public static final String ALPHAWALLET_REPO_NAME = "https://raw.githubusercontent.com/alphawallet/iconassets/master/";
    public static final String TRUST_ICON_REPO_BASE = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/";
    private static final String TRUST_ICON_REPO = TRUST_ICON_REPO_BASE + CHAIN_REPO_ADDRESS_TOKEN + "/assets/" + ICON_REPO_ADDRESS_TOKEN + TOKEN_LOGO;
    private static final String ALPHAWALLET_ICON_REPO = ALPHAWALLET_REPO_NAME + ICON_REPO_ADDRESS_TOKEN + TOKEN_LOGO;
    private static final String ATTESTATION_PREFIX = "#attestation=";
    private static final String SMART_PASS_PREFIX = "ticket=";
    private static final String TOKEN_ID_CODE = "{id}";

    public static int dp2px(Context context, int dp)
    {
        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
    }

    public static String formatUrl(String url)
    {
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url) || isWalletPrefix(url))
        {
            return url;
        }
        else
        {
            if (isValidUrl(url))
            {
                return C.HTTPS_PREFIX + url;
            }
            else
            {
                return C.INTERNET_SEARCH_PREFIX + url;
            }
        }
    }

    public static boolean isWalletPrefix(String url)
    {
        return url.startsWith(C.DAPP_PREFIX_TELEPHONE) ||
                url.startsWith(C.DAPP_PREFIX_MAILTO) ||
                url.startsWith(C.DAPP_PREFIX_ALPHAWALLET) ||
                url.startsWith(C.DAPP_PREFIX_MAPS) ||
                url.startsWith(C.DAPP_PREFIX_WALLETCONNECT) ||
                url.startsWith(C.DAPP_PREFIX_AWALLET);
    }

    public static boolean isValidUrl(String url)
    {
        if (TextUtils.isEmpty(url)) return false;
        Pattern p = Patterns.WEB_URL;
        Matcher m = p.matcher(url.toLowerCase());
        return m.matches() || isIPFS(url);
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
                if (!Character.isIdeographic(c) && !Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && (c < 32 || c > 126))
                {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    public static boolean isValidValue(String testStr)
    {
        boolean result = false;
        if (testStr != null && !testStr.isEmpty())
        {
            result = true;
            for (int i = 0; i < testStr.length(); i++)
            {
                char c = testStr.charAt(i);
                if (!Character.isDigit(c) && !(c == '.' || c == ','))
                {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    private static String getFirstWord(String text)
    {
        if (TextUtils.isEmpty(text)) return "";
        text = text.trim();
        int index = 1;
        for (; index < text.length(); index++)
        {
            if ((!Character.isLetterOrDigit(text.charAt(index)) && index > 4) || Character.isWhitespace(text.charAt(index))) break;
        }

        if (!text.isEmpty())
        {
            return text.substring(0, index).trim();
        }
        else
        {
            return "";
        }
    }

    public static String getIconisedText(String text)
    {
        if (TextUtils.isEmpty(text)) return "";
        if (text.length() <= 4) return text;
        String firstWord = getFirstWord(text);
        if (!TextUtils.isEmpty(firstWord))
        {
            return firstWord.substring(0, Math.min(firstWord.length(), 5));
        }
        else
        {
            return "";
        }
    }

    public static String getShortSymbol(String text)
    {
        if (TextUtils.isEmpty(text)) return "";
        String firstWord = getFirstWord(text);
        if (!TextUtils.isEmpty(firstWord))
        {
            return firstWord.substring(0, Math.min(firstWord.length(), C.SHORT_SYMBOL_LENGTH));
        }
        else
        {
            return "";
        }
    }

    /**
     * This is here rather than in the Signable class because Signable is cross platform not Android specific
     *
     * @param signable
     * @return
     */
    public static int getSigningTitle(Signable signable)
    {
        switch (signable.getMessageType())
        {
            default:
            case SIGN_MESSAGE:
                return R.string.dialog_title_sign_message_sheet; //warn user this is unsafe
            case SIGN_PERSONAL_MESSAGE:
                return R.string.dialog_title_sign_personal_message;
            case SIGN_TYPED_DATA:
            case SIGN_TYPED_DATA_V3:
            case SIGN_TYPED_DATA_V4:
                return R.string.dialog_title_sign_typed_message;
        }
    }

    public static CharSequence getSignMessageTitle(String message)
    {
        //produce readable text to display in the signing prompt
        StyledStringBuilder sb = new StyledStringBuilder();
        sb.startStyleGroup();
        sb.append(message);
        int i = message.length();
        sb.setSpan(new ForegroundColorSpan(Color.RED), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(Color.RED), i-1, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.applyStyles();

        return sb;
    }

    public static CharSequence formatTypedMessage(ProviderTypedData[] rawData)
    {
        //produce readable text to display in the signing prompt
        StyledStringBuilder sb = new StyledStringBuilder();
        boolean firstVal = true;
        for (ProviderTypedData data : rawData)
        {
            if (!firstVal) sb.append("\n");
            sb.startStyleGroup().append(data.name).append(":");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            sb.append("\n  ").append(data.value.toString());
            firstVal = false;
        }

        sb.applyStyles();

        return sb;
    }

    public static CharSequence formatEIP721Message(StructuredDataEncoder messageData)
    {
        HashMap<String, Object> messageMap = (HashMap<String, Object>) messageData.jsonMessageObject.getMessage();
        StyledStringBuilder sb = new StyledStringBuilder();
        for (String entry : messageMap.keySet())
        {
            sb.startStyleGroup().append(entry).append(":").append("\n");
            sb.setStyle(new StyleSpan(Typeface.BOLD));
            Object v = messageMap.get(entry);
            if (v instanceof LinkedHashMap)
            {
                HashMap<String, Object> valueMap = (HashMap<String, Object>) messageMap.get(entry);
                for (String paramName : valueMap.keySet())
                {
                    String value = valueMap.get(paramName).toString();
                    sb.startStyleGroup().append(" ").append(paramName).append(": ");
                    sb.setStyle(new StyleSpan(Typeface.BOLD));
                    sb.append(value).append("\n");
                }
            }
            else
            {
                sb.append(" ").append(v.toString()).append("\n");
            }
        }

        sb.applyStyles();

        return sb;
    }

    public static CharSequence createFormattedValue(String operationName, Token token)
    {
        String symbol = token != null ? token.getShortSymbol() : "";
        boolean needsBreak = false;

        if ((symbol.length() + operationName.length()) > 16 && symbol.length() > 0)
        {
            int spaceIndex = operationName.lastIndexOf(' ');
            if (spaceIndex > 0)
            {
                operationName = operationName.substring(0, spaceIndex) + '\n' + operationName.substring(spaceIndex + 1);
            }
            else
            {
                needsBreak = true;
            }
        }

        StyledStringBuilder sb = new StyledStringBuilder();
        sb.startStyleGroup().append(operationName);
        sb.setStyle(new StyleSpan(Typeface.NORMAL));

        if (needsBreak)
        {
            sb.append("\n");
        }
        else
        {
            sb.append(" ");
        }

        sb.startStyleGroup().append(symbol);
        sb.setStyle(new StyleSpan(Typeface.BOLD));

        sb.applyStyles();

        return sb;
    }

    public static String loadJSONFromAsset(Context context, String fileName)
    {
        String json = null;
        try
        {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        }
        catch (IOException ex)
        {
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
            Timber.e(e);
        }
        return false;
    }

    public static boolean isAddressValid(String address)
    {
        return address != null && address.length() > 0 && WalletUtils.isValidAddress(address);
    }

    public static String longArrayToString(Long[] values)
    {
        StringBuilder store = new StringBuilder();
        boolean firstValue = true;
        for (long network : values)
        {
            if (!firstValue) store.append(",");
            store.append(network);
            firstValue = false;
        }

        return store.toString();
    }

    public static List<Long> longListToArray(String list)
    {
        List<Long> idList = new ArrayList<>();
        //convert to array
        String[] split = list.split(",");
        for (String s : split)
        {
            Long value;
            try
            {
                value = Long.valueOf(s);
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
        for (int i = 0; i < ticketSendIndexList.size(); i++)
            indexList[i] = ticketSendIndexList.get(i).intValue();
        return indexList;
    }

    public static BigInteger parseTokenId(String tokenIdStr)
    {
        BigInteger tokenId;
        try
        {
            tokenId = new BigInteger(tokenIdStr);
        }
        catch (Exception e)
        {
            tokenId = BigInteger.ZERO;
        }

        return tokenId;
    }

    /**
     * Produce a string CSV of integer IDs given an input list of values
     *
     * @param idList
     * @param keepZeros
     * @return
     */
    public static String bigIntListToString(List<BigInteger> idList, boolean keepZeros)
    {
        if (idList == null) return "";
        String displayIDs = "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (BigInteger id : idList)
        {
            if (!keepZeros && id.compareTo(BigInteger.ZERO) == 0) continue;
            if (!first)
            {
                sb.append(",");
            }
            first = false;

            sb.append(Numeric.toHexStringNoPrefix(id));
            displayIDs = sb.toString();
        }

        return displayIDs;
    }

    public static List<Integer> stringIntsToIntegerList(String userList)
    {
        List<Integer> idList = new ArrayList<>();

        try
        {
            String[] ids = userList.split(",");

            for (String id : ids)
            {
                //remove whitespace
                String trim = id.trim();
                Integer intId = Integer.parseInt(trim);
                idList.add(intId);
            }
        }
        catch (Exception e)
        {
            idList = new ArrayList<>();
        }

        return idList;
    }

    public static String integerListToString(List<Integer> intList, boolean keepZeros)
    {
        if (intList == null) return "";
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (Integer id : intList)
        {
            if (!keepZeros && id == 0) continue;
            if (!first) sb.append(",");
            sb.append(id);
            first = false;
        }

        return sb.toString();
    }

    public static Map<BigInteger, BigInteger> getIdMap(List<BigInteger> tokenIds)
    {
        Map<BigInteger, BigInteger> tokenMap = new HashMap<>();
        for (BigInteger tokenId : tokenIds)
        {
            tokenMap.put(tokenId, tokenMap.containsKey(tokenId) ? tokenMap.get(tokenId).add(BigInteger.ONE) : BigInteger.ONE);
        }

        return tokenMap;
    }

    public static boolean isNumeric(String numString)
    {
        if (numString == null || numString.length() == 0) return false;

        for (int i = 0; i < numString.length(); i++)
        {
            if (Character.digit(numString.charAt(i), 10) == -1)
            {
                return false;
            }
        }

        return true;
    }

    public static boolean isHex(String hexStr)
    {
        if (hexStr == null || hexStr.length() == 0) return false;
        hexStr = Numeric.cleanHexPrefix(hexStr);

        for (int i = 0; i < hexStr.length(); i++)
        {
            if (Character.digit(hexStr.charAt(i), 16) == -1)
            {
                return false;
            }
        }

        return true;
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

    public static String formatAddress(String address)
    {
        if (isAddressValid(address))
        {
            address = Keys.toChecksumAddress(address);
            String result = "";
            String firstSix = address.substring(0, 6);
            String lastFour = address.substring(address.length() - 4);
            return result + firstSix + "..." + lastFour;
        }
        else
        {
            return "0x";
        }
    }

    public static String formatAddress(String address, int frontCharCount)
    {
        if (isAddressValid(address))
        {
            address = Keys.toChecksumAddress(address);
            String result = "";
            String front = address.substring(0, frontCharCount + 2);
            String back = address.substring(address.length() - 4);
            return result + front + "..." + back;
        }
        else
        {
            return "0x";
        }
    }

    public static String splitAddress(String address, int lines)
    {
        address = Keys.toChecksumAddress(address);
        return splitHex(address, lines);
    }

    public static String splitHex(String hex, int lines)
    {
        int split = hex.length()/lines;
        StringBuilder sb = new StringBuilder();
        int index = 0;
        int addend = 0;
        for (int i = 0; i < (lines-1); i++)
        {
            addend = 0;
            if (index > 0)
            {
                sb.append(" ");
            }
            else
            {
                if (lines%2 != 0)
                {
                    addend = 1;
                }
            }
            sb.append(hex.substring(0, split + addend));
            index += split;
            hex = hex.substring(split + addend);
        }
        sb.append(" ");
        sb.append(hex);
        //String front = hex.substring(0, split);
        //String back = hex.substring(split);
        return sb.toString();
    }

    public static String formatTxHash(String txHash)
    {
        if (isTxHashValid(txHash))
        {
            txHash = Keys.toChecksumAddress(txHash);
            String result = "";
            String firstSix = txHash.substring(0, 6);
            String lastFour = txHash.substring(txHash.length() - 4);
            return result + firstSix + "..." + lastFour;
        }
        else
        {
            return "0x";
        }
    }

    public static String formatTxHash(String txHash, int frontCharCount)
    {
        if (isTxHashValid(txHash))
        {
            txHash = Keys.toChecksumAddress(txHash);
            String result = "";
            String front = txHash.substring(0, frontCharCount + 2);
            String back = txHash.substring(txHash.length() - 4);
            return result + front + "..." + back;
        }
        else
        {
            return "0x";
        }
    }

    public static boolean isTxHashValid(String txHash)
    {
        return !TextUtils.isEmpty(txHash) &&
            WalletUtils.isValidAddress(txHash, 64);
    }

    /**
     * Just enough for diagnosis of most errors
     *
     * @param s String to be HTML escaped
     * @return escaped string
     */
    public static String escapeHTML(String s)
    {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++)
        {
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
        long days = pendingTimeInSeconds / (60 * 60 * 24);
        pendingTimeInSeconds -= (days * 60 * 60 * 24);
        long hours = pendingTimeInSeconds / (60 * 60);
        pendingTimeInSeconds -= (hours * 60 * 60);
        long minutes = pendingTimeInSeconds / 60;
        long seconds = pendingTimeInSeconds % 60;

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

    public static String shortConvertTimePeriodInSeconds(long pendingTimeInSeconds, Context ctx)
    {
        long days = pendingTimeInSeconds / (60 * 60 * 24);
        pendingTimeInSeconds -= (days * 60 * 60 * 24);
        long hours = pendingTimeInSeconds / (60 * 60);
        pendingTimeInSeconds -= (hours * 60 * 60);
        long minutes = pendingTimeInSeconds / 60;
        long seconds = pendingTimeInSeconds % 60;

        String timeStr;

        if (pendingTimeInSeconds == -1)
        {
            timeStr = ctx.getString(R.string.never);
        }
        else if (days > 0)
        {
            timeStr = ctx.getString(R.string.day_single);
        }
        else if (hours > 0)
        {
            if (hours == 1 && minutes == 0)
            {
                timeStr = ctx.getString(R.string.hour_single);
            }
            else
            {
                BigDecimal hourStr = BigDecimal.valueOf(hours + (double) minutes / 60.0)
                        .setScale(1, RoundingMode.HALF_DOWN); //to 1 dp
                timeStr = ctx.getString(R.string.hour_plural, hourStr.toString());
            }
        }
        else if (minutes > 0)
        {
            if (minutes == 1 && seconds == 0)
            {
                timeStr = ctx.getString(R.string.minute_single);
            }
            else
            {
                BigDecimal minsStr = BigDecimal.valueOf(minutes + (double) seconds / 60.0)
                        .setScale(1, RoundingMode.HALF_DOWN); //to 1 dp
                timeStr = ctx.getString(R.string.minute_plural, minsStr.toString());
            }
        }
        else
        {
            if (seconds == 1)
            {
                timeStr = ctx.getString(R.string.second_single);
            }
            else
            {
                timeStr = ctx.getString(R.string.second_plural, String.valueOf(seconds));
            }
        }

        return timeStr;
    }

    public static String localiseUnixTime(Context ctx, long timeStampInSec)
    {
        Date date = new Date(timeStampInSec * DateUtils.SECOND_IN_MILLIS);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(ctx));
        return timeFormat.format(date);
    }

    public static String localiseUnixDate(Context ctx, long timeStampInSec)
    {
        Date date = new Date(timeStampInSec * DateUtils.SECOND_IN_MILLIS);
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, LocaleUtils.getDeviceLocale(ctx));
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtils.getDeviceLocale(ctx));
        return timeFormat.format(date) + " | " + dateFormat.format(date);
    }

    public static long randomId()
    {
        return new Date().getTime();
    }

    public static String getDomainName(String url)
    {
        try
        {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        }
        catch (Exception e)
        {
            return url != null ? url : "";
        }
    }

    @NotNull
    public static String getTokenAddrFromUrl(String url)
    {
        if (!TextUtils.isEmpty(url) && url.startsWith(TRUST_ICON_REPO_BASE))
        {
            int start = url.lastIndexOf("/assets/") + "/assets/".length();
            int end = url.lastIndexOf(TOKEN_LOGO);
            if (start > 0 && end > 0)
            {
                return url.substring(start, end);
            }
        }

        return "";
    }

    @NotNull
    public static String getTokenAddrFromAWUrl(String url)
    {
        if (!TextUtils.isEmpty(url) && url.startsWith(ALPHAWALLET_REPO_NAME))
        {
            int start = ALPHAWALLET_REPO_NAME.length();
            int end = url.lastIndexOf(TOKEN_LOGO);
            if (end > 0 && end > start)
            {
                return url.substring(start, end);
            }
        }

        return "";
    }

    private static final Map<Long, String> twChainNames = new HashMap<Long, String>()
    {
        {
            put(CLASSIC_ID, "classic");
            put(GNOSIS_ID, "xdai");
            put(BINANCE_MAIN_ID, "smartchain");
            put(AVALANCHE_ID, "avalanche");
            put(OPTIMISTIC_MAIN_ID, "optimism");
            put(POLYGON_ID, "polygon");
            put(MAINNET_ID, "ethereum");
        }
    };

    @NotNull
    public static String getTWTokenImageUrl(long chainId, String address)
    {
        String tURL = TRUST_ICON_REPO;
        String repoChain = twChainNames.get(chainId);
        if (repoChain == null) repoChain = "ethereum";
        tURL = tURL.replace(ICON_REPO_ADDRESS_TOKEN, Keys.toChecksumAddress(address)).replace(CHAIN_REPO_ADDRESS_TOKEN, repoChain);
        return tURL;
    }

    @NotNull
    public static String getTokenImageUrl(String address)
    {
        return ALPHAWALLET_ICON_REPO.replace(ICON_REPO_ADDRESS_TOKEN, address.toLowerCase());
    }

    public static boolean isContractCall(Context context, String operationName)
    {
        return !TextUtils.isEmpty(operationName) && context.getString(R.string.contract_call).equals(operationName);
    }

    private static final String IPFS_PREFIX = "ipfs://";
    private static final String IPFS_DESIGNATOR = "/ipfs/";
    public static final String IPFS_INFURA_RESOLVER = "https://alphawallet.infura-ipfs.io";
    public static final String IPFS_MATCHER = "^Qm[1-9A-Za-z]{44}(\\/.*)?$";

    public static boolean isIPFS(String url)
    {
        return url.contains(IPFS_DESIGNATOR) || url.startsWith(IPFS_PREFIX) || shouldBeIPFS(url);
    }

    public static String parseIPFS(String URL)
    {
        return resolveIPFS(URL, IPFS_INFURA_RESOLVER);
    }

    public static String resolveIPFS(String URL, String resolver)
    {
        if (TextUtils.isEmpty(URL)) return URL;
        String parsed = URL;
        int ipfsIndex = URL.lastIndexOf(IPFS_DESIGNATOR);
        if (ipfsIndex >= 0)
        {
            parsed = resolver + URL.substring(ipfsIndex);
        }
        else if (URL.startsWith(IPFS_PREFIX))
        {
            parsed = resolver + IPFS_DESIGNATOR + URL.substring(IPFS_PREFIX.length());
        }
        else if (shouldBeIPFS(URL)) //have seen some NFTs designating only the IPFS hash
        {
            parsed = resolver + IPFS_DESIGNATOR + URL;
        }

        return parsed;
    }

    public static boolean shouldBeIPFS(String url)
    {
        Matcher regexResult = Pattern.compile(IPFS_MATCHER).matcher(url);
        return regexResult.find();
    }

    public static String loadFile(Context context, @RawRes int rawRes)
    {
        byte[] buffer = new byte[0];
        try
        {
            InputStream in = context.getResources().openRawResource(rawRes);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1)
            {
                throw new IOException("Nothing is read.");
            }
        }
        catch (Exception ex)
        {
            Timber.tag("READ_JS_TAG").d(ex, "Ex");
        }

        try
        {
            Timber.tag("READ_JS_TAG").d("HeapSize:%s", Runtime.getRuntime().freeMemory());
            return new String(buffer);
        }
        catch (Exception e)
        {
            Timber.tag("READ_JS_TAG").d(e, "Ex");
        }
        return "";
    }

    public static long timeUntil(long eventInMillis)
    {
        return eventInMillis - System.currentTimeMillis();
    }

    //TODO: detect various App Library installs and re-direct appropriately
    public static boolean verifyInstallerId(Context context)
    {
        try
        {
            PackageManager packageManager = context.getPackageManager();
            String installingPackageName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            {
                final InstallSourceInfo installer = packageManager.getInstallSourceInfo(context.getPackageName());
                installingPackageName = installer.getInstallingPackageName();
            }
            else
            {
                installingPackageName = packageManager.getInstallerPackageName(context.getPackageName());
            }
            // A list with valid installers package name
            List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));

            // true if your app has been downloaded from Play Store
            return installingPackageName != null && validInstallers.contains(installingPackageName);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            return false;
        }
    }

    public static boolean isTransactionHash(String input)
    {
        if (input == null || (input.length() != 66 && input.length() != 64)) return false;
        String cleanInput = Numeric.cleanHexPrefix(input);

        try
        {
            Numeric.toBigIntNoPrefix(cleanInput);
        }
        catch (NumberFormatException e)
        {
            return false;
        }

        return cleanInput.length() == 64;
    }

    public static @ColorInt
    int getColorFromAttr(Context context, int resId)
    {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(resId, typedValue, true);
        return typedValue.data;
    }

    public static String calculateContractAddress(String account, long nonce)
    {
        byte[] addressAsBytes = Numeric.hexStringToByteArray(account);
        byte[] calculatedAddressAsBytes =
                Hash.sha3(RlpEncoder.encode(
                        new RlpList(
                                RlpString.create(addressAsBytes),
                                RlpString.create((nonce)))));

        calculatedAddressAsBytes = Arrays.copyOfRange(calculatedAddressAsBytes,
                12, calculatedAddressAsBytes.length);
        return Keys.toChecksumAddress(Numeric.toHexString(calculatedAddressAsBytes));
    }

    public static <T> List<Type> decodeDynamicArray(String output)
    {
        List<TypeReference<Type>> adaptive = org.web3j.abi.Utils.convert(Collections.singletonList(new TypeReference<DynamicArray<Utf8String>>() {}));
        try
        {
            return FunctionReturnDecoder.decode(output, adaptive);
        }
        catch (Exception e)
        {
            // Expected
        }

        return new ArrayList<>();
    }

    public static <T> List<T> asAList(List<Type> responseValues, T convert)
    {
        List<T> converted = new ArrayList<>();
        if (responseValues.isEmpty())
        {
            return converted;
        }

        for (Object objUri : ((DynamicArray) responseValues.get(0)).getValue())
        {
            try
            {
                converted.add((T) ((Type<?>) objUri).getValue().toString());
            }
            catch (ClassCastException e)
            {
                //
            }
        }

        return converted;
    }

    public static boolean isJson(String value)
    {
        try
        {
            JSONObject stateData = new JSONObject(value);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static BigInteger stringToBigInteger(String value)
    {
        if (TextUtils.isEmpty(value)) return BigInteger.ZERO;
        try
        {
            if (Numeric.containsHexPrefix(value))
            {
                return Numeric.toBigInt(value);
            }
            else
            {
                return new BigInteger(value);
            }
        }
        catch (NumberFormatException e)
        {
            Timber.e(e);
            return BigInteger.ZERO;
        }
    }

    public static boolean stillAvailable(Context context)
    {
        if (context == null)
        {
            return false;
        }
        else if (context instanceof FragmentActivity)
        {
            return !((FragmentActivity) context).isDestroyed();
        }
        else if (context instanceof Activity)
        {
            return !((Activity) context).isDestroyed();
        }
        else if (context instanceof ContextWrapper)
        {
            return stillAvailable(((ContextWrapper) context).getBaseContext());
        }
        else
        {
            return false;
        }
    }

    public static ScanOptions getQRScanOptions(Context ctx)
    {
        ScanOptions options = new ScanOptions();
        options.addExtra(Intents.Scan.SCAN_TYPE, Intents.Scan.MIXED_SCAN);
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setPrompt(ctx.getString(R.string.message_scan_camera));
        return options;
    }

    public static String removeDoubleQuotes(String string)
    {
        return string != null ? string.replace("\"", "") : null;
    }

    public static boolean isAlphaWallet(Context context)
    {
        return context.getPackageName().equals("io.stormbird.wallet");
    }

    //Decode heuristic:
    //1. use Android URI parser to extract "ticket" or "attestation"
    //2. do URL decode on the extracted string
    //3. Base64 decode and unzip, return decoded string if any
    //4. Try the decode step "_ -> /" and "- -> +" and try Base64 decode and unzip, return decoded string
    //5. Try EAS format: extract tag from "ticket=" or "#attestation=" and try Base64 decode and unzip.
    public static String parseEASAttestation(String data)
    {
        String inflate;
        String attestation = attestationViaParams(data);
        if (!TextUtils.isEmpty(attestation))
        {
            //try decode without conversion
            inflate = wrappedInflateData(attestation);
            if (!TextUtils.isEmpty(inflate))
            {
                return inflate;
            }
        }

        //now check via pulling params directly
        attestation = extractParam(data, SMART_PASS_PREFIX);
        inflate = wrappedInflateData(attestation);
        if (!TextUtils.isEmpty(inflate))
        {
            return inflate;
        }

        attestation = extractParam(data, ATTESTATION_PREFIX);
        return wrappedInflateData(attestation);
    }

    public static boolean hasEASAttestation(String data)
    {
        return parseEASAttestation(data).length() > 0;
    }

    //Used to pull the raw attestation zip from the magiclink
    public static String extractRawAttestation(String data)
    {
        String inflate;
        String attestation = attestationViaParams(data);
        if (!TextUtils.isEmpty(attestation))
        {
            //try decode without conversion
            inflate = inflateData(attestation);
            if (!TextUtils.isEmpty(inflate))
            {
                return attestation;
            }
            String decoded = attestation.replace("_", "/").replace("-", "+");
            inflate = inflateData(decoded);
            if (!TextUtils.isEmpty(inflate))
            {
                return attestation;
            }
        }

        //now check via pulling params directly
        attestation = extractParam(data, SMART_PASS_PREFIX);
        inflate = inflateData(attestation);
        if (!TextUtils.isEmpty(inflate))
        {
            return attestation;
        }

        return extractParam(data, ATTESTATION_PREFIX);
    }

    private static String extractParam(String url, String param)
    {
        int paramIndex = url.indexOf(param);
        String decoded;
        try
        {
            if (paramIndex >= 0) //EAS style attestations have the magic link style
            {
                url = url.substring(paramIndex + param.length());
                decoded = universalURLDecode(url);
                //find end param if there is one
                int endIndex = decoded.indexOf("&");
                if (endIndex > 0)
                {
                    decoded = decoded.substring(0, endIndex);
                }
            }
            else
            {
                decoded = url;
            }
        }
        catch (Exception e)
        {
            decoded = url;
        }

        Timber.d("decoded url: %s", decoded);
        return decoded;
    }

    private static String attestationViaParams(String url)
    {
        String decoded = "";
        try
        {
            Uri uri = Uri.parse(url);
            String payload = uri.getQueryParameter("ticket");
            if (TextUtils.isEmpty(payload))
            {
                payload = uri.getQueryParameter("attestation");
            }

            if (TextUtils.isEmpty(payload))
            {
                return "";
            }

            decoded = universalURLDecode(payload);
            Timber.d("decoded url: %s", decoded);
        }
        catch (Exception e)
        {
            // Expected
        }
        return decoded;
    }

    public static String universalURLDecode(String url)
    {
        String decoded;
        try
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
            }
            else
            {
                decoded = URLDecoder.decode(url, "UTF-8");
            }
        }
        catch (Exception e)
        {
            decoded = url;
        }

        return decoded;
    }

    public static String toAttestationJson(String jsonString)
    {
        if (TextUtils.isEmpty(jsonString))
        {
            return "";
        }

        // Remove the square brackets
        jsonString = jsonString.substring(1, jsonString.length() - 1);
        String[] e = jsonString.split(",");

        // Clean the strings
        for (int i = 0; i < e.length; i++) {
            e[i] = e[i].trim();
            e[i] = e[i].replaceAll("\"", "");
        }

        long versionParam = 0;

        if (e.length < 16 || e.length > 17)
        {
            return "";
        }

        if (e.length == 17)
        {
            versionParam = Long.parseLong(e[16]);
        }

        EasAttestation easAttestation =
            new EasAttestation(
                e[0],
                Long.parseLong(e[1]),
                e[2],
                e[3],
                e[4],
                Long.parseLong(e[5]),
                e[6],
                e[7],
                e[8],
                e[9],
                Long.parseLong(e[10]),
                Long.parseLong(e[11]),
                e[12],
                Boolean.parseBoolean(e[13]),
                e[14],
                Long.parseLong(e[15]),
                versionParam
            );

        return new Gson().toJson(easAttestation);
    }

    private static String wrappedInflateData(String deflatedData)
    {
        String inflatedData = inflateData(deflatedData);
        if (TextUtils.isEmpty(inflatedData))
        {
            deflatedData = deflatedData.replace("_", "/").replace("-", "+");
            inflatedData = inflateData(deflatedData);
        }

        return inflatedData;
    }

    public static String inflateData(String deflatedData)
    {
        try
        {
            byte[] deflatedBytes = Base64.decode(deflatedData, Base64.DEFAULT);

            Inflater inflater = new Inflater();
            inflater.setInput(deflatedBytes);

            byte[] inflatedData;

            // Inflate the data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (!inflater.finished())
            {
                int inflatedBytes = inflater.inflate(buffer);
                outputStream.write(buffer, 0, inflatedBytes);
            }
            inflater.end();

            inflatedData = outputStream.toByteArray();

            // Convert the inflated bytes to a string
            return new String(inflatedData);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static boolean isDefaultName(String name, Context ctx)
    {
        //wallet.name = getString(R.string.wallet_name_template, walletCount);
        String walletStr = ctx.getString(R.string.wallet_name_template, 1);
        String[] walletSplit = walletStr.split(" ");
        walletStr = walletSplit[0];
        if (!TextUtils.isEmpty(name) && name.startsWith(walletStr) && walletSplit.length == 2)
        {
            //check last part is a number
            int walletNum = getWalletNum(walletSplit);
            return walletNum > 0;
        }
        else
        {
            return false;
        }
    }

    private static int getWalletNum(String[] walletSplit)
    {
        if (walletSplit.length != 2)
        {
            return 0;
        }

        String walletNum = walletSplit[1];
        try
        {
            return Integer.parseInt(walletNum);
        }
        catch (Exception e)
        {
            //
        }

        return 0;
    }

    // Detect if we're running in test mode. Don't use keys in test mode
    public static synchronized boolean isRunningTest()
    {
        if (!BuildConfig.DEBUG)
        {
            return false;
        }

        boolean istest;

        try
        {
            Class.forName("androidx.test.espresso.Espresso");
            istest = true;
        }
        catch (ClassNotFoundException e)
        {
            istest = false;
        }

        return istest;
    }

    public static String parseResponseValue(@Nullable String metaDataURI, BigInteger tokenId)
    {
        if (metaDataURI != null && metaDataURI.contains(TOKEN_ID_CODE))
        {
            String formattedTokenId = Numeric.toHexStringNoPrefixZeroPadded(tokenId, 64);
            return metaDataURI.replace(TOKEN_ID_CODE, formattedTokenId);
        }
        else
        {
            return metaDataURI;
        }
    }

    public static boolean isDivisibleString(String originalText)
    {
        return !TextUtils.isEmpty(originalText) && originalText.length() <= 64;
    }
}
