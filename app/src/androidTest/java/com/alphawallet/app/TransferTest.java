package com.alphawallet.app;

import static com.alphawallet.app.steps.Steps.assertBalanceIs;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.ensureTransactionConfirmed;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.importWalletFromSettingsPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.sendBalanceTo;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import android.os.Build;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TransferTest extends BaseE2ETest {
    // On CI server, run tests on different API levels concurrently may cause failure: Replacement transaction underpriced.
    // Use different wallet to transfer token from can avoid this error
    private static final Map<String, String[]> WALLETS = new HashMap<String, String[]>() {{
        put("24", new String[]{"essence allow crisp figure tired task melt honey reduce planet twenty rookie", "0xD0c424B3016E9451109ED97221304DeC639b3F84"});
        put("30", new String[]{"deputy review citizen bacon measure combine bag dose chronic retreat attack fly", "0xD8790c1eA5D15F8149C97F80524AC87f56301204"});
        put("31", new String[]{"omit mobile upgrade warm flock two era hamster local cat wink virus", "0x32f6F38137a79EA8eA237718b0AFAcbB1c58ca2e"});
    }};

    @Test
    @Ignore
    public void should_transfer_from_an_account_to_another() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS.get(String.valueOf(apiLevel));
        if (array == null) {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        String seedPhrase = array[0];
        String existedWalletAddress = array[1];

        createNewWallet();
        String newWalletAddress = getWalletAddress();

        importWalletFromSettingsPage(seedPhrase);
        assertThat(getWalletAddress(), equalTo(existedWalletAddress));

        selectTestNet();
        sendBalanceTo(newWalletAddress, "0.00001");
        ensureTransactionConfirmed();
        switchToWallet(newWalletAddress);
        assertBalanceIs("0.00001");
    }

}
