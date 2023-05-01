package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagKey;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldNotSee;
import static com.alphawallet.app.assertions.Should.shouldNotSeeTag;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.selectMenu;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.toggleSwitch;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntil;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;

import android.view.View;

import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.Helper;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
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

        shouldNotSeeTag(R.drawable.ic_node_strong);

        //onView(withId(R.id.image_status)).check(matches(withTagKey(R.drawable.ic_node_not_responding)));

        //onView(allOf(isDescendantOfA(withId(R.id.layout_list_item)), withTagKey(R.drawable.ic_node_strong))).check(matches(isDisplayed()));

        onView(isRoot()).perform(waitUntil(withTagKey(R.drawable.ic_node_not_responding), 300));


        //onView(allOf(withId(R.id.image_status))).check() .perform(ViewActions.click());

        //onView() withTagValue(equalTo(tagId))

        //now check nodes
        try
        {   //R.drawable.ic_node_not_responding

            onView(withTagKey(R.drawable.ic_node_not_responding)).check(matches(isDisplayed()));
            //if we detect, error
            throw new Exception("Node not responding");
        }
        catch (Exception e)
        {

        }


    }

}
