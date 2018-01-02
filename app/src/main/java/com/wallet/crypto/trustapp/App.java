package com.wallet.crypto.trustapp;

import android.app.Activity;
import android.support.multidex.MultiDexApplication;

import com.github.omadahealth.lollipin.lib.managers.LockManager;
import com.wallet.crypto.trustapp.di.DaggerAppComponent;
import com.wallet.crypto.trustapp.ui.CustomPinActivity;
import com.wallet.crypto.trustapp.ui.SplashActivity;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;

public class App extends MultiDexApplication implements HasActivityInjector {

	@Inject
	DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

	@Override
	public void onCreate() {
		super.onCreate();

		DaggerAppComponent
				.builder()
				.application(this)
				.build()
				.inject(this);

		// enable pin code for the application
		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
		lockManager.enableAppLock(this, CustomPinActivity.class);
	}

	@Override
	public AndroidInjector<Activity> activityInjector() {
		return dispatchingAndroidInjector;
	}

}
