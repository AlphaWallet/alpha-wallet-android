package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.steps.Steps.toggleSwitch;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.clickListItem;

import androidx.test.espresso.contrib.RecyclerViewActions;

import com.alphawallet.app.util.Helper;

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
        clickListItem(R.id.main_list, withSubstring("Aurora")); // scroll to bottom
        toggleSwitch(R.id.testnet_header);
        click(withText(R.string.action_enable_testnet));
        pressBack();
        selectMenu("Select Active Networks");
        clickListItem(R.id.main_list, withSubstring("Aurora")); // scroll to bottom
        clickListItem(R.id.test_list, withSubstring("Görli"));
        shouldSee("Enabled Networks (2)");
        pressBack();
//        selectMenu("Select Active Networks");
//        shouldSee("Enabled Networks (2)");
//        clickListItem(R.id.test_list, withSubstring("Gnosis")); // testnet are visible
//        onView(withId(R.id.testnet_header)).check(matches(isChecked()));
//        pressBack();
    }
}
