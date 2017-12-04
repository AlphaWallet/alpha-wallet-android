package com.wallet.crypto.trustapp;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.wallet.pwd.trustapp.PasswordManager;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by marat on 11/14/17.
 */

public class PasswordManagerTest {
    @Test
    public void setGetPassword() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, InvalidKeySpecException {
        Context context = InstrumentationRegistry.getTargetContext();

        PasswordManager.setPassword("myaddress", "mypassword", context);
        assertThat(PasswordManager.getPassword("myaddress", context), is("mypassword"));
    }

    @Test
    public void setGetPasswordLegacy() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, InvalidKeySpecException {
        Context context = InstrumentationRegistry.getTargetContext();

        PasswordManager.setPasswordLegacy("myaddress", "mypassword", context);
        assertThat(PasswordManager.getPassword("myaddress", context), is("mypassword"));
    }
}
