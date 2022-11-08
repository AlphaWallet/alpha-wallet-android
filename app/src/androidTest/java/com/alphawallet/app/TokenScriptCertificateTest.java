package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static com.alphawallet.app.assertions.Should.shouldSee;
import static com.alphawallet.app.steps.Steps.GANACHE_URL;
import static com.alphawallet.app.steps.Steps.addNewNetwork;
import static com.alphawallet.app.steps.Steps.getWalletAddress;
import static com.alphawallet.app.steps.Steps.importPKWalletFromFrontPage;
import static com.alphawallet.app.steps.Steps.selectTestNet;
import static com.alphawallet.app.steps.Steps.switchToWallet;
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntil;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.util.EthUtils;
import com.alphawallet.app.util.Helper;
import com.alphawallet.app.resources.Contracts;

import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by JB on 1/09/2022.
 */
public class TokenScriptCertificateTest extends BaseE2ETest
{
    private String doorContractAddress;
    private final String contractOwnerPk = "0x69c22d654be7fe75e31fbe26cb56c93ec91144fab67cb71529c8081971635069";
    private final Web3j web3j;

    private final boolean useMumbai = false; //for local testing

    private static final Map<String, String[]> WALLETS_ON_GANACHE = new HashMap<String, String[]>()
    {
        {
            put("24", new String[]{"0x644022aef70ad515ee186345fd74b005d759f41be8157c2835de3597d943146d", "0xE494323823fdF1A1Ab6ca79d2538C7182690D52a"});
            put("30", new String[]{"0x5c8843768e0e1916255def80ae7f6197e1f6a2dbcba720038748fc7634e5cffd", "0x162f5e0b63646AAA33a85eA13346F15C5289f901"});
            put("32", new String[]{"0x992b442eaa34de3c6ba0b61c75b2e4e0241d865443e313c4fa6ab8ba488a6957", "0xd7Ba01f596a7cc926b96b3B0a037c47A22904c06"});
        }
    };

    public TokenScriptCertificateTest()
    {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS_ON_GANACHE.get(String.valueOf(apiLevel));

        if (array == null)
        {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        web3j = EthUtils.buildWeb3j(GANACHE_URL);

        String privateKey = array[0];

        //create credentials for initial transfer
        Credentials credentials = Credentials.create(privateKey);

        //create credentials for contract deployment (fixed so we can link to a tokenscript)
        Credentials deployCredentials = Credentials.create(contractOwnerPk);

        //Transfer 1 eth into deployment wallet
        EthUtils.transferFunds(web3j, credentials, deployCredentials.getAddress(), BigDecimal.ONE);

        //Deploy door contract
        EthUtils.deployContract(web3j, deployCredentials, Contracts.doorContractCode);

        //Always use zero nonce for determining the contract address
        doorContractAddress = EthUtils.calculateContractAddress(deployCredentials.getAddress(), 0L);

        if (useMumbai)
        {
            //If using Mumbai for this test:
            doorContractAddress = "0xA0343dfd68FcD7F18153b8AB87936c5A9C1Da20e";
        }
    }

    @Test
    public void should_view_signature_details()
    {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS_ON_GANACHE.get(String.valueOf(apiLevel));
        if (array == null)
        {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        Credentials deployCredentials = Credentials.create(contractOwnerPk);
        String ownerAddress = Keys.toChecksumAddress(deployCredentials.getAddress());

        importPKWalletFromFrontPage(contractOwnerPk);

        assertThat(getWalletAddress(), equalTo(ownerAddress));

        addNewNetwork("Ganache", GANACHE_URL);
        selectTestNet(useMumbai ? "Mumbai" : "Ganache");

        //Ensure we're on the wallet page
        switchToWallet(ownerAddress);

        Helper.wait(1);

        //add the token manually since test doesn't seem to work normally
        click(withId(R.id.action_my_wallet));
        click(withSubstring("Add / Hide Tokens"));
        Helper.wait(1);
        click(withId(R.id.action_add));
        Helper.wait(1);

        onView(allOf(withId(R.id.edit_text))).perform(replaceText(doorContractAddress));

        onView(isRoot()).perform(waitUntil(withId(R.id.select_token), 300));

        click(withId(R.id.select_token));

        click(withSubstring("Save"));

        pressBack();

        //Swipe up
        onView(withId(R.id.coordinator)).perform(ViewActions.swipeUp());

        //Select token
        click(withSubstring("OFFIC"), 120);

        //Wait for cert to resolve
        //click certificate
        click(withId(R.id.image_lock), 180);

        shouldSee("Smart Token Labs");
        shouldSee("ECDSA");
        shouldSee("Contract Owner");    // Note this may fail once we pull owner() from contract,
                                            // test will need to be changed to contract owner,
                                            // which for this test is: 0xA20efc4B9537d27acfD052003e311f762620642D
    }
}
