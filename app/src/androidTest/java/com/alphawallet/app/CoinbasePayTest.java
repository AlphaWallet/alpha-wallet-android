package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.assertBalanceIs;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.steps.Steps.ensureTransactionConfirmed;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.gotoSettingsPage;
import static com.alphawallet.app.steps.Steps.importWalletFromSettingsPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.sendBalanceTo;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static com.alphawallet.app.steps.Steps.toggleSwitch;
import static com.alphawallet.app.util.Helper.click;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import android.os.Build;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CoinbasePayTest extends BaseE2ETest {
    @Test
    public void should_add_custom_network()
    {
        createNewWallet();
        click(withId(R.id.primary_button));
        shouldSee("Buy with Coinbase Pay");
        click(withId(R.id.buy_with_coinbase_pay));
        shouldSee("Buy with Coinbase Pay");
    }
}
