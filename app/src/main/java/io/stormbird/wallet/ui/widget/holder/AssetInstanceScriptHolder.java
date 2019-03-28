package io.stormbird.wallet.ui.widget.holder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.token.util.DateTime;
import io.stormbird.token.util.DateTimeFactory;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.web3.JsInjectorClient;
import io.stormbird.wallet.web3.entity.Address;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by James on 26/03/2019.
 * Stormbird in Singapore
 */
public class AssetInstanceScriptHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1011;

    private final WebView iFrame;
    private final Token token;
    private final LinearLayout detailLayout;
    private final WebView detailFrame;

    private final AssetDefinitionService assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    public AssetInstanceScriptHolder(int resId, ViewGroup parent, Token t, AssetDefinitionService assetService)
    {
        super(resId, parent);
        iFrame = findViewById(R.id.iframe);
        detailLayout = findViewById(R.id.layout_usage_details);
        detailFrame = findViewById(R.id.usage_details);
        iFrame.getSettings().setBuiltInZoomControls(false);
        iFrame.getSettings().setJavaScriptEnabled(true);
        iFrame.getSettings().setDisplayZoomControls(false);
        itemView.setOnClickListener(this);
        assetDefinition = assetService;
        token = t;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        try
        {
            int index = -1;
            for (int i = 0; i < data.tokenIds.size(); i++)
            {
                if (!data.tokenIds.get(i).equals(BigInteger.ZERO))
                {
                    index = i;
                    break;
                }
            }

            if (index == -1)
            {
                fillEmpty();
                return;
            }

            NonFungibleToken nft = assetDefinition.getNonFungibleToken(token.getAddress(), data.tokenIds.get(index));
            String cat = "";
            StringBuilder attrs = new StringBuilder();
            addPair(attrs, "name", token.getTokenTitle(nft));
            addPair(attrs, "symbol", token.tokenInfo.symbol);
            addPair(attrs, "_count", String.valueOf(data.tokenIds.size()));

            for (String attrKey : nft.getAttributes().keySet())
            {
                NonFungibleToken.Attribute attr = nft.getAttribute(attrKey);
                addPair(attrs, attrKey, attr.text);
            }

            String buildToken = "<script> const currentTokenInstance = {\n" +
                    attrs.toString() + //insert token definition
                    "}\n\n" +
                    "class TokenScriptDef {\n" +
                    "        constructor(walletAddress, tokenDef) {\n" +
                    "          this.address = walletAddress;\n" +
                    "          this.token = tokenDef;\n" +
                    "        }\n" +
                    "      }\n\n" +
                    "      const ts = new TokenScriptDef(" + token.getWallet() + ", currentTokenInstance)\n" +
                    "</script>";

            String display = JsInjectorClient.injectJS(tokenDisplay, buildToken); //TODO: replace with 

            iFrame.loadData(display, "text/html", "utf-8");
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    private void addPair(StringBuilder attrs, String name, String value) throws ParseException
    {
        attrs.append(name);
        attrs.append(": ");

        if (name.equals("time"))
        {
            //DateFormat date = android.text.format.DateFormat.getMediumDateFormat(ctx);
            //DateFormat time = android.text.format.DateFormat.getTimeFormat(ctx);
            DateTime dt = DateTimeFactory.getDateTime(value);
            //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyy-mm-dd hh:mm");
            //String dateStr = dateFormat.parse(dt);
            value = "{ venue: new Date(" + dt.toEpoch() + ") }";// ((DateTime) dt).toString();
            attrs.append(value);
        }
        else
        {
            attrs.append("\"");
            attrs.append(value);
            attrs.append("\"");
        }

        attrs.append(",\n");
    }

    private void fillEmpty()
    {
        iFrame.loadData("<html><body>No Data</body></html>", "text/html", "utf-8");
    }

    private String loadInitJs(Context context) {
        String initSrc = loadFile(context, R.raw.init);
        return String.format(initSrc, token.getWallet(), "", token.tokenInfo.chainId);
    }

    private String loadFile(Context context, @RawRes int rawRes) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = context.getResources().openRawResource(rawRes);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            Log.d("READ_JS_TAG", "Ex", ex);
        }
        return new String(buffer);
    }

    @Override
    public void onClick(View v) {

    }


    static final String tokenDisplay = "<html>\n" +
            "<head>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1,  maximum-scale=1, shrink-to-fit=no\">\n" +
            "<link href=\"https://fonts.googleapis.com/css?family=Roboto:100,300,400\" rel=\"stylesheet\">\n" +
            "</head>\n" +
            "\n" +
            "        <style type=\"text/css\">\n" +
            "@font-face {\n" +
            "font-family: 'SourceSansPro-Light';\n" +
            "src: local('SourceSansPro-Light'),url('SourceSansPro-Light.otf') format('opentype');\n" +
            "font-weight: lighter;\n" +
            "}\n" +
            "@font-face {\n" +
            "font-family: 'SourceSansPro-Regular';\n" +
            "src: local('SourceSansPro-Regular'),url('SourceSansPro-Regular.otf') format('opentype');\n" +
            "font-weight: normal;\n" +
            "}\n" +
            "@font-face {\n" +
            "font-family: 'SourceSansPro-Semibold';\n" +
            "src: local('SourceSansPro-Semibold'),url('SourceSansPro-Semibold.otf') format('opentype');\n" +
            "font-weight: bold;\n" +
            "}\n" +
            "@font-face {\n" +
            "font-family: 'SourceSansPro-Bold';\n" +
            "src: local('SourceSansPro-Bold'),url('SourceSansPro-Bold.otf') format('opentype');\n" +
            "font-weight: boldedr;\n" +
            "}\n" +
            ".tbml-count {\n" +
            "font-family: \"SourceSansPro-Bold\";\n" +
            "font-weight: bolder;\n" +
            "font-size: 21px;\n" +
            "color: rgb(47, 187, 79);\n" +
            "}\n" +
            ".tbml-category {\n" +
            "font-family: 'Roboto', sans-serif;\n" +
            //"text-shadow: 4px 4px 4px #aaa;\n" +
            //"font-family: \"SourceSansPro-Light\";\n" +
            "font-weight: 300;\n" +
            "font-size: 21px;\n" +
            "color: rgb(37, 37, 37);\n" +
            "}\n" +
            ".tbml-venue {\n" +
            "font-family: 'Roboto', sans-serif;\n" +
            "font-weight: 300;\n" +
            "font-size: 12px;\n" +
            "color: rgb(0, 0, 0);\n" +
            "}\n" +
            ".tbml-date {\n" +
            "font-family: \"SourceSansPro-Semibold\";\n" +
            "font-weight: bold;\n" +
            "font-size: 16px;\n" +
            "color: rgb(112, 112, 112);\n" +
            "}\n" +
            ".tbml-time {\n" +
            "font-family: \"SourceSansPro-Light\";\n" +
            "font-weight: lighter;\n" +
            "font-size: 16px;\n" +
            "color: rgb(112, 112, 112);\n" +
            "}\n" +
            "html {\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "padding: 0px;\n" +
            "margin: 0px;\n" +
            "}\n" +
            "\n" +
            "div {\n" +
            "margin: 0px;\n" +
            "padding: 0px;\n" +
            "}\n" +
            "</style>\n" +
            "        \n" +
            "\n" +
            "<!-- Script tags in a XSL template with the name \"library\" if you want to import libraries. Not recommended -->\n" +
            "\n" +
            "\n" +
            "          \n" +
            "    <style type=\"text/css\">\n" +
            "      .data-icon {\n" +
            "        height:16px;\n" +
            "        vertical-align: middle:\n" +
            "      }\n" +
            "    </style>\n" +
            "\n" +
            "    <script>\n" +
            "      class Token {\n" +
            "        constructor(tokenInstance) {\n" +
            "          this.props = tokenInstance\n" +
            "        }\n" +
            "\n" +
            "      formatTime(d) {\n" +
            "        return d.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})\n" +
            "      }\n" +
            "\n" +
            "      render() {\n" +
            "      let time\n" +
            "      if (this.props.time == null) {\n" +
            "        time = \"\"\n" +
            "      } else {\n" +
            "        time = this.formatTime(this.props.time.venue)\n" +
            "      }\n" +
            "      const result = `<div>\n" +
            "          <div>\n" +
            "            <span class=\"tbml-count\">x${this.props._count}</span>  <span class=\"tbml-category\">${this.props.name}</span>\n" +
            "          </div>\n" +
            "          <div>\n" +
            "              <span class=\"tbml-venue\">${this.props.venue}</span>\n" +
            "          </div>\n" +
            "           <div style=\"margin: 0px; padding:0px; clear: both; height: 6px\">\n" +
            "           &nbsp;\n" +
            "           </div>\n" +
            "          <div>\n" +
            "            <img src=\"data:image/png;base64,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\" class=\"data-icon\"/>\n" +
            "            <span class=\"tbml-date\">${this.props.time == null ? \"\": this.props.time.venue.toLocaleDateString()}</span>\n" +
            "            <img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACcAAAA5CAYAAAC1fzLeAAAABGdBTUEAALGPC/xhBQAABhdJREFUaAXtWWtsFFUUPnd2u22XYpCHbRcIEptYg6hBEoHYbTDiD4NteJn4SDQmEh8R0ISgJqLgH0kTFXzGPz4S/VMKtCA/0KhdEGIsig8iGgxpCrutUEgUtu0+5vqd2b27d7Y7uzOgZmM6yface893z/nmnjvTO/cIyl5r+2TV0KXB9ZLkXYLkAkk0TdmyclQI8ekkEXz0k5YpFwpsJZtr+6LBwbjcK6RohX+fDhZEw5LEd4LEgfpJDdvfWyiSym6wsuJI9IahS7GjkswOIrmsCDGG1UgpV100409yw8s1FJebSNIdhcTYRyYWxzQ7mMOqw9Fm5dt4+JSsSSepEwPnq84y8oEydpsZN4RE0CO2TocGc0imaCdzYohxYSD6HEk5T8cjff34HVU/IpHK22XzykODi/Lt0tqKQ4NIJc3Ko0RK+c3K/rwNGrhYnKCK9siZE1LS9QqAAc90h0OvqTbL9kj0HczAY1rfSV/N5EW7b7tqWOsbp97XF50ev0TfYkauVUb4fxf+H1dtlvD/NPy/qvqEoF+7wzObec3NVZ2Qo4GWxh1a21KlUfUyBoxo/U3m6F971xz+o0nrs6lsi8dpn50YjbAvGxCNbMxRrd/i5MesBXKdINApRDrXzio9t8+IYobxsNBmZUOqFidSyeNtkejbSMDn1Yb8iW1jpsDalXfC9oTNd2ZgB/tSPpTkmG2RMyNYm9ZaU+P8ClBOYppfbO+NTsFMrFPYjBO5AWQ2jOVuCbRxZf4qJOgL4409LQ0v4dXh+rJeJW7R3a2h9YKMbYhUGNvZBbA8pjvcuA7rzf04ePREjhl0tzY+W0W+WxCoqyRJJgUMY3mMM3tni+u06i66wvU/or2aX94yZdwtTes/ygzGIG1nhYE3vl/u37049Is+zqt+WeRUkGzwKyKgfBWTntNazMm/1TdB7nJntqJnzv5ASKpt7429UninRkB+qD95KyPRBWkp7i3EFWv7zPSbu5bOOq1s/ISbCfGQaisppVmrdCXt5HjPRuYmZVTSTPm+hp57KrELml8Mp/C6TFUZO9HOkTNTRpOk9LgY+hilV3RaK5qcLa3Yx18kw7dCTauSwdr0MaWzFMJ/QAhzmd7npNf5//xNtwVrzSPxEf/4sWZ6NzYVdTpWtPWeyf8zFnShJzxzqg74r3Rsmc5jK3O1itfTOlNUdFonyKlUeZUTM+d1xhTe9ipRnU6yvXd4tt9IVTnZS/WnTH+yu3XaQClMoc0TORJjXyZNeV2hEzdtfGD9DlyTG6zCTKw5NRNepbe0ktiPL5gGr0EyeDHodZwncvzt6TXAleAres25mrk1x2WgIcCfpFd+DSZIds4TCTeeXJFLnIsd7Sd5oxuH5TDYlv0MjKuDyopOa0WTc5VWHBPtRTq+L5cyN3bsbE+7wTHGFbmecOh5tw7/SVxFp3WC3OWmemLm/pcz5+pV4nTnmTNhUVlnwqsiQzelKL0ZBb2VJE3bhoCPD3CAfT8lRQfKRrv85NuaPeB2ukfHfs8PBJ/fJSl9jMubKKLZiNmiwMYYxhY787NhHRquySEQinixHda5XClShYGYJM78UP3ZXmgq13ZNrv1gbAtOH5/SHaJYl8CR0+skjOXVPjGHf6xzn2XTwFyWQv1si9ZVVuWS5pgqhAE9Wh0O1RUW59oOnQ0JM3ESuNzRKKoz3wT8/gc7l1xzslgUrhomksmPsAYXKztXHqURaCoszq2R0jcWiV4EzirM8Y2h1lbNM3dKDWZj4mBs3HeCMJMv6MQwM6eCQVruRIz9sc2omXwP1Bx59sG+2K5f2ZgWsWy/xYlnDunKlyrZiFnphziXBWLd0814BnOvHcMwlu5pafxK2UtJrmqn0ukjeQxXqumHfJumYz3P0dqIT1u5Sim4nn5+INZXWErXwbqOgQN7WkJzcANS7y+lt/VGcdgtm0thcjYhjk+d3bjwg7li1OA/VX5ajc2kVczNgZyV970Qy7r52Nld3sIcmAtz4l7rae1aEjpRP6nxVtRFNyKpn+HlNZwfktEwMI3vry/qg2Jboa1cu84IvoUb6gJOL5VbwzKxOKaxkTkwF+Xvb7lWOHNlqS1ZAAAAAElFTkSuQmCC\" class=\"data-icon\"/>\n" +
            "            <span class=\"tbml-date\">${this.props.countryA}-${this.props.countryB}</span>\n" +
            "            <img src=\"data:image/png;base64,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\" class=\"data-icon\"/>\n" +
            "            <span class=\"tbml-date\">M${this.props.match}</span>\n" +
            "          </div>\n" +
            "          <div>\n" +
            "            <span class=\"tbml-time\">${time}</span>  (<span>${this.props.locality}</span>)\n" +
            "          </div>\n" +
            "      </div>`\n" +
            "      return result\n" +
            "      }\n" +
            "      }\n" +
            "    </script>\n" +
            "                    \n" +
            "\n" +
            "  \n" +
            "          \n" +
            "        <script>\n" +
            "          window.onload = startup;\n" +
            "\n" +
            "          function startup() {\n" +
            "              const currentTokenInstance = ts.token\n" +
            "              const domHtml = new Token(currentTokenInstance).render()\n" +
            "              document.getElementById('root').innerHTML = domHtml\n" +
            "           }\n" +
            "        </script>\n" +
            "    <body>\n" +
            "      <div id=\"root\" >\n" +
            "    </body>\n" +
            "                    \n" +
            "  \n" +
            "</html>\n";
}
