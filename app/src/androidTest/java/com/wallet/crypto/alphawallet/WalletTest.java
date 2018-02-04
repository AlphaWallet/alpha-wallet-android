package com.wallet.crypto.trustapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.model.VMAccount;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class WalletTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void deleteAccountTest() throws Exception {
        Controller controller = Controller.with(InstrumentationRegistry.getTargetContext());
        VMAccount account = controller.createAccount("test password");
        assert(account != null);

        try {
            controller.deleteAccount(account.getAddress());
        } catch (Exception e) {
            assert(false);
        }

        assert(controller.getAccount(account.getAddress()) != null);
    }

    @Test
    public void createAccountTest() throws Exception {
        Controller controller = Controller.with(InstrumentationRegistry.getTargetContext());
        VMAccount account = controller.createAccount("test password");
        assert(account != null);
    }
}
