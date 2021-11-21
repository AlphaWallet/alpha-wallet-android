package com.alphawallet.app;

import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.app.util.GetTextAction;
import com.alphawallet.app.util.Helper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.util.Helper.waitUntil;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

@RunWith(AndroidJUnit4.class)
public class TransferTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityScenarioRule
            = new ActivityScenarioRule<>(SplashActivity.class);

    @Before
    public void setUp() {
        ensureAppStarted();
    }

    @Test
    public void should_transfer_from_an_account_to_another() throws InterruptedException {
        String seedPhrase = "essence allow crisp figure tired task melt honey reduce planet twenty rookie";
        String existedWalletAddress = "0xD0c424B3016E9451109ED97221304DeC639b3F84";

        createNewWalletOnFirstStart();
        String newWalletAddress = getWalletAddress();

        importWalletFromSettingsPage(seedPhrase);
        assertThat(getWalletAddress(), equalTo(existedWalletAddress));

        selectTestNet();
        sendBalanceTo(newWalletAddress, 0.001);
        ensureTransactionConfirmed();
        switchToWallet(0);
        assertBalanceIs(0.001);
    }

    private void ensureAppStarted() {
        onView(isRoot()).perform(waitUntil(withText("CREATE A NEW WALLET")));
    }

    private void gotoSettingsPage() {
        onView(withId(R.id.nav_settings)).perform(click());
    }

    private void assertBalanceIs(double balance) {
        onView(withId(R.id.nav_wallet)).perform(click());
        String balanceString = String.valueOf(balance);
        if (balance == 0) {
            balanceString = "0";
        }
        onView(isRoot()).perform(waitUntil(R.id.eth_data, withText(startsWith(balanceString))));
    }

    private void ensureTransactionConfirmed() {
        onView(withText(R.string.rate_no_thanks)).perform(click());
        onView(withId(R.string.action_show_tx_details)).perform(click());
        onView(isRoot()).perform(waitUntil(withSubstring("Sent ETH")));
        pressBack();
    }

    private void createNewWalletOnFirstStart() throws InterruptedException {
        onView(withText("CREATE A NEW WALLET")).perform(click());
        onView(isRoot()).perform(Helper.waitUntil(allOf(withText("CLOSE"), isDisplayed())));
        onView(withText("CLOSE")).perform(click());
    }

    private void selectTestNet() {
        gotoSettingsPage();
        ViewInteraction selectActiveNetworks = onView(withText("Select Active Networks"));
        selectActiveNetworks.perform(scrollTo(), click());
        onView(withId(R.id.mainnet_switch)).perform(click());
        onView(withText(R.string.action_enable_testnet)).perform(click());
        onView(withId(R.id.mainnet_switch)).check(matches(isNotChecked()));
        onView(withId(R.id.testnet_switch)).check(matches(isChecked()));
        onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(0, click()));
        onView(withId(R.id.test_list)).perform(actionOnItemAtPosition(1, click()));
        pressBack();
    }

    private void sendBalanceTo(String receiverAddress, double amount) throws InterruptedException {
        onView(withId(R.id.nav_wallet)).perform(click());
        onView(isRoot()).perform(waitUntil(R.id.eth_data, withText(not(startsWith("0")))));
        onView(withId(R.id.eth_data)).perform(click());
        onView(withText("Send")).perform(click());
        onView(withHint("0")).perform(replaceText(String.valueOf(amount)));
        onView(withHint(R.string.recipient_address)).perform(replaceText(receiverAddress));
        onView(withId(R.string.action_next)).perform(click());
        onView(withId(R.string.action_confirm)).perform(click());
    }


    private void switchToWallet(int index) {
        gotoSettingsPage();
        onView(withText("Change / Add Wallet")).perform(click());
        onView(withId(R.id.list)).perform(actionOnItemAtPosition(index + 1, click())); // need +1 because the list title "YOUR WALLETS" is at position 0
    }

    private String getWalletAddress() {
        gotoSettingsPage();
        onView(withText("Show My Wallet Address")).perform(click());
        GetTextAction getTextAction = new GetTextAction();
        onView(withText(startsWith("0x"))).perform(getTextAction);
        pressBack();
        return getTextAction.getText().toString().replace(" ", ""); // The address show on 2 lines so there is a blank space
    }

    private void importWalletFromSettingsPage(String seedPhrase) {
        gotoSettingsPage();
        onView(withText("Change / Add Wallet")).perform(click());
        onView(withId(R.id.action_add)).perform(click());
        onView(withId(R.id.import_account_action)).perform(click());
        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_seed)))))).perform(replaceText(seedPhrase));
        onView(withId(R.id.import_action)).perform(click());
    }
}