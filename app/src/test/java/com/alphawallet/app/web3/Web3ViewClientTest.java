package com.alphawallet.app.web3;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.EXTRA_TEXT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowJsInjectorClient;
import com.alphawallet.shadows.ShadowRealm;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowRealm.class, ShadowJsInjectorClient.class})
public class Web3ViewClientTest
{
    private static final String PACKAGE_NAME_OF_TELEGRAM = "org.telegram.messenger";
    private static final String URL_TG_JOIN_INVITE = "tg:join?invite=12345";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Spy
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setUpMockito()
    {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDownMockito()
    {
        Mockito.validateMockitoUsage();
    }

    @Captor
    ArgumentCaptor<Intent> intentCaptor;

    @Test
    public void should_start_trusted_app_if_installed()
    {
        assumeAppInstalled(context, PACKAGE_NAME_OF_TELEGRAM);
        boolean overrideUrlLoading = new Web3ViewClient(context).shouldOverrideUrlLoading(null, URL_TG_JOIN_INVITE);
        assertTrue(overrideUrlLoading);

        Mockito.verify(context).startActivity(intentCaptor.capture());
        Intent expectedIntent = intentCaptor.getValue();
        assertThat(expectedIntent.getAction(), is(ACTION_VIEW));
        assertThat(expectedIntent.getData(), is(Uri.parse(URL_TG_JOIN_INVITE)));
        assertThat(expectedIntent.getPackage(), is(PACKAGE_NAME_OF_TELEGRAM));
        assertThat(expectedIntent.getFlags(), is(FLAG_ACTIVITY_NEW_TASK));
        assertThat(expectedIntent.getStringExtra(EXTRA_TEXT), is(URL_TG_JOIN_INVITE));
    }

    @Test
    public void should_notify_user_if_trusted_app_not_installed() throws PackageManager.NameNotFoundException
    {
        assumeNotAppInstalled(context, PACKAGE_NAME_OF_TELEGRAM);
        boolean overrideUrlLoading = new Web3ViewClient(context).shouldOverrideUrlLoading(null, URL_TG_JOIN_INVITE);
        assertTrue(overrideUrlLoading);

        assertThat(ShadowToast.getTextOfLatestToast(), is("Required App not Installed"));
    }

    @Test
    public void should_not_override_untrusted_app_request()
    {
        WebView webView = Mockito.mock(WebView.class);
        String url= "discord://discordapp.com/channels/someId/someId2/someId3";
        boolean overrideUrlLoading = new Web3ViewClient(context).shouldOverrideUrlLoading(webView, url);
        assertFalse(overrideUrlLoading);
        Mockito.verify(webView, never()).loadUrl(url);
    }

    @Test
    public void should_not_override_main_frame_redirect_request()
    {
        WebResourceRequest request = Mockito.mock(WebResourceRequest.class);
        String url = "https://alphawallet.com";
        Mockito.doReturn(Uri.parse(url)).when(request).getUrl();
        Mockito.doReturn(true).when(request).isRedirect();
        Mockito.doReturn(true).when(request).isForMainFrame();

        WebView webView = Mockito.mock(WebView.class);
        boolean overrideUrlLoading = new Web3ViewClient(context).shouldOverrideUrlLoading(webView, request);
        assertFalse(overrideUrlLoading);

        Mockito.verify(webView, never()).loadUrl(url);
    }

    private void assumeNotAppInstalled(Context context, String packageName) throws PackageManager.NameNotFoundException
    {
        PackageManager mockPm = Mockito.mock(PackageManager.class);
        Mockito.doThrow(PackageManager.NameNotFoundException.class).when(mockPm).getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        Mockito.doReturn(mockPm).when(context).getPackageManager();
    }

    private void assumeAppInstalled(Context context, String packageName)
    {
        PackageManager mockPm = Mockito.mock(PackageManager.class);
        try
        {
            Mockito.doReturn(new PackageInfo()).when(mockPm).getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        Mockito.doReturn(mockPm).when(context).getPackageManager();
    }
}
