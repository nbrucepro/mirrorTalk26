package com.example.mirrortalk26.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.SpeechSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    // ── Click callback ──────────────────────────────────────
    public interface OnSessionClickListener {
        void onSessionClick(long sessionId);
    }

    private OnSessionClickListener listener;

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }
    public SpeechSession getSessionAt(int position) {
        return sessions.get(position);
    }

    private List<SpeechSession> sessions = new ArrayList<>();

    public void setSessions(List<SpeechSession> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SpeechSession session = sessions.get(position);

        String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(session.timestamp));
        holder.tvDate.setText(date);
        holder.tvVersion.setText("Ver. " + session.version);
        holder.tvWpm.setText(String.valueOf((int) session.averageWpm));
        holder.tvFillers.setText(String.valueOf(session.fillerWordCount));
        holder.tvEye.setText((int) session.eyeContactPercent + "%");
        holder.tvDuration.setText(session.durationSeconds + "s");

        // Confidence score on card
        float wpmScore;
        int wpm = (int) session.averageWpm;
        if (wpm >= 120 && wpm <= 160)     wpmScore = 100f;
        else if (wpm >= 100)              wpmScore = 75f;
        else if (wpm > 0)                 wpmScore = 40f;
        else                              wpmScore = 0f;

        float fillerScore = Math.max(0f, 100f - (session.fillerWordCount * 10f));
        int score = (int)((session.eyeContactPercent * 0.40f)
                + (wpmScore * 0.35f)
                + (fillerScore * 0.25f));

        holder.tvScore.setText(score + "");

        // Colour score
        if (score >= 80)      holder.tvScore.setTextColor(0xFF1D9E75);
        else if (score >= 60) holder.tvScore.setTextColor(0xFF7F77DD);
        else if (score >= 40) holder.tvScore.setTextColor(0xFFFFCC44);
        else                  holder.tvScore.setTextColor(0xFFFF6B6B);

        // Navigate on tap
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSessionClick(session.id);
        });
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvVersion, tvWpm, tvFillers, tvEye, tvDuration, tvScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate     = itemView.findViewById(R.id.tvDate);
            tvVersion  = itemView.findViewById(R.id.tvVersion);
            tvWpm      = itemView.findViewById(R.id.tvWpm);
            tvFillers  = itemView.findViewById(R.id.tvFillers);
            tvEye      = itemView.findViewById(R.id.tvEye);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvScore    = itemView.findViewById(R.id.tvScore);
        }
    }
}