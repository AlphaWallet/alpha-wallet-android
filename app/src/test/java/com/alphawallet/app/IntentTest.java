package com.alphawallet.app;

import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.entity.DeepLinkRequest;
import com.alphawallet.app.entity.DeepLinkType;
import com.alphawallet.app.service.DeepLinkService;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowKeyProviderFactory;
import com.alphawallet.shadows.ShadowKeyService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

//Note that these tests may go 'stale' if ownership of the ENS domains changes or avatars change. This is not expected to happen frequently.
@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowKeyProviderFactory.class, ShadowKeyService.class})
public class IntentTest
{
    @Test
    public void intentsAreParsedCorrectly()
    {
        // WalletConnect
        String walletconnectTest1 = "https://aw.app/wc?uri=wc%3Ad3995aaf65b26f95e6047aa93dec800255b6d763823472238b8137dc3ff519c3%402%3Frelay-protocol%3Dirn%26symKey" +
                "%3D6ce01accc21d0b59752107d3d43c303ee7ccbdb5f418923c9b12742630f4c792";
        String walletconnectTest2 = "wc:d3995aaf65b26f95e6047aa93dec800255b6d763823472238b8137dc3ff519c3%402%3Frelay-protocol%3Dirn%26symKey" +
                "%3D6ce01accc21d0b59752107d3d43c303ee7ccbdb5f418923c9b12742630f4c792";
        String walletconnectTest3 = "awallet://wc?uri=wc%3A5f577f99-2f54-40f7-9463-7ff640772090%401%3Fbridge%3Dhttps%253A%252F%252Fwalletconnect.depay.com%26key%3D1938aa2c9d4104c91cbc60e94631cf769c96ebad1ea2fc30e18ba09e39bc3c0b";
        String walletconnectTest4 = "awallet://openURL?q=wc%3A115a1223589295f91056be883897ce4edef1525f0becdec54b24cb6e296e9a40%402%3Frelay-protocol%3Dirn%26symKey%3Dbf97933cebac73de3106e3deff551ec09b0d2a13ac1ed40d7dc8e33de1a4f86b";

        String attestationIntent = "awallet://openURL?q=https%3A%2F%2Fsmart-layer.vercel.app%2Fpass%3Ftype%3Deas%26ticket%3DeNrFU0luHDEM_EufBwF3kUfbnflE4INEUQ8wEiDPj3q-0IBDQSvIIotQ_TrgB9nxQETVvTwO-PtB1mK9n9ymCrt_lAQ_8XxvygQnsRU6ah2Xs3uGjl41uyKEFEwlsNZUbdbYAdqXUFApoZl001YRAdJWkvcXiFaKTmQ2XrUWEU0jqjArg2jWcq3pmcyLNRC4iXBijbWSMfF4ULtwRGxzALd402l6PpsN15-Up4_xPOcbZbeS-UraFsGqBmni1Xs4sQzHYbkZiDj3SITu6sGbkE-IXZxXDhrpaC8QuGkXyO6-BYopIz3g9fD760_dh_ebxeVddvgfoy_ryjIFlVQk9x6i-3N-n3Umjj0aM8M-8zW_L70gCMzF3TJ4BcAW8EKmwt5tzcCapbgV5ZoOcnXGcEHAjLA09vTLHWUMjzFCbF9qRZ8JW60T5qQuTZKbKzYzLNn6m2trZkukQzDelQd8_gOdBAEY%26secret%3D0x1a89af8bed1c8ce25e2737a7e3990a7f302f29094380234d6adf9ec62b7b3636%26id%3Dweihong0427%2540163.com";

        DeepLinkRequest request = DeepLinkService.parseIntent(walletconnectTest1, null);
        assertTrue(request.type == DeepLinkType.WALLETCONNECT);
        assertTrue(request.data.startsWith("wc:"));

        request = DeepLinkService.parseIntent(walletconnectTest2, null);
        assertTrue(request.type == DeepLinkType.WALLETCONNECT);
        assertTrue(request.data.startsWith("wc:"));

        request = DeepLinkService.parseIntent(walletconnectTest3, null);
        assertTrue(request.type == DeepLinkType.WALLETCONNECT);
        assertTrue(request.data.startsWith("wc:"));

        request = DeepLinkService.parseIntent(walletconnectTest4, null);
        assertTrue(request.type == DeepLinkType.WALLETCONNECT);
        assertTrue(request.data.startsWith("wc:"));

        //SmartPass import
        String sp = "https://aw.app/openurl?url=?ticket=eNrFk02OGzAIRu-SdVRhwGCWk0lziaoLfswBqlbq8evMHCFSB3ll2e8DpPfjAt9QLtdxvcDft4GgD7rdHJG_Y1pSyn36Q-gOO8Zc-L7n0svzMUw37_JKnRLFsIoz02JVZEiotsQ07gnskZDmgo4521iaPiE4ta2bo7HHajVi0zW82MeeOVDVypDZdeXQYVukIcyB8nTjlyvqkyPy9hCFh1YrNQK8s9_mJCDoc3bfWM-H-0eoB5UtVeWyzr0nKW7J6p60BKmrNp8QJF4CprVIxqId1Fkw4nP8F-sJOWsX4xODYFf4uPj9689-Hb9ebC7hS2u8TFBRMWUhaTHZ_38Cwq9cIA9gdJvCDSURR7AC34Kr0Plo2tKqySE8yUj3YDuqEeBYIsXmy2H1dgB7Cl6hCMJ0xCvEozYuDa3hMjVzxBrlVDNjL8IRfhzHV_WAn_8AxSYC3Q==&secret=0x2eb74750df993a163a95db3031359f0bfca6eb56e597f6ede056bd69d1803196&id=geman%40gemanji.com";
        request = DeepLinkService.parseIntent(sp, null);
        assertTrue(request.type == DeepLinkType.SMARTPASS);

        request = DeepLinkService.parseIntent(attestationIntent, null);
        assertTrue(request.type == DeepLinkType.SMARTPASS);

        String sp2 = "https://aw.app/openurl?url=ticket=eNrFk02OGzAIRu-SdVRhwGCWk0lziaoLfswBqlbq8evMHCFSB3ll2e8DpPfjAt9QLtdxvcDft4GgD7rdHJG_Y1pSyn36Q-gOO8Zc-L7n0svzMUw37_JKnRLFsIoz02JVZEiotsQ07gnskZDmgo4521iaPiE4ta2bo7HHajVi0zW82MeeOVDVypDZdeXQYVukIcyB8nTjlyvqkyPy9hCFh1YrNQK8s9_mJCDoc3bfWM-H-0eoB5UtVeWyzr0nKW7J6p60BKmrNp8QJF4CprVIxqId1Fkw4nP8F-sJOWsX4xODYFf4uPj9689-Hb9ebC7hS2u8TFBRMWUhaTHZ_38Cwq9cIA9gdJvCDSURR7AC34Kr0Plo2tKqySE8yUj3YDuqEeBYIsXmy2H1dgB7Cl6hCMJ0xCvEozYuDa3hMjVzxBrlVDNjL8IRfhzHV_WAn_8AxSYC3Q==&secret=0x2eb74750df993a163a95db3031359f0bfca6eb56e597f6ede056bd69d1803196&id=geman%40gemanji.com";
        request = DeepLinkService.parseIntent(sp2, null);
        assertTrue(request.type == DeepLinkType.SMARTPASS);

        //Test URL Redirect
        String url = "https://aw.app/openurl?url=https://ratyeeting.com?ratlaunch=5000";
        request = DeepLinkService.parseIntent(url, null);
        assertTrue(request.type == DeepLinkType.URL_REDIRECT);
        assertTrue(request.data.equals("https://ratyeeting.com?ratlaunch=5000"));

        //test awallet style link
        url = "awallet://openURL?q=https://ratyeeting.com?ratlaunch=5000";
        request = DeepLinkService.parseIntent(url, null);
        assertTrue(request.type == DeepLinkType.URL_REDIRECT);
        assertTrue(request.data.equals("https://ratyeeting.com?ratlaunch=5000"));

        url = "https://aw.app/openurl";
        Intent startIntent = new Intent();
        startIntent.putExtra("url", "https://wombatcave.com.au");
        request = DeepLinkService.parseIntent(url, startIntent);
        assertTrue(request.type == DeepLinkType.URL_REDIRECT);
        assertTrue(request.data.equals("https://wombatcave.com.au"));

        request = DeepLinkService.parseIntent(null, startIntent);
        assertTrue(request.type == DeepLinkType.URL_REDIRECT);
        assertTrue(request.data.equals("https://wombatcave.com.au"));

        //Test TOKEN_NOTIFICATION
        String tkn = "AW://0x62130D3EC0A74D797BD3B1645222843a601F92Ae";
        request = DeepLinkService.parseIntent(tkn, null);
        assertTrue(request.type == DeepLinkType.TOKEN_NOTIFICATION);

        //Test WALLET_API_DEEPLINK
        String deeplink = "https://wombatfinder.com.au/wallet/v1/connect?redirecturl=https://wombatfinder.com.au?metadata={ }";
        request = DeepLinkService.parseIntent(deeplink, null);
        assertTrue(request.type == DeepLinkType.WALLET_API_DEEPLINK);

        //Test LEGACY_MAGICLINK
        String magiclink = "https://aw.app/AgAAAABdBkJ4Y8zvczoJPlvXc7QcltPs42FGSUIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAcRbggtfCALFUcjtYDb5_J-NDJKz5_-d83ZPlCibmWmfM47sENfVNKiXYMrj1Ibjlw3zOLfbZifYD6RVe9Jfd7Eb";
        request = DeepLinkService.parseIntent(magiclink, null);
        assertTrue(request.type == DeepLinkType.LEGACY_MAGICLINK);

        //Test IMPORT_SCRIPT
        String script = "content://this is a fake tokenscript test";
        startIntent = new Intent();
        startIntent.setData(Uri.parse("https://wombatfinder.com.au/wombatden"));
        request = DeepLinkService.parseIntent(script, startIntent);
        assertTrue(request.type == DeepLinkType.IMPORT_SCRIPT);

        //Test INVALID_LINK / invalid start Intent
        String invalid = "crypto owls here";
        request = DeepLinkService.parseIntent(invalid, null);
        assertTrue(request.type == DeepLinkType.INVALID_LINK);

        request = DeepLinkService.parseIntent(null, null);
        assertTrue(request.type == DeepLinkType.INVALID_LINK);
    }
}
