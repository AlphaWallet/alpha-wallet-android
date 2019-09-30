package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

public class CreateAccountViewModelFactory implements ViewModelProvider.Factory {

	public CreateAccountViewModelFactory() { }

	@NonNull
	@Override
	public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
		return (T) new CreateAccountViewModel();
	}
}
