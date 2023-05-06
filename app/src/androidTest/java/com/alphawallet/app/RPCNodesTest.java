package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.util.Helper.click;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
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

        onView(withId(R.id.main_list)).check(matches(not(hasDescendant(allOf(withId(R.id.image_status), withTagValue(is(404)))))));

        onView(withId(R.id.test_list)).check(matches(not(hasDescendant(allOf(withId(R.id.image_status), withTagValue(is(404)))))));


        //onView(withId(R.id.image_status)).check(matches(withTagKey(R.drawable.ic_node_not_responding)));

        //onView(allOf(isDescendantOfA(withId(R.id.layout_list_item)), withTagKey(R.drawable.ic_node_strong))).check(matches(isDisplayed()));

//        onView(isRoot()).perform(waitUntil(withTagKey(R.drawable.ic_node_not_responding), 300));


        //onView(allOf(withId(R.id.image_status))).check() .perform(ViewActions.click());

        //onView() withTagValue(equalTo(tagId))

        //now check nodes
//        try
//        {   //R.drawable.ic_node_not_responding
//
//            onView(withTagKey(R.drawable.ic_node_not_responding)).check(matches(isDisplayed()));
//            //if we detect, error
//            throw new Exception("Node not responding");
//        }
//        catch (Exception e)
//        {
//
//        }


    }

}
