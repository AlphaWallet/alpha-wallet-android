package com.alphawallet.app.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import static com.alphawallet.app.repository.SharedPreferenceRepository.HIDE_ZERO_BALANCE_TOKENS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SharedPreferenceRepositoryTest {
    private PreferenceRepositoryType preferenceRepositoryType;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        preferenceRepositoryType = new SharedPreferenceRepository(context);
    }

    @Test
    public void should_not_show_zero_balance_tokens_by_default() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().remove(HIDE_ZERO_BALANCE_TOKENS).apply();

        assertFalse(preferenceRepositoryType.shouldShowZeroBalanceTokens());
    }

    @Test
    public void hide_zero_balance_tokens() {
        preferenceRepositoryType.setShowZeroBalanceTokens(false);
        assertFalse(preferenceRepositoryType.shouldShowZeroBalanceTokens());
    }

    @Test
    public void show_zero_balance_tokens() {
        preferenceRepositoryType.setShowZeroBalanceTokens(true);
        assertTrue(preferenceRepositoryType.shouldShowZeroBalanceTokens());
    }
}