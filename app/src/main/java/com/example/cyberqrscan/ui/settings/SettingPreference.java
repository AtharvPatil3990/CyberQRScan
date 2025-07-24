package com.example.cyberqrscan.ui.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.example.cyberqrscan.R;

public class SettingPreference extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
    }
}
