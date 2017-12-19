package com.wallet.crypto.trustapp.views;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
//import android.preference.PreferenceFragment;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.Controller;
import com.wallet.crypto.trustapp.model.VMNetwork;

import java.util.List;

public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener{
    private Controller mController;
    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.fragment_settings);

        mController = Controller.with(getActivity());

        final Preference donate = findPreference("pref_donate");

        donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                mController.navigateToSend(getActivity(), "0x9f8284ce2cf0c8ce10685f537b1fff418104a317");
                return false;
            }
        });

        final Preference export = findPreference("pref_export");

        export.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
	            ExportAccountActivity.open(getActivity(), mController.getCurrentAccount().getAddress());
                return false;
            }
        });

        final ListPreference listPreference = (ListPreference) findPreference("pref_rpcServer");

        // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
        setRpcServerPreferenceData(listPreference);

        listPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                setRpcServerPreferenceData(listPreference);
                return false;
            }
        });

        String versionString = Controller.with(getActivity()).getVersion();

        Preference version = findPreference("pref_version");
        version.setSummary(versionString);

        preferences = PreferenceManager
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
            mController.setCurrentNetwork(selectedRpcServer);
        }
    }

    private void setRpcServerPreferenceData(ListPreference lp) {
        List<VMNetwork> networks = mController.getNetworks();

        assert(networks.size() > 0);

        CharSequence[] entries = new CharSequence[networks.size()];
        for (int ii = 0; ii < networks.size(); ii++) {
            entries[ii] = networks.get(ii).getName();
        }

        CharSequence[] entryValues = new CharSequence[networks.size()];
        for (int ii = 0; ii < networks.size(); ii++) {
            entryValues[ii] = networks.get(ii).getName();
        }

        String currentValue = mController.getCurrentNetwork().getName();

        lp.setEntries(entries);
        lp.setDefaultValue(currentValue);
        lp.setValue(currentValue);
        lp.setSummary(currentValue);
        lp.setEntryValues(entryValues);
    }
}

