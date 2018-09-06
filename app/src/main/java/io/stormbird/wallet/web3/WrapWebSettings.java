package io.stormbird.wallet.web3;

import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.webkit.WebSettings;

class WrapWebSettings extends WebSettings {
    private final WebSettings origin;

    WrapWebSettings(WebSettings origin) {
        this.origin = origin;
    }

    @Override
    public void setSupportZoom(boolean support) {
        origin.setSupportZoom(support);
    }

    @Override
    public boolean supportZoom() {
        return origin.supportZoom();
    }

    @Override
    public void setMediaPlaybackRequiresUserGesture(boolean require) {
        origin.setMediaPlaybackRequiresUserGesture(require);
    }

    @Override
    public boolean getMediaPlaybackRequiresUserGesture() {
        return origin.getMediaPlaybackRequiresUserGesture();
    }

    @Override
    public void setBuiltInZoomControls(boolean enabled) {
        origin.setBuiltInZoomControls(enabled);
    }

    @Override
    public boolean getBuiltInZoomControls() {
        return origin.getBuiltInZoomControls();
    }

    @Override
    public void setDisplayZoomControls(boolean enabled) {
        origin.setDisplayZoomControls(enabled);
    }

    @Override
    public boolean getDisplayZoomControls() {
        return origin.getDisplayZoomControls();
    }

    @Override
    public void setAllowFileAccess(boolean allow) {
        origin.setAllowFileAccess(allow);
    }

    @Override
    public boolean getAllowFileAccess() {
        return origin.getAllowFileAccess();
    }

    @Override
    public void setAllowContentAccess(boolean allow) {
        origin.setAllowContentAccess(allow);
    }

    @Override
    public boolean getAllowContentAccess() {
        return origin.getAllowContentAccess();
    }

    @Override
    public void setLoadWithOverviewMode(boolean overview) {
        throw new UnsupportedOperationException("If you need to disable this option, use an old WebView.");
    }

    @Override
    public boolean getLoadWithOverviewMode() {
        return origin.getLoadWithOverviewMode();
    }

    @Override
    public void setEnableSmoothTransition(boolean enable) {
        origin.setEnableSmoothTransition(enable);
    }

    @Override
    public boolean enableSmoothTransition() {
        return origin.enableSmoothTransition();
    }

    @Override
    public void setSaveFormData(boolean save) {
        origin.setSaveFormData(save);
    }

    @Override
    public boolean getSaveFormData() {
        return origin.getSaveFormData();
    }

    @Override
    public void setSavePassword(boolean save) {
        origin.setSavePassword(save);
    }

    @Override
    public boolean getSavePassword() {
        return origin.getSavePassword();
    }

    @Override
    public void setTextZoom(int textZoom) {
        origin.setTextZoom(textZoom);
    }

    @Override
    public int getTextZoom() {
        return origin.getTextZoom();
    }

    @Override
    public void setDefaultZoom(ZoomDensity zoom) {
        origin.setDefaultZoom(zoom);
    }

    @Override
    public ZoomDensity getDefaultZoom() {
        return origin.getDefaultZoom();
    }

    @Override
    public void setLightTouchEnabled(boolean enabled) {
        origin.setLightTouchEnabled(enabled);
    }

    @Override
    public boolean getLightTouchEnabled() {
        return origin.getLightTouchEnabled();
    }

    @Override
    public void setUseWideViewPort(boolean use) {
        throw new UnsupportedOperationException("If you need to disable this option, use an old WebView.");
    }

    @Override
    public boolean getUseWideViewPort() {
        return true;
    }

    @Override
    public void setSupportMultipleWindows(boolean support) {
        origin.setSupportMultipleWindows(support);
    }

    @Override
    public boolean supportMultipleWindows() {
        return origin.supportMultipleWindows();
    }

    @Override
    public void setLayoutAlgorithm(LayoutAlgorithm l) {
        origin.setLayoutAlgorithm(l);
    }

    @Override
    public LayoutAlgorithm getLayoutAlgorithm() {
        return origin.getLayoutAlgorithm();
    }

    @Override
    public void setStandardFontFamily(String font) {
        origin.setStandardFontFamily(font);
    }

    @Override
    public String getStandardFontFamily() {
        return origin.getStandardFontFamily();
    }

    @Override
    public void setFixedFontFamily(String font) {
        origin.setFixedFontFamily(font);
    }

    @Override
    public String getFixedFontFamily() {
        return origin.getFixedFontFamily();
    }

    @Override
    public void setSansSerifFontFamily(String font) {
        origin.setSansSerifFontFamily(font);
    }

    @Override
    public String getSansSerifFontFamily() {
        return origin.getSansSerifFontFamily();
    }

    @Override
    public void setSerifFontFamily(String font) {
        origin.setSerifFontFamily(font);
    }

    @Override
    public String getSerifFontFamily() {
        return origin.getSerifFontFamily();
    }

    @Override
    public void setCursiveFontFamily(String font) {
        origin.setCursiveFontFamily(font);
    }

    @Override
    public String getCursiveFontFamily() {
        return origin.getCursiveFontFamily();
    }

    @Override
    public void setFantasyFontFamily(String font) {
        origin.setFantasyFontFamily(font);
    }

    @Override
    public String getFantasyFontFamily() {
        return origin.getFantasyFontFamily();
    }

    @Override
    public void setMinimumFontSize(int size) {
        origin.setMinimumFontSize(size);
    }

    @Override
    public int getMinimumFontSize() {
        return origin.getMinimumFontSize();
    }

    @Override
    public void setMinimumLogicalFontSize(int size) {
        origin.setMinimumLogicalFontSize(size);
    }

