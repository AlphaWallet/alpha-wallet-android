package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.steps.Steps.toggleSwitch;
import static com.alphawallet.app.util.Helper.click;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

public class RPCNodesTest extends BaseE2ETest
{

    @Test
    public void should_select_network()
    {
        createNewWallet();
        gotoSettingsPage();
        selectMenu("Select Active Networks");
        toggleSwitch(R.id.mainnet_header);
        click(withText(R.string.action_enable_testnet));
        Helper.wait(1);
        click(withId(R.id.action_node_status));
        Helper.wait(3);

    }
}
