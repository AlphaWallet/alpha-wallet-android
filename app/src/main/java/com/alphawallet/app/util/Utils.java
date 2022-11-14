package com.alphawallet.app.util;

import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.GNOSIS_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POLYGON_ID;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.webkit.URLUtil;

import androidx.annotation.ColorInt;
import androidx.annotation.RawRes;
import androidx.fragment.app.FragmentActivity;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.util.pattern.Patterns;
import com.alphawallet.app.web3j.StructuredDataEncoder;
import com.alphawallet.token.entity.ProviderTypedData;
import com.alphawallet.token.entity.Signable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public class Utils
{
    private static final String ISOLATE_NUMERIC = "(0?x?[0-9a-fA-F]+)";
    private static final String ICON_REPO_ADDRESS_TOKEN = "[TOKEN]";
    private static final String CHAIN_REPO_ADDRESS_TOKEN = "[CHAIN]";
    private static final String TOKEN_LOGO = "/logo.png";
    public static final String ALPHAWALLET_REPO_NAME = "https://raw.githubusercontent.com/alphawallet/iconassets/master/";
    private static final String TRUST_ICON_REPO_BASE = "https://raw.githubusercontent.com/trustwallet/assets/master/blockchains/";
    private static final String TRUST_ICON_REPO = TRUST_ICON_REPO_BASE + CHAIN_REPO_ADDRESS_TOKEN + "/assets/" + ICON_REPO_ADDRESS_TOKEN + TOKEN_LOGO;
    private static final String ALPHAWALLET_ICON_REPO = ALPHAWALLET_REPO_NAME + ICON_REPO_ADDRESS_TOKEN + TOKEN_LOGO;

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
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url))
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
        if (testStr != null && testStr.length() > 0)
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
        int index;
        for (index = 0; index < text.length(); index++)
        {
            if (!Character.isLetterOrDigit(text.charAt(index))) break;
        }

        return text.substring(0, index).trim();
    }

    public static String getIconisedText(String text)
    {
        if (TextUtils.isEmpty(text)) return "";
        if (text.length() <= 4) return text;
        String firstWord = getFirstWord(text);
        if (!TextUtils.isEmpty(firstWord))
        {
            return firstWord.substring(0, Math.min(firstWord.length(), 4)).toUpperCase();
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
            return firstWord.substring(0, Math.min(firstWord.length(), 5)).toUpperCase();
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
                return R.string.dialog_title_sign_message;
            case SIGN_PERSONAL_MESSAGE:
                return R.string.dialog_title_sign_personal_message;
            case SIGN_TYPED_DATA:
            case SIGN_TYPED_DATA_V3:
            case SIGN_TYPED_DATA_V4:
                return R.string.dialog_title_sign_typed_message;
        }
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

    public static CharSequence createFormattedValue(Context ctx, String operationName, Token token)
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
            String lastSix = address.substring(address.length() - 4);
            return result + firstSix + "..." + lastSix;
        }
        else
        {
            return "0x";
        }
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
            put(POA_ID, "poa");
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
    public static final String IPFS_IO_RESOLVER = "https://ipfs.io";

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

    private static boolean shouldBeIPFS(String url)
    {
        return url.startsWith("Qm") && url.length() == 46 && !url.contains(".") && !url.contains("/");
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

    public static String removeDoubleQuotes(String string)
    {
        return string != null ? string.replace("\"", "") : null;
    }
}
