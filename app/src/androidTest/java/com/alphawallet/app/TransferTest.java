package com.alphawallet.app;

import static com.alphawallet.app.steps.Steps.GANACHE_URL;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.assertBalanceIs;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.ensureTransactionConfirmed;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.importWalletFromSettingsPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.sendBalanceTo;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static org.junit.Assert.fail;

import android.os.Build;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TransferTest extends BaseE2ETest
{
    // On CI server, run tests on different API levels concurrently may cause failure: Replacement transaction underpriced.
    // Use different wallet to transfer token from can avoid this error
    private static final Map<String, String[]> WALLETS_ON_GANACHE = new HashMap<String, String[]>()
    {
        {
            put("24", new String[]{"0x644022aef70ad515ee186345fd74b005d759f41be8157c2835de3597d943146d", "0xE494323823fdF1A1Ab6ca79d2538C7182690D52a"});
            put("30", new String[]{"0x5c8843768e0e1916255def80ae7f6197e1f6a2dbcba720038748fc7634e5cffd", "0x162f5e0b63646AAA33a85eA13346F15C5289f901"});
            put("32", new String[]{"0x992b442eaa34de3c6ba0b61c75b2e4e0241d865443e313c4fa6ab8ba488a6957", "0xd7Ba01f596a7cc926b96b3B0a037c47A22904c06"});
        }
    };

    @Test
    public void should_transfer_from_an_account_to_another()
    {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS_ON_GANACHE.get(String.valueOf(apiLevel));
        if (array == null)
        {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        String privateKey = array[0];

        createNewWallet();
        String newWalletAddress = getWalletAddress();

        importWalletFromSettingsPage(privateKey);
        addNewNetwork("Ganache", GANACHE_URL);
        selectTestNet("Ganache");
        sendBalanceTo("ETH", "0.001", newWalletAddress);
        ensureTransactionConfirmed();
        switchToWallet(newWalletAddress);
        assertBalanceIs("0.001");
    }
}
