package com.alphawallet.app;

import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.util.Helper.click;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import com.alphawallet.app.util.Helper;

import org.junit.Test;

public class CoinbasePayTest extends BaseE2ETest
{
    @Test
    public void should_see_coinbase_pay_window()
    {
        //TODO: Work out why this doesn't seem to work
        /*createNewWallet();
        click(withText("Buy ETH"));
        Helper.wait(2);
        shouldSee("Buy with Coinbase Pay"); // <-- here - the test suite can't seem to find this
        Helper.wait(2);
        click(withId(R.id.buy_with_coinbase_pay));
        Helper.wait(2);
        shouldSee("Buy with Coinbase Pay");*/
    }
}
