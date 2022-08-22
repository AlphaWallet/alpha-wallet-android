package com.alphawallet.app;

import static androidx.test.espresso.Espresso.pressBack;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TransferTest extends BaseE2ETest {
    // On CI server, run tests on different API levels concurrently may cause failure: Replacement transaction underpriced.
    // Use different wallet to transfer token from can avoid this error
    private static final Map<String, String[]> WALLETS = new HashMap<String, String[]>() {{
        put("24", new String[]{"essence allow crisp figure tired task melt honey reduce planet twenty rookie", "0xD0c424B3016E9451109ED97221304DeC639b3F84"});
        put("30", new String[]{"deputy review citizen bacon measure combine bag dose chronic retreat attack fly", "0xD8790c1eA5D15F8149C97F80524AC87f56301204"});
        put("32", new String[]{"omit mobile upgrade warm flock two era hamster local cat wink virus", "0x32f6F38137a79EA8eA237718b0AFAcbB1c58ca2e"});
    }};

    private static final Map<String, String[]> WALLETS_ON_GANACHE = new HashMap<String, String[]>() {{
        put("24", new String[]{"0x644022aef70ad515ee186345fd74b005d759f41be8157c2835de3597d943146d", "0xE494323823fdF1A1Ab6ca79d2538C7182690D52a"});
        put("30", new String[]{"0x5c8843768e0e1916255def80ae7f6197e1f6a2dbcba720038748fc7634e5cffd", "0x162f5e0b63646AAA33a85eA13346F15C5289f901"});
        put("32", new String[]{"omit mobile upgrade warm flock two era hamster local cat wink virus", "0xd7Ba01f596a7cc926b96b3B0a037c47A22904c06"});
    }};

    @Test
    public void should_transfer_from_an_account_to_another() {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS_ON_GANACHE.get(String.valueOf(apiLevel));
        if (array == null) {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        String privateKey = array[0];
        String existedWalletAddress = array[1];

        createNewWallet();
        String newWalletAddress = getWalletAddress();

        importWalletFromSettingsPage(privateKey);
        assertThat(getWalletAddress(), equalTo(existedWalletAddress));

        addNewNetwork("Ganache");
        selectTestNet("Ganache");
        sendBalanceTo(newWalletAddress, "0.001");
        ensureTransactionConfirmed();
        switchToWallet(newWalletAddress);
        assertBalanceIs("0.001");
    }
}
