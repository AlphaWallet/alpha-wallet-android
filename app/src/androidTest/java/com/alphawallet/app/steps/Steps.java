package com.alphawallet.app.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntil;
import static com.alphawallet.app.util.RootUtil.isDeviceRooted;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.R;
import com.alphawallet.app.util.GetTextAction;
import com.alphawallet.app.util.Helper;

public class Steps
{
    public static void createNewWallet()
    {
        if (isDeviceRooted()) {
            click(withText(R.string.ok));
        }
        click(withId(R.id.button_create));
        Helper.wait(10);
        click(withText(R.string.action_close)); // works well locally but NOT work with GitHub actions
    }

    public static void visit(String urlString)
    {
        navigateToBrowser();
        onView(withId(R.id.url_tv)).perform(replaceText(urlString), pressImeActionButton());
    }

    public static void navigateToBrowser()
    {
        click(withId(R.id.nav_browser_text));
    }

    public static void selectTestNet()
    {
        gotoSettingsPage();
        ViewInteraction selectActiveNetworks = onView(withText("Select Active Networks"));
        selectActiveNetworks.perform(scrollTo(), ViewActions.click());
        toggleSwitch(R.id.mainnet_header);
        click(withText(R.string.action_enable_testnet));
        onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(1, ViewActions.click())); // Rinkeby
        onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(3, ViewActions.click())); // Kovan
        pressBack();
    }

    public static void assertBalanceIs(String balanceStr) {
        onView(isRoot()).perform(waitUntil(withSubstring(balanceStr), 10 * 60));
    }

    public static void ensureTransactionConfirmed() {
//        onView(withText(R.string.rate_no_thanks)).perform(click());
        click(withId(R.string.action_show_tx_details));
        onView(isRoot()).perform(waitUntil(withSubstring("Sent ETH"), 30 * 60));
        pressBack();
    }

    public static void sendBalanceTo(String receiverAddress, String amountStr) {
        click(withId(R.id.nav_wallet_text));
        onView(isRoot()).perform(waitUntil(R.id.eth_data, withText(not(startsWith("0")))));
        click(withId(R.id.eth_data));
        click(withText("Send"));
        onView(withHint("0")).perform(replaceText(amountStr));
        onView(withHint(R.string.recipient_address)).perform(replaceText(receiverAddress));
        click(withId(R.string.action_next));
        Helper.wait(5);
        click(withId(R.string.action_confirm));
    }

    public static void switchToWallet(String address) {
        gotoSettingsPage();
        click(withText("Change / Add Wallet"));
        onView(withSubstring(address.substring(0, 6))).perform(ViewActions.click());
    }

    public static String getWalletAddress() {
        gotoSettingsPage();
        click(withText("Show My Wallet Address"));
        GetTextAction getTextAction = new GetTextAction();
        onView(withText(startsWith("0x"))).perform(getTextAction);
        pressBack();
        return getTextAction.getText().toString().replace(" ", ""); // The address show on 2 lines so there is a blank space
    }

    public static void importWalletFromSettingsPage(String seedPhrase) {
        gotoSettingsPage();
        click(withText("Change / Add Wallet"));
        Helper.wait(10);
        click(withId(R.id.action_add));
//        SnapshotUtil.take("after-add");
        click(withId(R.id.import_account_action));
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_seed)))))).perform(replaceText(seedPhrase));
        Helper.wait(2); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withId(R.id.import_action));
        Helper.wait(10);
    }

    public static void gotoSettingsPage() {
        click(withId(R.id.nav_settings_text));
    }

    private static void toggleSwitch(int id) {
        onView(allOf(withId(R.id.switch_material), isDescendantOfA(withId(id)))).perform(ViewActions.click());
    }
}
