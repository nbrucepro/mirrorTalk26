package com.example.mirrortalk26.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mirrortalk26.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {
    public static final String PREF_VERSION    = "version";
    public static final String PREF_SAVE_VIDEO = "save_video";

    private SharedPreferences prefs;
    private View optionA;
    private View optionB;
    private RadioButton radioA;
    private RadioButton radioB;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        optionA = view.findViewById(R.id.optionA);
        optionB = view.findViewById(R.id.optionB);
        radioA  = view.findViewById(R.id.radioA);
        radioB  = view.findViewById(R.id.radioB);
        SwitchMaterial switchVideo = view.findViewById(R.id.switchVideo);

        // Load saved preferences
        String  currentVersion = prefs.getString(PREF_VERSION, "A");
        boolean saveVideo      = prefs.getBoolean(PREF_SAVE_VIDEO, false);

        switchVideo.setChecked(saveVideo);
        // Restore visual selection to match saved state
        selectVersion(currentVersion.equals("A"));

        // FIX 4: Toggle backgrounds AND radio states together.
        // Old code only updated radioA/radioB.isChecked() but never swapped the
        // optionA/optionB background drawables, so the highlighted border never moved.
        optionA.setOnClickListener(v -> selectVersion(true));
        optionB.setOnClickListener(v -> selectVersion(false));

        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            String version = radioA.isChecked() ? "A" : "B";
            prefs.edit()
                    .putString(PREF_VERSION, version)
                    .putBoolean(PREF_SAVE_VIDEO, switchVideo.isChecked())
                    .apply();
            // FIX 5: Was "Settings saved! version" + version — missing space.
            Toast.makeText(requireContext(),
                    "Settings saved! Version " + version + " active",
                    Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    /**
     * Selects version A (isA=true) or B (isA=false).
     * Updates both the radio button checked states AND the container backgrounds
     * so the highlighted border matches the current selection.
     */
    private void selectVersion(boolean isA) {
        radioA.setChecked(isA);
        radioB.setChecked(!isA);
        optionA.setBackgroundResource(isA  ? R.drawable.option_selected : R.drawable.option_unselected);
        optionB.setBackgroundResource(!isA ? R.drawable.option_selected : R.drawable.option_unselected);
    }
}