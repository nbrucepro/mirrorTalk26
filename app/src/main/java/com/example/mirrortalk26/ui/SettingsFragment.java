package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;   // FIX: was android.preference (deprecated API 29+)

import com.example.mirrortalk26.R;

/**
 * Settings fragment — matches the ORIGINAL fragment_settings.xml IDs exactly:
 *   optionA, optionB, radioA, radioB, switchVideo, btnSaveSettings, btnBack
 *
 * Previous version referenced cardVersionA/cardVersionB/rbVersionA/rbVersionB
 * which don't exist in the layout.
 */
public class SettingsFragment extends Fragment {

    private static final String PREF_VERSION = "selected_version";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Read current version from prefs
        String current = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString(PREF_VERSION, "A");

        // IDs from the ORIGINAL fragment_settings.xml
        View        optionA = view.findViewById(R.id.optionA);
        View        optionB = view.findViewById(R.id.optionB);
        RadioButton radioA  = view.findViewById(R.id.radioA);
        RadioButton radioB  = view.findViewById(R.id.radioB);

        applySelection(current, optionA, optionB, radioA, radioB);

        View.OnClickListener listener = v -> {
            String version = (v.getId() == R.id.optionA || v.getId() == R.id.radioA) ? "A" : "B";
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit().putString(PREF_VERSION, version).apply();
            applySelection(version, optionA, optionB, radioA, radioB);
        };

        optionA.setOnClickListener(listener);
        optionB.setOnClickListener(listener);
        radioA.setOnClickListener(listener);
        radioB.setOnClickListener(listener);

        // Save button — version already saved on click above; this just pops back
        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void applySelection(String version,
                                View optionA, View optionB,
                                RadioButton radioA, RadioButton radioB) {
        boolean isA = "A".equals(version);
        radioA.setChecked(isA);
        radioB.setChecked(!isA);
        optionA.setBackgroundResource(isA
                ? R.drawable.option_selected : R.drawable.option_unselected);
        optionB.setBackgroundResource(!isA
                ? R.drawable.option_selected : R.drawable.option_unselected);
    }
}
