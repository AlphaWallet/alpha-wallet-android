package com.alphawallet.app;

import static com.alphawallet.app.assertions.Should.shouldSeeTitle;
import static com.alphawallet.app.steps.Steps.createNewWallet;

import org.junit.Test;

public class SelectNetworkTest extends BaseE2ETest {
    @Test
    public void should_select_network_when_first_create_wallet() {
        createNewWallet();
//        shouldSee(R.id.mainnet_header);
        shouldSeeTitle("Select Active Networks");
    }
}
