package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.util.Helper.clickListItem;

import org.junit.Test;

public class SelectNetworkTest extends BaseE2ETest
{
    @Test
    public void title_should_update_count()
    {
        createNewWallet();
        gotoSettingsPage();
        selectMenu("Select Active Networks");
        shouldSee("Enabled Networks (1)");
        clickListItem(R.id.test_list, withSubstring("Gnosis"));
        shouldSee("Enabled Networks (2)");
    }
}
