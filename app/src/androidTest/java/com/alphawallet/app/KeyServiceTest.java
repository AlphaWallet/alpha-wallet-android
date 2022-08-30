package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.closeSecurityWarning;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.importKSWalletFromFrontPage;
import static com.alphawallet.app.util.Helper.click;
import static org.junit.Assert.fail;

import android.os.Build;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyServiceTest extends BaseE2ETest {
    private static final String keystore = "{\"address\":\"f9c883c8dca140ebbdc87a225fe6e330be5d25ef\",\"id\":\"5648908b-1862-4f3e-b425-d1ba0790a601\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"cipherparams\":{\"iv\":\"bcbfcffb52f42e9d149b97a8512d4c49\"},\"ciphertext\":\"967d3cd0db82445e4e74a6d5e537c799632e91cf0ca6f9fec17c769812e9454f\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"084c44b6e76e2b879257520ac00bb59c93e17321ce4a029f9b8294e304defc7a\"},\"mac\":\"0e4a74746d0c3e2739653200bcffb92716e77677d41f84c1184a4eb2054963c6\"}}\n";
    private static final String password = "hellohello";

    @Test
    public void cipher_integrity_test() {
        createNewWallet();
        gotoSettingsPage();

        click(withText("Change / Add Wallet"));
        click(withId(R.id.manage_wallet_btn));
        click(withId(R.id.action_key_status));

        click(withText("Run Key Diagnostic"));

        Helper.wait(1);

        //now check the key is decoded correctly
        shouldSee("Key found");
        shouldSee("Unlocked");
        shouldSee("Seed Phrase detected public key");
        shouldSee("HDKEY");
    }

    @Test
    public void cipher_integrity_test_keystore() {
        importKSWalletFromFrontPage(keystore, password);

        gotoSettingsPage();
        click(withText("Change / Add Wallet"));
        Helper.wait(1);
        click(withId(R.id.manage_wallet_btn));
        Helper.wait(1);

        click(withId(R.id.action_key_status));

        Helper.wait(1);

        click(withText("Run Key Diagnostic"));

        Helper.wait(1);

        //now check the key is decoded correctly
        shouldSee("Key found");
        shouldSee("Unlocked");
        shouldSee("Decoded Keystore public key");
        shouldSee("KEYSTORE");
    }
}
