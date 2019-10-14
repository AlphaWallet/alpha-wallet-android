package com.alphawallet.app.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.webkit.URLUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.repository.EthereumNetworkRepository;

import org.web3j.crypto.WalletUtils;

public class Utils {

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
        return WalletUtils.isValidAddress(address);
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

    public static String[] stringListToArray(String list)
    {
        //convert to array
        String[] split = list.split(",");
        List<String> strList = new ArrayList<>();
        Collections.addAll(strList, split);
        return strList.toArray(new String[0]);
    }
}