    @Override
    public int getMinimumLogicalFontSize() {
        return origin.getMinimumLogicalFontSize();
    }

    @Override
    public void setDefaultFontSize(int size) {
        origin.setDefaultFontSize(size);
    }

    @Override
    public int getDefaultFontSize() {
        return origin.getDefaultFontSize();
    }

    @Override
    public void setDefaultFixedFontSize(int size) {
        origin.setDefaultFixedFontSize(size);
    }

    @Override
    public int getDefaultFixedFontSize() {
        return origin.getDefaultFixedFontSize();
    }

    @Override
    public void setLoadsImagesAutomatically(boolean flag) {
        origin.setLoadsImagesAutomatically(flag);
    }

    @Override
    public boolean getLoadsImagesAutomatically() {
        return origin.getLoadsImagesAutomatically();
    }

    @Override
    public void setBlockNetworkImage(boolean flag) {
        origin.setBlockNetworkImage(flag);
    }

    @Override
    public boolean getBlockNetworkImage() {
        return origin.getBlockNetworkImage();
    }

    @Override
    public void setBlockNetworkLoads(boolean flag) {
        origin.setBlockNetworkLoads(flag);
    }

    @Override
    public boolean getBlockNetworkLoads() {
        return origin.getBlockNetworkLoads();
    }

    @Override
    public void setJavaScriptEnabled(boolean flag) {
        throw new UnsupportedOperationException("If you need to disable this option, use an old WebView.");
    }

    @Override
    public void setAllowUniversalAccessFromFileURLs(boolean flag) {
        origin.setAllowUniversalAccessFromFileURLs(flag);
    }

    @Override
    public void setAllowFileAccessFromFileURLs(boolean flag) {
        origin.setAllowFileAccessFromFileURLs(flag);
    }

    @Override
    public void setPluginState(PluginState state) {
        origin.setPluginState(state);
    }

    @Override
    public void setDatabasePath(String databasePath) {
        origin.setDatabasePath(databasePath);
    }

    @Override
    public void setGeolocationDatabasePath(String databasePath) {
        origin.setGeolocationDatabasePath(databasePath);
    }

    @Override
    public void setAppCacheEnabled(boolean flag) {
        origin.setAppCacheEnabled(flag);
    }

    @Override
    public void setAppCachePath(String appCachePath) {
        origin.setAppCachePath(appCachePath);
    }

    @Override
    public void setAppCacheMaxSize(long appCacheMaxSize) {
        origin.setAppCacheMaxSize(appCacheMaxSize);
    }

    @Override
    public void setDatabaseEnabled(boolean flag) {
        origin.setDatabaseEnabled(flag);
    }

    @Override
    public void setDomStorageEnabled(boolean flag) {
        throw new UnsupportedOperationException("If you need to disable this option, use an old WebView.");
    }

    @Override
    public boolean getDomStorageEnabled() {
        return true;
    }

    @Override
    public String getDatabasePath() {
        return origin.getDatabasePath();
    }

    @Override
    public boolean getDatabaseEnabled() {
        return origin.getDatabaseEnabled();
    }

    @Override
    public void setGeolocationEnabled(boolean flag) {
        origin.setGeolocationEnabled(flag);
    }

    @Override
    public boolean getJavaScriptEnabled() {
        return true;
    }

    @Override
    public boolean getAllowUniversalAccessFromFileURLs() {
        return origin.getAllowUniversalAccessFromFileURLs();
    }

    @Override
    public boolean getAllowFileAccessFromFileURLs() {
        return origin.getAllowFileAccessFromFileURLs();
    }

    @Override
    public PluginState getPluginState() {
        return origin.getPluginState();
    }

    @Override
    public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
        origin.setJavaScriptCanOpenWindowsAutomatically(flag);
    }

    @Override
    public boolean getJavaScriptCanOpenWindowsAutomatically() {
        return origin.getJavaScriptCanOpenWindowsAutomatically();
    }

    @Override
    public void setDefaultTextEncodingName(String encoding) {
        origin.setDefaultTextEncodingName(encoding);
    }

    @Override
    public String getDefaultTextEncodingName() {
        return origin.getDefaultTextEncodingName();
    }

    @Override
    public void setUserAgentString(@Nullable String ua) {
        origin.setUserAgentString(ua);
    }

    @Override
    public String getUserAgentString() {
        return origin.getUserAgentString();
    }

    @Override
    public void setNeedInitialFocus(boolean flag) {
        origin.setNeedInitialFocus(flag);
    }

    @Override
    public void setRenderPriority(RenderPriority priority) {
        origin.setRenderPriority(priority);
    }

    @Override
    public void setCacheMode(int mode) {
        origin.setCacheMode(mode);
    }

    @Override
    public int getCacheMode() {
        return origin.getCacheMode();
    }

    @Override
    public void setMixedContentMode(int mode) {
        throw new UnsupportedOperationException("If you need to disable this option, use an old WebView.");
    }

    @Override
    public int getMixedContentMode() {
        return MIXED_CONTENT_ALWAYS_ALLOW;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void setOffscreenPreRaster(boolean enabled) {
        origin.setOffscreenPreRaster(enabled);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean getOffscreenPreRaster() {
        return origin.getOffscreenPreRaster();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void setSafeBrowsingEnabled(boolean enabled) {
        origin.setSafeBrowsingEnabled(enabled);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean getSafeBrowsingEnabled() {
        return origin.getSafeBrowsingEnabled();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void setDisabledActionModeMenuItems(int menuItems) {
        origin.setDisabledActionModeMenuItems(menuItems);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int getDisabledActionModeMenuItems() {
        return origin.getDisabledActionModeMenuItems();
    }
}
