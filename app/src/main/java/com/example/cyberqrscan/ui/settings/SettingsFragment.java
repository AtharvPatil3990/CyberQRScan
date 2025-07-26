package com.example.cyberqrscan.ui.settings;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import com.example.cyberqrscan.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
 setPreferencesFromResource(R.xml.settings_preference, rootKey);

        // Find the ListPreference by key
        ListPreference themePreference = findPreference("prefAppTheme");

        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedTheme = (String) newValue;

                switch (selectedTheme) {
                    case "light":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case "dark":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                }

                return true; 
            });
        }
    }
}