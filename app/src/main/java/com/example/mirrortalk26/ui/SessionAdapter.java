package com.example.mirrortalk26.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.util.ScoreUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

    // ── Tap callback (navigate to result) ─────────────────────────────────────
    public interface OnSessionClickListener {
        void onSessionClick(long sessionId);
    }

    // ── Long-press comparison callback (fires when exactly 2 selected) ─────────
    public interface OnCompareSelectedListener {
        void onTwoSelected(long idA, long idB);
    }

    private OnSessionClickListener    clickListener;
    private OnCompareSelectedListener compareListener;

    public void setOnSessionClickListener(OnSessionClickListener l)      { clickListener  = l; }
    public void setOnCompareSelectedListener(OnCompareSelectedListener l) { compareListener = l; }

    private List<SpeechSession> sessions = new ArrayList<>();
    private final Set<Long>     selected = new HashSet<>();

    public void setSessions(List<SpeechSession> newSessions) {
        this.sessions = newSessions;
        selected.clear();
        notifyDataSetChanged();
    }

    public SpeechSession getSessionAt(int position) { return sessions.get(position); }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new ViewHolder(v);
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

        // FIX 3: Use ScoreUtils so History card scores match ResultFragment exactly.
        // Old code was "else if (wpm >= 100) wpmScore = 75f" — never penalised WPM > 180.
        int score = ScoreUtils.computeScore(
                session.averageWpm, session.fillerWordCount, session.eyeContactPercent);
        holder.tvScore.setText(score + "");
        if      (score >= 80) holder.tvScore.setTextColor(0xFF1D9E75);
        else if (score >= 60) holder.tvScore.setTextColor(0xFF7F77DD);
        else if (score >= 40) holder.tvScore.setTextColor(0xFFFFCC44);
        else                  holder.tvScore.setTextColor(0xFFFF6B6B);

        boolean isSelected = selected.contains(session.id);
        holder.itemView.setAlpha(isSelected ? 1.0f : 0.85f);
        holder.itemView.setBackgroundColor(isSelected ? 0xFF1A2A3A : 0x00000000);

        holder.itemView.setOnClickListener(v -> {
            if (selected.isEmpty()) {
                if (clickListener != null) clickListener.onSessionClick(session.id);
            } else {
                toggleSelection(session.id, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            toggleSelection(session.id, position);
            return true;
        });
    }

    private void toggleSelection(long id, int position) {
        if (selected.contains(id)) {
            selected.remove(id);
            notifyItemChanged(position);
        } else {
            if (selected.size() < 2) {
                selected.add(id);
                notifyItemChanged(position);
            }
        }
        if (selected.size() == 2 && compareListener != null) {
            Long[] ids = selected.toArray(new Long[0]);
            compareListener.onTwoSelected(ids[0], ids[1]);
            selected.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() { return sessions.size(); }

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