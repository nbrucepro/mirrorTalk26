package com.example.mirrortalk26.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mirrortalk26.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Shown once on first launch (FR-1 User Profile).
 * Stores the user's name and primary speaking goal.
 * HomeFragment reads these to personalise the greeting and prompt.
 */
public class OnboardingFragment extends Fragment {

    public static final String PREF_ONBOARDING_DONE = "onboarding_done";
    public static final String PREF_USER_NAME        = "user_name";
    public static final String PREF_USER_GOAL        = "user_goal";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputLayout    tilName  = view.findViewById(R.id.tilName);
        TextInputEditText  etName   = view.findViewById(R.id.etName);
        RadioGroup         rgGoal   = view.findViewById(R.id.rgGoal);
        MaterialButton     btnStart = view.findViewById(R.id.btnGetStarted);

        btnStart.setOnClickListener(v -> {
            String name = etName.getText() != null
                    ? etName.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name)) {
                tilName.setError("Please enter your name");
                return;
            }
            tilName.setError(null);

            int checkedId = rgGoal.getCheckedRadioButtonId();
            String goal = "General improvement";
            if (checkedId != -1) {
                RadioButton rb = view.findViewById(checkedId);
                if (rb != null) goal = rb.getText().toString();
            }

            SharedPreferences prefs = androidx.preference.PreferenceManager
                    .getDefaultSharedPreferences(requireContext());
            prefs.edit()
                    .putBoolean(PREF_ONBOARDING_DONE, true)
                    .putString(PREF_USER_NAME,         name)
                    .putString(PREF_USER_GOAL,         goal)
                    .apply();

            Navigation.findNavController(view)
                    .navigate(R.id.action_onboarding_to_home);
        });
    }
}
