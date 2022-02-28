package com.alphawallet.app.viewmodel;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.ui.HomeActivity;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SelectThemeViewModel extends BaseViewModel {
    private final HomeRouter homeRouter;
    private final PreferenceRepositoryType preferenceRepository;
    @Inject
    public SelectThemeViewModel(HomeRouter homeRouter,
                                PreferenceRepositoryType preferenceRepository)
    {
        this.homeRouter = homeRouter;
        this.preferenceRepository = preferenceRepository;
    }

    public int getTheme()
    {
        return preferenceRepository.getTheme();
    }

    public void setTheme(int theme)
    {
        preferenceRepository.setTheme(theme);
        if (theme == C.THEME_LIGHT)
        {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
        }
        else if (theme == C.THEME_DARK)
        {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
        }
        else
        {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
