package com.alphawallet.app.viewmodel;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.app.UiModeManager;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.alphawallet.app.C;
import com.alphawallet.app.repository.PreferenceRepositoryType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SelectThemeViewModel extends BaseViewModel
{
    private final PreferenceRepositoryType preferenceRepository;

    @Inject
    public SelectThemeViewModel(PreferenceRepositoryType preferenceRepository)
    {
        this.preferenceRepository = preferenceRepository;
    }

    public int getTheme()
    {
        return preferenceRepository.getTheme();
    }

    public void setTheme(Context context, int theme)
    {
        preferenceRepository.setTheme(theme);
        updateTheme(context, theme);
    }

    private void updateTheme(Context context, int theme)
    {
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
            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            int mode = uiModeManager.getNightMode();
            if (mode == UiModeManager.MODE_NIGHT_YES)
            {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
            }
            else if (mode == UiModeManager.MODE_NIGHT_NO)
            {
                AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
            }
        }
    }
}
