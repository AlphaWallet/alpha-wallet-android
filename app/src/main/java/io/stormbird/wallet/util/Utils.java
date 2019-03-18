package io.stormbird.wallet.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.webkit.URLUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.TextView;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.repository.EthereumNetworkRepository;

import static io.stormbird.token.entity.MagicLinkInfo.getNetworkNameById;

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
                return C.HTTP_PREFIX + url;
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

    public static void setChainColour(View view, int chainId) {
        switch (chainId) {
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
            case EthereumNetworkRepository.XDAI_ID:
                view.setBackgroundResource(R.drawable.background_xdai);
                break;
        }

        ((TextView)view).setText(getNetworkNameById(chainId));
    }
}
