package com.wallet.crypto.trustapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    EthereumNetworkRepositoryType ethereumNetworkRepository;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fragment_settings);
        final Preference donate = findPreference("pref_donate");

        donate.setOnPreferenceClickListener(preference -> {
//                mController.navigateToSend(getActivity(), "0x9f8284ce2cf0c8ce10685f537b1fff418104a317");
            return false;
        });
        final ListPreference listPreference = (ListPreference) findPreference("pref_rpcServer");
        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        setRpcServerPreferenceData(listPreference);
        listPreference.setOnPreferenceClickListener(preference -> {
            setRpcServerPreferenceData(listPreference);
            return false;
        });
        String versionString = Controller.with(getActivity()).getVersion();
        Preference version = findPreference("pref_version");
        version.setSummary(versionString);
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        preferences
                .registerOnSharedPreferenceChangeListener(SettingsFragment.this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals("pref_rpcServer")) {
            Preference rpcServerPref = findPreference(key);
            // Set summary
            String selectedRpcServer = sharedPreferences.getString(key, "");
            rpcServerPref.setSummary(selectedRpcServer);
            NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
            for (NetworkInfo networkInfo : networks) {
                if (networkInfo.name.equals(selectedRpcServer)) {
                    ethereumNetworkRepository.setDefaultNetworkInfo(networkInfo);
                    return;
                }
            }
        }
    }

    private void setRpcServerPreferenceData(ListPreference lp) {
        NetworkInfo[] networks = ethereumNetworkRepository.getAvailableNetworkList();
        CharSequence[] entries = new CharSequence[networks.length];
        for (int ii = 0; ii < networks.length; ii++) {
            entries[ii] = networks[ii].name;
        }

        CharSequence[] entryValues = new CharSequence[networks.length];
        for (int ii = 0; ii < networks.length; ii++) {
            entryValues[ii] = networks[ii].name;
        }

        String currentValue = ethereumNetworkRepository.getDefaultNetwork().name;

        lp.setEntries(entries);
        lp.setDefaultValue(currentValue);
        lp.setValue(currentValue);
        lp.setSummary(currentValue);
        lp.setEntryValues(entryValues);
    }
}

