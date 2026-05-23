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
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        RadioButton radioA = view.findViewById(R.id.radioA);
        RadioButton radioB   = view.findViewById(R.id.radioB);
        SwitchMaterial switchVideo = view.findViewById(R.id.switchVideo);
        // Load saved preferences
        String currentVersion = prefs.getString(PREF_VERSION, "A");
        boolean saveVideo     = prefs.getBoolean(PREF_SAVE_VIDEO, false);

        radioA.setChecked(currentVersion.equals("A"));
        radioB.setChecked(currentVersion.equals("B"));
        switchVideo.setChecked(saveVideo);
        // Toggle between A and B
        view.findViewById(R.id.optionA).setOnClickListener(v -> {
            radioA.setChecked(true);
            radioB.setChecked(false);
        });
        view.findViewById(R.id.optionB).setOnClickListener(v -> {
            radioA.setChecked(false);
            radioB.setChecked(true);
        });
        view.findViewById(R.id.btnSaveSettings).setOnClickListener(v->{
        String version = radioA.isChecked() ? "A" : "B";
        prefs.edit()
                .putString(PREF_VERSION,version)
                .putBoolean(PREF_SAVE_VIDEO,switchVideo.isChecked())
                .apply();
            Toast.makeText(requireContext(),
                    "Settings saved! version" + version + " active",
                    Toast.LENGTH_LONG).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
    });
        //back button
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
}}
