package com.wallet.crypto.trustapp.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.omadahealth.lollipin.lib.managers.AppLock;
import com.github.omadahealth.lollipin.lib.managers.LockManager;
import com.wallet.crypto.trustapp.views.CustomPinActivity;

/**
 * Created by Philipp Rieger on 28.12.17.
 */

/**
 * Singleton class handling pincode functionality
 */
public class PincodeUtil {

    private static PincodeUtil instance;

    /**
     * private constructor (singleton)
     */
    private PincodeUtil() { }

    /**
     * get instance of the util class (singleton)
     *
     * @return
     */
    public static PincodeUtil getInstance() {
        if (instance == null) {
            instance = new PincodeUtil();
        }
        return instance;
    }

    /**
     * ask the user for the pin code
     *
     * @param context
     */
    public void askForPin(final Context context) {
        if (isPincodeEnabled(context)) {
            final Intent intent = new Intent(context, CustomPinActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.UNLOCK_PIN);
            context.startActivity(intent);
        }
    }

    /**
     * checks if the pincode is set by searching for flag in shared preferences
     *
     * @param context
     * @return
     */
    public boolean isPincodeEnabled(final Context context) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref.contains("isPincodeSet")) {
            return true;
        }
        return false;
    }

    /**
     * enables pincode by requesting the user to set one up and inserts
     * shared preference flag.
     *
     * @param context
     */
    public void enablePincode(final Context context) {
        if (!isPincodeEnabled(context)) {
            final LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
            lockManager.enableAppLock(context, CustomPinActivity.class);

            final Intent intent = new Intent(context, CustomPinActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.ENABLE_PINLOCK);
            context.startActivity(intent);

            // set the shared pref flag
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor ed = pref.edit();
            ed.putBoolean("isPincodeSet", true);
            ed.commit();
        }
    }

    /**
     * disables the pincode and thereby removes the shared preference flag.
     * @param context
     */
    public void disablePincode(final Context context) {
        if (isPincodeEnabled(context)) {
            final Intent intent = new Intent(context, CustomPinActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(AppLock.EXTRA_TYPE, AppLock.DISABLE_PINLOCK);
            context.startActivity(intent);

            final LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
            lockManager.disableAppLock();

            // remove the shared pref flag
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor ed = pref.edit();
            ed.remove("isPincodeSet");
            ed.commit();
        }
    }

}
