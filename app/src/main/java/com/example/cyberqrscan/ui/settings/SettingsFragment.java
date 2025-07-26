package com.example.cyberqrscan.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;

import com.example.cyberqrscan.databinding.FragmentSettingsBinding;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FragmentSettingsBinding binding;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
         setPreferencesFromResource(R.xml.settings_preference, rootKey);
        
        // ListPreference by key
        ListPreference themePreference = findPreference("prefAppTheme");

        if (themePreference != null) {
            // Set the listener
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedTheme = (String) newValue;

                switch (selectedTheme) {
                    case "light":
                    // light theme    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                    case "dark":
                    // dark theme    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    
                    default:
                    // system_default theme AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                }
                return true;
           });
       }
        
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}