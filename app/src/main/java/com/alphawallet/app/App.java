package com.alphawallet.app;

import android.app.Activity;
import android.app.Application;
import android.app.Service;

import androidx.fragment.app.Fragment;
import com.alphawallet.app.di.DaggerAppComponent;
import javax.inject.Inject;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.realm.Realm;

public class App extends Application implements HasActivityInjector, HasSupportFragmentInjector, HasServiceInjector {

	@Inject
	DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

	@Inject
	DispatchingAndroidInjector<Fragment> dispatchingAndroidSupportInjector;

	@Inject
	DispatchingAndroidInjector<Service> dispatchingAndroidServiceInjector;

	@Override
	public void onCreate() {
		super.onCreate();
        Realm.init(this);
        DaggerAppComponent
				.builder()
				.application(this)
				.build()
				.inject(this);

		// enable pin code for the application
//		LockManager<CustomPinActivity> lockManager = LockManager.getInstance();
//		lockManager.enableAppLock(this, CustomPinActivity.class);
//		lockManager.getAppLock().setShouldShowForgot(false);
	}

	@Override
	public AndroidInjector<Activity> activityInjector() {
		return dispatchingAndroidInjector;
	}

	@Override
	public AndroidInjector<Fragment> supportFragmentInjector() {
		return dispatchingAndroidSupportInjector;
	}

	@Override
	public AndroidInjector<Service> serviceInjector() {
		return dispatchingAndroidServiceInjector;
	}
}
