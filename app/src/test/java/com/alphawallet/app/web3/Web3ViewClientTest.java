package com.alphawallet.app.web3;

import static android.content.Intent.ACTION_VIEW;
import static android.content.Intent.EXTRA_TEXT;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowRealm.class, ShadowJsInjectorClient.class})
public class Web3ViewClientTest
{
    private static final String PACKAGE_NAME_OF_TELEGRAM = "org.telegram.messenger";
    private static final String URL_TG_JOIN_INVITE = "tg:join?invite=12345";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

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
    public void should_override_url_loading_and_start_telegram_if_installed()
    {
        Context context = Mockito.spy(ApplicationProvider.getApplicationContext());
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
    public void should_override_url_loading_and_notify_user_if_telegram_not_installed() throws PackageManager.NameNotFoundException
    {
        Context context = Mockito.spy(ApplicationProvider.getApplicationContext());
        assumeNotAppInstalled(context, PACKAGE_NAME_OF_TELEGRAM);
        boolean overrideUrlLoading = new Web3ViewClient(context).shouldOverrideUrlLoading(null, URL_TG_JOIN_INVITE);
        assertTrue(overrideUrlLoading);

        assertThat(ShadowToast.getTextOfLatestToast(), is("Required App not Installed"));
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
