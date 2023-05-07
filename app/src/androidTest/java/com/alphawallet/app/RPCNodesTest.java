package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.hasBackgroundResource;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

public class RPCNodesTest extends BaseE2ETest
{

    @Test
    public void should_select_network()
    {
        createNewWallet();
        selectTestNet("Mumbai");

        gotoSettingsPage();
        selectMenu("Select Active Networks");
        Helper.wait(1);
        click(withId(R.id.action_node_status));
        Helper.wait(3);

        onView(withId(R.id.main_list)).check(matches(not(hasDescendant(allOf(withId(R.id.image_status), hasBackgroundResource(R.drawable.ic_node_not_responding))))));
        onView(withId(R.id.test_list)).check(matches(not(hasDescendant(allOf(withId(R.id.image_status), hasBackgroundResource(R.drawable.ic_node_not_responding))))));
    }

}
