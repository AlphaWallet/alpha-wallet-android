package com.alphawallet.app;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.util.ReleaseTree;

import dagger.hilt.android.HiltAndroidApp;
import io.realm.Realm;
import timber.log.Timber;

@HiltAndroidApp
public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
        Realm.init(this);

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		} else {
			Timber.plant(new ReleaseTree());
		}

		int defaultTheme = PreferenceManager.getDefaultSharedPreferences(this).getInt("theme", C.THEME_AUTO);

		if (defaultTheme == C.THEME_LIGHT)
		{
			AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
		}
		else if (defaultTheme == C.THEME_DARK)
		{
			AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
		}
		else
		{
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		}

		// enable pin code for the application
//		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
//		lockManager.enableAppLock(this, CustomPinActivity.class);
//		lockManager.getAppLock().setShouldShowForgot(false);
	}
}
