package com.alphawallet.app.widget;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.R;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowRealm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowRealm.class})
public class BuyEthOptionsViewTest
{
    private BuyEthOptionsView buyEthOptionsView;
    private View buyWithCoinbaseButton;

    @Before
    public void setUp()
    {
        buyEthOptionsView = new BuyEthOptionsView(ApplicationProvider.getApplicationContext());
        initView();
    }

    private void initView()
    {
        buyWithCoinbaseButton = buyEthOptionsView.findViewById(R.id.buy_with_coinbase_pay);
    }

    @Test
    public void testBuyWithCoinbasePay()
    {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        buyEthOptionsView.setOnBuyWithCoinbasePayListener(listener);

        buyWithCoinbaseButton.performClick();

        verify(listener).onClick(buyWithCoinbaseButton);
    }
}
