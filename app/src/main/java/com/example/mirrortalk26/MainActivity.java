package com.example.mirrortalk26;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import com.example.mirrortalk26.ui.OnboardingFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        // If onboarding is already done, skip straight to Home.
        boolean done = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(OnboardingFragment.PREF_ONBOARDING_DONE, false);

        if (done) {
            // Pop onboarding off the back stack and land on Home
            navController.navigate(R.id.action_onboarding_to_home);
        }
        // else: nav_graph startDestination=onboardingFragment handles it
    }
}
