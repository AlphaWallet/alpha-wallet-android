package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.importKSWalletFromFrontPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static com.alphawallet.app.util.Helper.click;
import static org.hamcrest.Matchers.allOf;

import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

/**
 * Created by JB on 1/09/2022.
 */
public class TokenScriptCertificateTest extends BaseE2ETest {
    private static final String keystore = "{\"address\":\"f9c883c8dca140ebbdc87a225fe6e330be5d25ef\",\"id\":\"5648908b-1862-4f3e-b425-d1ba0790a601\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"cipherparams\":{\"iv\":\"bcbfcffb52f42e9d149b97a8512d4c49\"},\"ciphertext\":\"967d3cd0db82445e4e74a6d5e537c799632e91cf0ca6f9fec17c769812e9454f\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"084c44b6e76e2b879257520ac00bb59c93e17321ce4a029f9b8294e304defc7a\"},\"mac\":\"0e4a74746d0c3e2739653200bcffb92716e77677d41f84c1184a4eb2054963c6\"}}\n";
    private static final String password = "hellohello";

    @Test
    public void certificate_test() {
        importKSWalletFromFrontPage(keystore, password);

        selectTestNet("Kovan");

        //Ensure we're on the wallet page
        switchToWallet("0xF9c883c8DcA140EBbdC87a225Fe6E330BE5D25ef");

        //Wait for TokensService engine to resolve the STL token
        Helper.wait(1);

        //add the token manually since test doesn't seem to work normally
        click(withId(R.id.action_my_wallet));
        click(withSubstring("Add / Hide Tokens"));
        Helper.wait(1);
        click(withId(R.id.action_add));
        Helper.wait(1);

        onView(allOf(withId(R.id.edit_text))).perform(replaceText("0x9afb1e2822edab926d0dea894f8864368b7bb47a"));

        Helper.wait(15);

        click(withId(R.id.select_token));

        click(withSubstring("Save"));

        pressBack();

        //Swipe up
        onView(withId(R.id.coordinator)).perform(ViewActions.swipeUp());

        //Select token
        click(withSubstring("OFFIC"));

        //Wait for cert to resolve
        Helper.wait(8);

        //click certificate
        click(withId(R.id.image_lock));

        shouldSee("Smart Token Labs");
        shouldSee("ECDSA");
        shouldSee("Contract Owner"); // Note this may fail once we pull owner() from contract, test will need to be changed to contract owner, which for this test is: 0xA20efc4B9537d27acfD052003e311f762620642D
    }
}
