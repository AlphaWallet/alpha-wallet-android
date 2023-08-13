package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.gotoWalletPage;
import static com.alphawallet.app.steps.Steps.input;
import static com.alphawallet.app.steps.Steps.watchWalletWithENS;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.clickMadly;
import static com.alphawallet.app.util.Helper.waitUntil;

import androidx.test.espresso.Espresso;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

public class AWalletNameTest extends BaseE2ETest
{
    @Test
    public void should_show_custom_name_instead_of_address()
    {
        /*createNewWallet();
        String address = getWalletAddress();

        gotoWalletPage();
        shouldSeeFormattedAddress(address);

        renameWalletTo("TestWallet");
        waitUntil(withSubstring("TestWallet"), 10);

        renameWalletTo("");
        shouldSeeFormattedAddress(address);  //TODO: Work out why this hangs
        */
    }

    @Test
    public void should_show_custom_name_instead_of_ENS_name()
    {
        watchWalletWithENS("vitalik.eth");
        // Should see ENS name instead of address
        waitUntil(withSubstring("vitalik.eth"), 10);

        renameWalletTo("Vitalik");
        gotoWalletPage();
        waitUntil(withSubstring("Vitalik"), 10);

        renameWalletTo("");
        gotoWalletPage();
        waitUntil(withSubstring("vitalik.eth"), 10);
        //Espresso.pressBack();
    }

    private void renameWalletTo(String name)
    {
        //clickMadly2(withId(R.id.action_my_wallet));
        gotoSettingsPage();
        click(withText("Name This Wallet"));
        Helper.wait(1);
        onView(withId(R.id.edit_text)).perform(replaceText(name));
        input(R.id.input_name, name);
        clickMadly(withText("Save Name"));
        Helper.wait(1);
        gotoWalletPage();
    }

    private void shouldSeeFormattedAddress(String address)
    {
        String formattedAddr = address.substring(0, 6) + "..." + address.substring(address.length() - 4);
        waitUntil(withSubstring(formattedAddr), 10);
    }
}
