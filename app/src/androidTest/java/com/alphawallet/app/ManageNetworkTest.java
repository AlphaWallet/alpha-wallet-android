package com.alphawallet.app;

import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;

import org.junit.Ignore;
import org.junit.Test;

public class ManageNetworkTest extends BaseE2ETest
{
    @Test
    @Ignore
    public void should_add_custom_network()
    {
        createNewWallet();
        gotoSettingsPage();
        addNewNetwork("MyTestNet");
        shouldSee("MyTestNet");
    }
}
