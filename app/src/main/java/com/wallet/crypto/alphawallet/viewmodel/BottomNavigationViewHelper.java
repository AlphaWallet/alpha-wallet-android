package com.wallet.crypto.alphawallet.viewmodel;

import android.annotation.SuppressLint;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Created by James on 22/02/2018.
 */

public class BottomNavigationViewHelper
{
    @SuppressLint("RestrictedApi")
    public static void removeShiftMode(BottomNavigationView view)
    {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) view.getChildAt(0);
        try
        {
            Field shiftingMode = menuView.getClass().getDeclaredField("mShiftingMode");
            shiftingMode.setAccessible(true);
            shiftingMode.setBoolean(menuView, false);
            shiftingMode.setAccessible(false);
            for (int i = 0; i < menuView.getChildCount(); i++)
            {
                BottomNavigationItemView item = (BottomNavigationItemView) menuView.getChildAt(i);
                item.setShiftingMode(false);
                // set once again checked value, so view will be updated
                item.setChecked(item.getItemData().isChecked());
            }

        }
        catch (NoSuchFieldException e)
        {
            Log.e("ERROR NO SUCH FIELD", "Unable to get shift mode field");
        }
        catch (IllegalAccessException e)
        {
            Log.e("ERROR ILLEGAL ALG", "Unable to change value of shift mode");
        }
    }
}