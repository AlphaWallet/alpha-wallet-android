package com.alphawallet.app.web3;

import static androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF;
import static androidx.webkit.WebSettingsCompat.FORCE_DARK_ON;
import static com.alphawallet.app.service.AssetDefinitionService.ASSET_DETAIL_VIEW_NAME;
import static com.alphawallet.app.service.AssetDefinitionService.ASSET_SUMMARY_VIEW_NAME;
import static com.alphawallet.token.tools.TokenDefinition.TOKENSCRIPT_ERROR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokenscript.TokenScriptRenderCallback;
import com.alphawallet.app.entity.tokenscript.WebCompletionCallback;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.FunctionCallback;
import com.alphawallet.app.web3.entity.PageReadyCallback;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.tools.TokenDefinition;

import org.jetbrains.annotations.NotNull;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmResults;
import timber.log.Timber;

/**
 * Created by James on 3/04/2019.
 * Stormbird in Singapore
 */
public class Web3TokenView extends WebView
{
    public static final String RENDERING_ERROR = "<html>" + TOKENSCRIPT_ERROR + "${ERR1}</html>";
    public static final String RENDERING_ERROR_SUPPLIMENTAL = "</br></br>Error in line $ERR1:</br>$ERR2";

    private static final String JS_PROTOCOL_CANCELLED = "cancelled";
    private static final String JS_PROTOCOL_ON_SUCCESSFUL = "executeCallback(%1$s, null, \"%2$s\")";
    private static final String JS_PROTOCOL_ON_FAILURE = "executeCallback(%1$s, \"%2$s\", null)";
    private static final String REFRESH_ERROR = "refresh is not defined";

    private JsInjectorClient jsInjectorClient;
    private TokenScriptClient tokenScriptClient;
    private PageReadyCallback assetHolder;
    private boolean showingError = false;
    private String unencodedPage;
    private RealmResults<RealmAuxData> realmAuxUpdates;

    protected WebCompletionCallback keyPressCallback;

    @Nullable
    private OnSignPersonalMessageListener onSignPersonalMessageListener;
    @Nullable
    private OnSetValuesListener onSetValuesListener;

    public Web3TokenView(@NonNull Context context) {
        super(context);
        init();
    }

