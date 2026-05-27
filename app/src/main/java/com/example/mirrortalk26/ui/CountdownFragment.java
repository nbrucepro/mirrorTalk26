package com.example.mirrortalk26.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.mirrortalk26.R;

/**
 * 3-second animated countdown shown before RecordingFragment.
 * Navigates automatically when done — no user input required.
 */
public class CountdownFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int count = 3;
    private TextView tvCount;
    private TextView tvPrompt;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_countdown, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvCount  = view.findViewById(R.id.tvCountdown);
        tvPrompt = view.findViewById(R.id.tvCountdownPrompt);
        tick();
    }

    private void tick() {
        if (!isAdded()) return;

        if (count > 0) {
            tvCount.setText(String.valueOf(count));
            tvPrompt.setText(count == 3 ? "Get ready…"
                           : count == 2 ? "Take a breath"
                                        : "Here we go");
            animatePop(tvCount);
            count--;
            handler.postDelayed(this::tick, 1000);
        } else {
            // Navigate to recording
            if (isAdded()) {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_countdown_to_recording);
            }
        }
    }

    private void animatePop(View v) {
        ScaleAnimation scale = new ScaleAnimation(
                0.5f, 1f, 0.5f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(400);

        AlphaAnimation fade = new AlphaAnimation(0.3f, 1f);
        fade.setDuration(300);

        AnimationSet set = new AnimationSet(true);
        set.addAnimation(scale);
        set.addAnimation(fade);
        v.startAnimation(set);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
