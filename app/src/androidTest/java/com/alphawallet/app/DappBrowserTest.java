package com.alphawallet.app;

import static androidx.test.espresso.Espresso.pressBack;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.navigateToBrowser;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.visit;

import com.alphawallet.app.util.Helper;
import com.alphawallet.app.util.SnapshotUtil;

import org.junit.Test;

public class DappBrowserTest extends BaseE2ETest
{

    @Test
    public void should_switch_network()
    {
        String urlString = "https://opensea.io";

        createNewWallet();
        visit(urlString);
        shouldSee("Ethereum");
        Helper.wait(5);
        SnapshotUtil.take("1");
        selectTestNet("Görli");
        navigateToBrowser();
        Helper.wait(3);
        pressBack();
        shouldSee("Görli");
    }
}
