package com.alphawallet.app;

import android.app.Application;

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

		// enable pin code for the application
//		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
//		lockManager.enableAppLock(this, CustomPinActivity.class);
//		lockManager.getAppLock().setShouldShowForgot(false);
	}
}
