package com.alphawallet.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static com.alphawallet.app.steps.Steps.GANACHE_URL;
import static com.alphawallet.app.steps.Steps.addCustomToken;
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
import static com.alphawallet.app.util.Helper.click;
import static com.alphawallet.app.util.Helper.waitUntil;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.test.espresso.action.ViewActions;

import com.alphawallet.app.resources.Contracts;
import com.alphawallet.app.steps.Steps;
import com.alphawallet.app.util.EthUtils;
import com.alphawallet.app.util.Helper;

import org.junit.Before;
import org.junit.Test;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TransferERC20Test extends BaseE2ETest
{
    private String contractAddress;
    private final String contractOwnerPk = "0x69c22d654be7fe75e31fbe26cb56c93ec91144fab67cb71529c8081971635069";
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
    private Web3j web3j;

    @Override
    @Before
    public void setUp()
    {
        super.setUp();
        deployUSDCOnGanache();
    }

    private void deployUSDCOnGanache()
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
        EthUtils.deployContract(web3j, deployCredentials, Contracts.usdcContractCode);

        //Always use zero nonce for determining the contract address
        contractAddress = EthUtils.calculateContractAddress(deployCredentials.getAddress(), 0L);

        assertNotNull(contractAddress);
    }

    @Test
    public void should_transfer_from_an_account_to_another()
    {
        int apiLevel = Build.VERSION.SDK_INT;
        String[] array = WALLETS_ON_GANACHE.get(String.valueOf(apiLevel));
        if (array == null)
        {
            fail("Please config seed phrase and wallet address for this API level first.");
        }

        transferToSenderAccount(array[1]);
        String privateKey = array[0];

        createNewWallet();
        String newWalletAddress = getWalletAddress();

        importWalletFromSettingsPage(privateKey);
        addNewNetwork("Ganache", GANACHE_URL);
        selectTestNet("Ganache");
        gotoWalletPage();
        addCustomToken(contractAddress);
        sendBalanceTo("USDC", "0.001", newWalletAddress);
        ensureTransactionConfirmed();
        switchToWallet(newWalletAddress);
        assertBalanceIs("0.001");
    }

    private void transferToSenderAccount(String receiverAddress)
    {

    }
}
