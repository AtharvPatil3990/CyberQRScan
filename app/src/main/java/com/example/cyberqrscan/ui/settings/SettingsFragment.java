package com.example.cyberqrscan.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.example.cyberqrscan.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    PackageManager pm;
    PackageInfo pInfo;
    String appVersion;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);

        Preference versionPref = findPreference("prefVersion");
        ListPreference themePreference = findPreference("prefAppTheme");
        Preference reportBugPref = findPreference("prefReportBug");

//        Version Preference
        try {
            pm = requireContext().getPackageManager();
            pInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);;
            pInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);
            appVersion = "v"+pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            appVersion = "Version not found";
        }

        assert versionPref != null;
        versionPref.setSummary(appVersion);
        // Theme Preference Listener
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
        else{
            Toast.makeText(requireContext(), "Error occurred", Toast.LENGTH_SHORT).show();
        }

    //  BugReport listener
        if (reportBugPref != null) {
            reportBugPref.setOnPreferenceClickListener(preference -> {
                sendBugReportEmail();
                return true;
            });
        }
    }

    private void sendBugReportEmail(){
        String deviceInfo = "Brand: " + Build.BRAND + "\n" +
                "Model: " + Build.MODEL + "\n" +
                "Android Version: " + Build.VERSION.RELEASE + "\n" +
                "App Version: " + appVersion;
        String emailBody = "Please describe the bug below:\n\n\n\n\n---\n" + deviceInfo;
        String devEmail=getString(R.string.reportEmail);
        Intent sendReportEmailIntent = new Intent(Intent.ACTION_SENDTO);
        sendReportEmailIntent.setData(Uri.parse("mailto:"));
        sendReportEmailIntent.putExtra(Intent.EXTRA_EMAIL, devEmail);
        sendReportEmailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - QR Scanner App");
        sendReportEmailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
        startActivity(sendReportEmailIntent);
    }

}