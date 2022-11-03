package com.alphawallet.app;

import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.createNewWallet;
import static com.alphawallet.app.util.Helper.click;

import org.junit.Test;

public class SwapTest extends BaseE2ETest
{
    @Test
    public void should_see_swap_window()
    {
        createNewWallet();
        click(withText("0 ETH"));
        click(withId(R.id.more_button));
        click(withText("Swap"));
        shouldSee("Select Exchanges");
        click(withText("DODO"));
        pressBack();
        shouldSee("Swap");
    }
}
