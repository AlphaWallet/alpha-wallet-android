package com.alphawallet.app.viewmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.alphawallet.app.repository.SharedPreferenceRepository;
import com.alphawallet.shadows.ShadowApp;
import com.alphawallet.shadows.ShadowRealm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(shadows = {ShadowApp.class, ShadowRealm.class})
public class HomeViewModelTest
{
    private HomeViewModel homeViewModel;

    @Before
    public void setUp() throws Exception
    {
        SharedPreferenceRepository sharedPreferenceRepository = new SharedPreferenceRepository(RuntimeEnvironment.getApplication());
        homeViewModel = new HomeViewModel(sharedPreferenceRepository, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    public void should_not_be_new_wallet_by_default()
    {
        String address = "0x1";
        assertThat(homeViewModel.checkNewWallet(address), is(false));
    }

    @Test
    public void should_set_is_new_wallet()
    {
        String address = "0x1";
        homeViewModel.setNewWallet(address, true);
        assertThat(homeViewModel.checkNewWallet(address), is(true));
        homeViewModel.setNewWallet(address, false);
        assertThat(homeViewModel.checkNewWallet(address), is(false));
    }
}
