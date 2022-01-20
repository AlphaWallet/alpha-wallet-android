package com.alphawallet.app;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;
import io.realm.Realm;

@HiltAndroidApp
public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
        Realm.init(this);

		// enable pin code for the application
//		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
//		lockManager.enableAppLock(this, CustomPinActivity.class);
//		lockManager.getAppLock().setShouldShowForgot(false);
	}
}
