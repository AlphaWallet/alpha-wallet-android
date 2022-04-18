package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.navigateToBrowser;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.visit;
import static com.alphawallet.app.util.Helper.waitUntil;

import com.alphawallet.app.util.Helper;

import org.junit.Ignore;
import org.junit.Test;

public class DappBrowserTest extends BaseE2ETest
{

    @Test
    @Ignore("This test is not working on CI, but works locally, so ignoring for now until we can fix it")
    public void should_switch_network()
    {
        String urlString = "https://opensea.io";

        createNewWallet();
        visit(urlString);
        onView(isRoot()).perform(waitUntil(withText("Ethereum"), 60));
        selectTestNet();
        navigateToBrowser();
        Helper.wait(3);
        pressBack();
        onView(isRoot()).perform(waitUntil(withText("Kovan"), 60));
    }
}
