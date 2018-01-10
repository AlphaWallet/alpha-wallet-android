package com.wallet.crypto.trustapp.ui;

import com.github.omadahealth.lollipin.lib.managers.AppLockActivity;

/**
 * @author Philipp Rieger
 *
 * @implNote Adjust this class to modify the pin code behavior
 */
public class CustomPinActivity extends AppLockActivity {

    @Override
    public void showForgotDialog() {

    }

    @Override
    public void onPinFailure(int attempts) {

    }

    @Override
    public void onPinSuccess(int attempts) {

    }

    @Override
    public int getPinLength() {
        return super.getPinLength();
    }
}
