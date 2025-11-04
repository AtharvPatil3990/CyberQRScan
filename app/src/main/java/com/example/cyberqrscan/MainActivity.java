package com.example.cyberqrscan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.example.cyberqrscan.ui.settings.AppInfo;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navView = findViewById(R.id.nav_view);

        // Get NavHostFragment from XML
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_history, R.id.navigation_settings)
                .build();

        // Setup ActionBar & BottomNavigationView
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        AppInfo.init(MainActivity.this); // Passed Context for getting app version info

        if (NetworkUtil.isNetworkAvailable(MainActivity.this)) {
            SharedPreferences prefs = MainActivity.this.getSharedPreferences("SafeBrowsingPrefs", Context.MODE_PRIVATE);
            long lastUpdateTime = prefs.getLong("last_URLdb_update_time", 0);
            Toast.makeText(MainActivity.this, "network available", Toast.LENGTH_SHORT).show();
            System.out.println("Network available");

            //            long twoHoursMillis = 2 * 60 * 60 * 1000;
            long twoHoursMillis = 0;

            if((System.currentTimeMillis() - lastUpdateTime) >= twoHoursMillis){
                SafeBrowsingAPI api = new SafeBrowsingAPI(MainActivity.this);
                System.out.println("Entering update threat list function");
                api.updateThreatList();
            }
        } else {
            System.out.println("No Internet connection");
            Toast.makeText(MainActivity.this, "No internet connection!", Toast.LENGTH_SHORT).show();
        }


        System.out.println("All hashes:");
        QRDatabase db = new QRDatabase(MainActivity.this);
        db.showAllHashes();
        db.close();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
