package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.toggleSwitch;
import static com.alphawallet.app.util.Helper.click;

import androidx.test.espresso.action.ViewActions;

import org.junit.Test;

public class ManageNetworkTest extends BaseE2ETest
{
    @Test
    public void should_add_custom_network()
    {
        createNewWallet();
        gotoSettingsPage();
        addNewNetwork("MyTestNet");
        toggleSwitch(R.id.mainnet_header);
        click(withText(R.string.action_enable_testnet));
        shouldSee("MyTestNet");
    }
}
