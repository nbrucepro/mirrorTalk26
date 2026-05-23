package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;

import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_home,container,false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        TextView tvCount = view.findViewById(R.id.tvSessionCount);

        // Load session count from DB
        Executors.newSingleThreadExecutor().execute(() -> {
            int count = AppDatabase.getInstance(requireContext())
                    .sessionDao().getSessionCount();
            requireActivity().runOnUiThread(() ->
                    tvCount.setText(count + (count == 1 ? " session" : " sessions") + " completed"));
        });
        // Start Session button → go to RecordingFragment
        view.findViewById(R.id.btnStart).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_home_to_recording)
        );
        // History
        view.findViewById(R.id.btnHistory).setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.historyFragment));
        // Settings
        view.findViewById(R.id.btnSettings).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_home_to_settings));
    }
}
