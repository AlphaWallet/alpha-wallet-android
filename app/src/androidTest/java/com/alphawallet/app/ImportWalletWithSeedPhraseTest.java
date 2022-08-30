package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.steps.Steps.closeSecurityWarning;
import static com.alphawallet.app.steps.Steps.closeSelectNetworkPage;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.util.Helper.click;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

import android.os.Build;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ImportWalletWithSeedPhraseTest extends BaseE2ETest {
    private static final Map<String, String[]> WALLETS = new HashMap<String, String[]>() {{
        put("24", new String[]{"essence allow crisp figure tired task melt honey reduce planet twenty rookie", "0xD0c424B3016E9451109ED97221304DeC639b3F84"});
        put("30", new String[]{"deputy review citizen bacon measure combine bag dose chronic retreat attack fly", "0xD8790c1eA5D15F8149C97F80524AC87f56301204"});
        put("32", new String[]{"omit mobile upgrade warm flock two era hamster local cat wink virus", "0x32f6F38137a79EA8eA237718b0AFAcbB1c58ca2e"});
    }};

    @Test
    public void should_import_wallet_with_seed_phrase() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS.get(String.valueOf(apiLevel));
        if (array == null) {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        String seedPhrase = array[0];
        String existedWalletAddress = array[1];

        click(withText("I already have a Wallet"));

        onView(allOf(withId(R.id.edit_text), withParent(withParent(withParent(withId(R.id.input_seed)))))).perform(replaceText(seedPhrase));
        Helper.wait(2); // Avoid error: Error performing a ViewAction! soft keyboard dismissal animation may have been in the way. Retrying once after: 1000 millis
        click(withId(R.id.import_action));
        assertThat(getWalletAddress(), equalTo(existedWalletAddress));
    }
}
