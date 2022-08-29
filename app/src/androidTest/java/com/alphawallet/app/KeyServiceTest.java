package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.importKSWalletFromSettingsPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.util.Helper.click;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyServiceTest extends BaseE2ETest {
    // On CI server, run tests on different API levels concurrently may cause failure: Replacement transaction underpriced.
    // Use different wallet to transfer token from can avoid this error
    private static final Map<String, String[]> WALLETS = new HashMap<String, String[]>() {{
        put("24", new String[]{"essence allow crisp figure tired task melt honey reduce planet twenty rookie", "0xD0c424B3016E9451109ED97221304DeC639b3F84"});
        put("30", new String[]{"deputy review citizen bacon measure combine bag dose chronic retreat attack fly", "0xD8790c1eA5D15F8149C97F80524AC87f56301204"});
        put("32", new String[]{"omit mobile upgrade warm flock two era hamster local cat wink virus", "0x32f6F38137a79EA8eA237718b0AFAcbB1c58ca2e"});
    }};

    private static final String keystore = "{\"address\":\"f9c883c8dca140ebbdc87a225fe6e330be5d25ef\",\"id\":\"5648908b-1862-4f3e-b425-d1ba0790a601\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"cipherparams\":{\"iv\":\"bcbfcffb52f42e9d149b97a8512d4c49\"},\"ciphertext\":\"967d3cd0db82445e4e74a6d5e537c799632e91cf0ca6f9fec17c769812e9454f\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":4096,\"p\":6,\"r\":8,\"salt\":\"084c44b6e76e2b879257520ac00bb59c93e17321ce4a029f9b8294e304defc7a\"},\"mac\":\"0e4a74746d0c3e2739653200bcffb92716e77677d41f84c1184a4eb2054963c6\"}}\n";
    private static final String password = "hellohello";

    @Test
    public void cipher_integrity_test() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS.get(String.valueOf(apiLevel));
        if (array == null) {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        createNewWallet();
        gotoSettingsPage();

        click(withText("Change / Add Wallet"));
        click(withId(R.id.manage_wallet_btn));
        click(withId(R.id.action_key_status));

        //can we click it?
        click(withText("Run Key Diagnostic"));

        //now check the key is decoded correctly
        Helper.wait(1);

        shouldSee("Key Found");
        shouldSee("Unlocked");
        shouldSee("Seed Phrase detected public key");
        shouldSee("HDKEY");
    }

    @Test
    public void cipher_integrity_test_keystore() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS.get(String.valueOf(apiLevel));
        if (array == null)
        {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        createNewWallet();
        gotoSettingsPage();

        importKSWalletFromSettingsPage(keystore, password);

        Helper.wait(5);
        selectTestNet();

        Helper.wait(5);

        gotoSettingsPage();

        Helper.wait(5);

        click(withText("Change / Add Wallet"));

        Helper.wait(5);

        //Click the second hamburger button in the wallets view (should be the 6th item)
        onView(withId(R.id.list)).perform(actionOnItemAtPosition(6, ViewActions.pressKey(R.id.manage_wallet_btn)));
        //click(withId(R.id.manage_wallet_btn));
        click(withId(R.id.action_key_status));

        //can we click it?
        click(withText("Run Key Diagnostic"));

        //now check the key is decoded correctly
        Helper.wait(1);

        shouldSee("Key Found");
        shouldSee("Unlocked");
        shouldSee("Decoded Keystore public key");
        shouldSee("KEYSTORE");
    }
}