    public Web3TokenView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Web3TokenView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        tokenScriptClient = new TokenScriptClient(this);
        jsInjectorClient = new JsInjectorClient(getContext());
        WebSettings webSettings = super.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUserAgentString(webSettings.getUserAgentString()
                                               + "AlphaWallet(Platform=Android&AppVersion=" + BuildConfig.VERSION_NAME + ")");
        WebView.setWebContentsDebuggingEnabled(true);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK))
        {
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            {
                case Configuration.UI_MODE_NIGHT_YES:
                    WebSettingsCompat.setForceDark(getSettings(), FORCE_DARK_ON);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    WebSettingsCompat.setForceDark(getSettings(), FORCE_DARK_OFF);
                    break;
            }
        }

        setScrollBarSize(0);
        setVerticalScrollBarEnabled(false);
        setScrollContainer(false);
        setScrollbarFadingEnabled(true);

        setInitialScale(0);
        clearCache(true);
        showingError = false;

        addJavascriptInterface(new TokenScriptCallbackInterface(
                this,
                innerOnSignPersonalMessageListener,
                innerOnSetValuesListener), "alpha");

        super.setWebViewClient(tokenScriptClient);

        setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage msg)
            {
                if (!showingError && msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR)
                {
                    if (msg.message().contains(REFRESH_ERROR)) return true; //don't stop for refresh error
                    String errorLine = "";
                    try
                    {
                        LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(unencodedPage));
                        lineNumberReader.setLineNumber(0);

                        String lineStr;
                        while ((lineStr = lineNumberReader.readLine()) != null)
                        {
                            if (lineNumberReader.getLineNumber() == msg.lineNumber())
                            {
                                errorLine = Utils.escapeHTML(lineStr); //ensure string is displayed exactly how it is read
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        errorLine = "";
                    }

                    String errorMessage = RENDERING_ERROR.replace("${ERR1}", msg.message());
                    if (!TextUtils.isEmpty(errorLine)) errorMessage += RENDERING_ERROR_SUPPLIMENTAL.replace("$ERR1", String.valueOf(msg.lineNumber())).replace("$ERR2", errorLine); //.replace("$ERR2", errorMessage)
                    showError(errorMessage);
                    unencodedPage = null;
                }
                return true;
            }
        });
    }

    public void showError(String error)
    {
        showingError = true;
        setVisibility(View.VISIBLE);
        loadData(error, "text/html", "utf-8");
    }

    @Override
    public void setWebChromeClient(WebChromeClient client)
    {
        super.setWebChromeClient(client);
    }

    @JavascriptInterface
    public void onValue(String data)
    {
        Timber.d(data);
    }

    public void setWalletAddress(@NonNull Address address)
    {
        jsInjectorClient.setWalletAddress(address);
    }

    public void setupWindowCallback(@NonNull FunctionCallback callback)
    {
        setWebChromeClient(
                new WebChromeClient()
                {
                    @Override
                    public void onCloseWindow(WebView window)
                    {
                        callback.functionSuccess();
                    }
                }
        );
    }

    public void setChainId(long chainId) {
        jsInjectorClient.setChainId(chainId);
    }

    public void setRpcUrl(@NonNull long chainId) {
        jsInjectorClient.setRpcUrl(EthereumNetworkRepository.getDefaultNodeURL(chainId));
    }

    public void onSignPersonalMessageSuccessful(@NotNull Signable message, String signHex) {
        long callbackId = message.getCallbackId();
        callbackToJS(callbackId, JS_PROTOCOL_ON_SUCCESSFUL, signHex);
    }

    public void setKeyboardListenerCallback(WebCompletionCallback cpCallback)
    {
        keyPressCallback = cpCallback;
    }

    @Override
    public String getUrl()
    {
        return "TokenScript";
    }

    public void callToJS(String function) {
        post(() -> evaluateJavascript(function, value -> Timber.tag("WEB_VIEW").d(value)));
    }

    @JavascriptInterface
    public void TScallToJS(String fName, String script, TokenScriptRenderCallback cb)
    {
        post(() -> evaluateJavascript(script, value -> cb.callToJSComplete(fName, value)));
    }

    @JavascriptInterface
    public void callbackToJS(long callbackId, String function, String param) {
        String callback = String.format(function, callbackId, param);
        post(() -> evaluateJavascript(callback, value -> Timber.tag("WEB_VIEW").d(value)));
    }

    public void setOnSignPersonalMessageListener(@Nullable OnSignPersonalMessageListener onSignPersonalMessageListener) {
        this.onSignPersonalMessageListener = onSignPersonalMessageListener;
    }

    public void setOnSetValuesListener(@Nullable OnSetValuesListener onSetValuesListener) {
        this.onSetValuesListener = onSetValuesListener;
    }

    private final OnSignTransactionListener innerOnSignTransactionListener = new OnSignTransactionListener() {
        @Override
        public void onSignTransaction(Web3Transaction transaction, String url) {

        }
    };

    private final OnSignMessageListener innerOnSignMessageListener = new OnSignMessageListener() {
        @Override
        public void onSignMessage(EthereumMessage message) {

        }
    };

    private final OnSignPersonalMessageListener innerOnSignPersonalMessageListener = new OnSignPersonalMessageListener() {
        @Override
        public void onSignPersonalMessage(EthereumMessage message) {
            onSignPersonalMessageListener.onSignPersonalMessage(message);
        }
    };

    private final OnSetValuesListener innerOnSetValuesListener = new OnSetValuesListener() {
        @Override
        public void setValues(Map<String, String> updates)
        {
            if (onSetValuesListener != null) onSetValuesListener.setValues(updates);
        }
    };

    public void onSignCancel(@NotNull Signable message) {
        long callbackId = message.getCallbackId();
        callbackToJS(callbackId, JS_PROTOCOL_ON_FAILURE, JS_PROTOCOL_CANCELLED);
    }

    public void setOnReadyCallback(PageReadyCallback holder)
    {
        assetHolder = holder;
    }

    public String injectWeb3TokenInit(String view, String tokenContent, BigInteger tokenId)
    {
        return jsInjectorClient.injectWeb3TokenInit(getContext(), view, tokenContent, tokenId);
    }

    public String injectJS(String view, String buildToken)
    {
        return jsInjectorClient.injectJS(view, buildToken);
    }

    public String injectJSAtEnd(String view, String JSCode)
    {
        return jsInjectorClient.injectJSAtEnd(view, JSCode);
    }

    public String injectStyleAndWrapper(String viewData, String style)
    {
        return jsInjectorClient.injectStyleAndWrap(viewData, style);
    }

    public void setLayout(Token token, boolean iconified)
    {
        if (iconified && token.itemViewHeight > 0)
        {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, token.itemViewHeight);
            setLayoutParams(params);
        }
    }

    private class TokenScriptClient extends WebViewClient
    {
        public TokenScriptClient(Web3TokenView web3)
        {
            super();
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            unencodedPage = null;
            if (assetHolder != null)
                assetHolder.onPageRendered(view);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url)
        {
            super.onPageCommitVisible(view, url);
            if (assetHolder != null)
                assetHolder.onPageLoaded(view);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
            {
                if (keyPressCallback != null)
                    keyPressCallback.enterKeyPressed();
            }
            super.onUnhandledKeyEvent(view, event);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            if (assetHolder != null)
            {
                return assetHolder.overridePageLoad(view, url);
            }
            else
            {
                return false;
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error)
        {
            showError(RENDERING_ERROR.replace("${ERR1}", error.getDescription()));
        }
    }

    // Rendering
    public void displayTicketHolder(Token token, TicketRange range, AssetDefinitionService assetService) {
        displayTicketHolder(token, range, assetService, true);
    }

    /**
     * This is a single method that populates any instance of graphic ticket anywhere
     *
     * @param range
     * @param assetService
     */
    public void displayTicketHolder(Token token, TicketRange range, AssetDefinitionService assetService, boolean iconified)
    {
        //need to wait until the assetDefinitionService has finished loading assets
        assetService.getAssetDefinitionASync(token.tokenInfo.chainId, token.tokenInfo.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(td -> renderTicketHolder(token, td, range, assetService, iconified), this::loadingError).isDisposed();
    }

    private void loadingError(Throwable e)
    {
        Timber.e(e);
    }

    private void renderTicketHolder(Token token, TokenDefinition td, TicketRange range, AssetDefinitionService assetService, boolean iconified)
    {
        if (td != null && td.holdingToken != null)
        {
            //use webview
            renderTokenscriptView(token, range, assetService, iconified);
        }
        else
        {
            showLegacyView(token, range);
        }
    }

    private void showLegacyView(Token token, TicketRange range)
    {
        setVisibility(View.VISIBLE);
        String displayData = "<!DOCTYPE html>\n" +
                "<html><style>" +
                "h4 { display: inline; color: green; font: 20px Helvetica, Sans-Serif; padding: 6px; font-weight: bold;}\n" +
                "h5 { display: inline; color: black; font: 18px Helvetica, Sans-Serif; font-weight: normal;}\n" +
                "</style>" +
                "<body>" +
                "<div><h4><span>x" + range.tokenIds.size() + "</span></h4>" +
                "<h5><span>" + token.getFullName() + "</span></h5></div>\n" +
                "</body></html>";

        loadData(displayData, "text/html", "utf-8");
    }

    public void renderTokenscriptView(Token token, TicketRange range, AssetDefinitionService assetService, boolean itemView)
    {
        BigInteger tokenId = range.tokenIds.get(0);

        final StringBuilder attrs = assetService.getTokenAttrs(token, tokenId, range.tokenIds.size());

        assetService.resolveAttrs(token, tokenId, assetService.getTokenViewLocalAttributes(token.tokenInfo.chainId, token.tokenInfo.address), itemView)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(attr -> onAttr(attr, attrs), throwable -> onError(token, throwable, range),
                           () -> displayTicket(token, assetService, attrs, itemView, range))
                .isDisposed();
    }

    /**
     * Add the decoded and resolved attributes as Token properties to the relevant view
     *
     * @param assetService
     * @param attrs
     * @param iconified
     * @param range
     */
    private void displayTicket(Token token, AssetDefinitionService assetService, StringBuilder attrs, boolean iconified, TicketRange range)
    {
        setVisibility(View.VISIBLE);
        String viewName = iconified ? ASSET_SUMMARY_VIEW_NAME : ASSET_DETAIL_VIEW_NAME;

        String view = assetService.getTokenView(token.tokenInfo.chainId, token.getAddress(), viewName);
        if (TextUtils.isEmpty(view)) view = buildViewError(token, range, viewName);
        String style = assetService.getTokenViewStyle(token.tokenInfo.chainId, token.getAddress(), viewName);
        unencodedPage = injectWeb3TokenInit(view, attrs.toString(), range.tokenIds.get(0));
        unencodedPage = injectStyleAndWrapper(unencodedPage, style); //style injected last so it comes first

        String base64 = android.util.Base64.encodeToString(unencodedPage.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
        loadData(base64, "text/html; charset=utf-8", "base64");

        if (realmAuxUpdates != null) realmAuxUpdates.removeAllChangeListeners();
        //TODO: Re-do this to use the JavaScript minimal interface
        //now set realm listener ready to refresh view
        Realm realm = assetService.getEventRealm();
        long lastUpdateTime = getLastUpdateTime(realm, token, range.tokenIds.get(0));
        realmAuxUpdates = RealmAuxData.getEventListener(realm, token, range.tokenIds.get(0), 1, lastUpdateTime);
        realmAuxUpdates.addChangeListener(realmAux -> {
            if (realmAux.size() == 0) return;
            //reload token view, updated event will be fetched from DB
            displayTicketHolder(token, range, assetService, iconified);
        });
    }

    private long getLastUpdateTime(Realm realm, Token token, BigInteger tokenId)
    {
        long lastResultTime = 0;
        RealmResults<RealmAuxData> lastEntry = RealmAuxData.getEventQuery(realm, token, tokenId, 1, 0).findAll();
        if (lastEntry.size() > 0)
        {
            RealmAuxData data = lastEntry.first();
            lastResultTime = data.getResultTime();
        }

        return lastResultTime + 1;
    }

    @Override
    public void destroy()
    {
        super.destroy();
        //remove listeners
        if (realmAuxUpdates != null)
        {
            realmAuxUpdates.removeAllChangeListeners();
            if (!realmAuxUpdates.getRealm().isClosed())
            {
                realmAuxUpdates.getRealm().close();
            }
        }
    }

    /**
     * Form TokenScript diagnostic message if relevant view not found
     * @param range
     * @param viewName
     * @return
     */
    private String buildViewError(Token token, TicketRange range, String viewName)
    {
        String displayData = "<h3><span style=\"color:Green\">x" + range.tokenIds.size() + "</span><span style=\"color:Black\"> " + token.getFullName() + "</span></h3>";
        displayData += ("<br /><body>" + getContext().getString(R.string.card_view_not_found_error, viewName) + "</body>");
        return displayData;
    }

    /**
     * Display Token amount and diagnostic, rather than a blank card or error
     *
     * @param throwable
     * @param range
     */
    private void onError(Token token, Throwable throwable, TicketRange range)
    {
        String displayData = "<h3><span style=\"color:Green\">x" + range.tokenIds.size() + "</span><span style=\"color:Black\"> " + token.getFullName() + "</span></h3>";
        if (BuildConfig.DEBUG) displayData += ("<br /><body>" + throwable.getLocalizedMessage() + "</body>");
        loadData(displayData, "text/html", "utf-8");
    }

    /**
     * Encode the resolved attribute into the Token properties declaration, eg 'name: "Entry Token",'
     *
     * @param attribute
     * @param attrs StringBuilder holding the token properties as it's being built
     */
    private void onAttr(TokenScriptResult.Attribute attribute, StringBuilder attrs)
    {
        TokenScriptResult.addPair(attrs, attribute.id, attribute.text);
    }
}
