package com.alphawallet.app.steps;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldNotSee;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.util.EthUtils.GANACHE_URL;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntil;
import static com.alphawallet.app.util.RootUtil.isDeviceRooted;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.R;
import com.alphawallet.app.assertions.Should;
import com.alphawallet.app.util.GetTextAction;
import com.alphawallet.app.util.Helper;

/**
 * Every step consists of several operations, step name stands for user perspective actions.
 * You can add steps as you wish to reuse code between test cases.
 */
public class Steps
{
    public static void createNewWallet()
    {
        click(withId(R.id.button_create));
        closeSelectNetworkPage();
        click(withText(R.string.action_close));
    }

    public static void closeSecurityWarning()
    {
        if (isDeviceRooted()) {
            click(withText(R.string.ok));
        }
    }

    public static void closeSelectNetworkPage()
    {
        Helper.wait(5);
        pressBack();
    }

    public static void visit(String urlString)
    {
        Helper.wait(1);
        navigateToBrowser();
        onView(withId(R.id.url_tv)).perform(replaceText(urlString), pressImeActionButton());
    }

    public static void navigateToBrowser()
    {
        click(withId(R.id.nav_browser_text));
    }

    public static void selectTestNet(String name)
    {
        gotoSettingsPage();
        selectMenu("Select Active Networks");
        toggleSwitch(R.id.mainnet_header);
        click(withText(R.string.action_enable_testnet));
        click(withSubstring("Rinkeby")); // Deselect
        click(withSubstring(name)); // Select
        pressBack();
    }

    public static void selectMenu(String text)
    {
        ViewInteraction selectActiveNetworks = onView(withText(text));
        selectActiveNetworks.perform(scrollTo(), ViewActions.click());
    }

    public static void assertBalanceIs(String balanceStr) {
        Should.shouldSee(balanceStr);
    }

    public static void ensureTransactionConfirmed() {
//        onView(withText(R.string.rate_no_thanks)).perform(click());
        click(withId(R.string.action_show_tx_details));
        onView(isRoot()).perform(waitUntil(withSubstring("Sent"), 30 * 60));
        pressBack();
    }

    public static void sendBalanceTo(String receiverAddress, String amountStr) {
        click(withId(R.id.nav_wallet_text));
        ensureBalanceFetched();
        click(withSubstring("ETH"));
        click(withText("Send"));
        onView(withHint("0")).perform(replaceText(amountStr));
        onView(withHint(R.string.recipient_address)).perform(replaceText(receiverAddress));
        click(withId(R.string.action_next));
        Helper.wait(5);
        click(withId(R.string.action_confirm));
    }

    private static void ensureBalanceFetched()
    {
        shouldSee("Ganache");
        shouldNotSee("0 ETH");
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

    public static void importWalletFromSettingsPage(String text) {
        gotoSettingsPage();
        click(withText("Change / Add Wallet"));
        click(withId(R.id.action_add));
//        SnapshotUtil.take("after-add");
        click(withId(R.id.import_account_action));
        int textId;
        int buttonId;
        boolean isPrivateKey = text.startsWith("0x");
        if (isPrivateKey) {
            click(withText("Private key"));
            textId = R.id.input_private_key;
            buttonId = R.id.import_action_pk;
        } else {
            textId = R.id.input_seed;
            buttonId = R.id.import_action;
        }
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(textId)))))).perform(replaceText(text));
        Helper.wait(2); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withId(buttonId));
        closeSelectNetworkPage();
    }

    public static void importPKWalletFromFrontPage(String privateKey) {
        click(withText("I already have a Wallet"));
        click(withText("Private key"));
        Helper.wait(1);
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_private_key)))))).perform(replaceText(privateKey));
        Helper.wait(1); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withId(R.id.import_action_pk));
        Helper.wait(5);
    }

    public static void importKSWalletFromFrontPage(String keystore, String password) {
        click(withText("I already have a Wallet"));
        click(withText("Keystore"));
        Helper.wait(1);
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_keystore)))))).perform(replaceText(keystore));
        Helper.wait(1); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withText("Continue"));
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_password)))))).perform(replaceText(password));
        click(withText("Continue"));
        Helper.wait(5);
    }

    public static void importKSWalletFromSettingsPage(String keystore, String password) {
        gotoSettingsPage();
        click(withText("Change / Add Wallet"));
        click(withId(R.id.action_add));
        click(withId(R.id.import_account_action));
        click(withText("Keystore"));
        Helper.wait(1);
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_keystore)))))).perform(replaceText(keystore));
        Helper.wait(1); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withText("Continue"));
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_password)))))).perform(replaceText(password));
        click(withText("Continue"));
        Helper.wait(5);
        shouldSee("Select Active Networks");
        pressBack();
    }

    public static void gotoWalletPage()
    {
        click(withId(R.id.nav_wallet_text));
    }

    public static void gotoSettingsPage() {
        click(withId(R.id.nav_settings_text));
    }

    public static void toggleSwitch(int id) {
        onView(allOf(withId(R.id.switch_material), isDescendantOfA(withId(id)))).perform(ViewActions.click());
    }

    public static void addNewNetwork(String name)
    {
        selectMenu("Select Active Networks");
        click(withId(R.id.action_add));
        input(R.id.input_network_name, name);
        input(R.id.input_network_rpc_url, GANACHE_URL);
        input(R.id.input_network_chain_id, "2");
        input(R.id.input_network_symbol, "ETH");
        input(R.id.input_network_explorer_api, GANACHE_URL);
        input(R.id.input_network_block_explorer_url, GANACHE_URL);
        onView(withId(R.id.network_input_scroll)).perform(swipeUp());
        Helper.wait(1);
        click(withId(R.id.checkbox_testnet));
        click(withId(R.string.action_add_network));
        pressBack();
    }

    private static void input(int id, String text)
    {
        onView(allOf(withId(R.id.edit_text), isDescendantOfA(withId(id)))).perform(replaceText(text));
    }

    public static void watchWalletWithENS(String ens)
    {
        click(withText("I already have a Wallet"));
        click(withText("Private key")); // Scroll to right
        Helper.wait(1);
        click(withText("Watch-only Wallets"));
        Helper.wait(1);
        input(R.id.input_watch_address, ens);
        Helper.wait(5);
        click(withText("Watch Wallet"));
    }

    public static void selectCurrency(String currency)
    {
        gotoSettingsPage();
        selectMenu("Change Currency");
        Helper.wait(1);
        try
        {
            click(withText(currency));
        }
        catch (Exception e)
        {
            onView(withId(R.id.list)).perform(ViewActions.swipeUp());
            click(withText(currency));
        }
        pressBack();
    }
}
