package com.alphawallet.app;

import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.GANACHE_URL;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.assertBalanceIs;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.ensureTransactionConfirmed;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.gotoWalletPage;
import static com.alphawallet.app.steps.Steps.importWalletFromSettingsPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.sendBalanceTo;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static org.junit.Assert.fail;

import android.os.Build;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class WalletNameTest extends BaseE2ETest
{
    @Test
    public void should_show_formatted_address_by_default() {
        createNewWallet();
        String address = getWalletAddress();
        gotoWalletPage();
        shouldSee(address.substring(0, 6) + "..." + address.substring(address.length() - 4)); // 0xabcd...wxyz
    }

    @Test
    public void should_show_custom_name_instead_of_address()
    {
    }

    @Test
    public void should_show_ENS_name_instead_of_address()
    {
    }

    @Test
    public void should_show_custom_name_instead_of_ENS_name()
    {
    }
}
