package com.example.mirrortalk26.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.SpeechSession;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {
    private List<SpeechSession> sessions = new ArrayList<>();

    public void setSesions(List<SpeechSession> newSessions){
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session,parent,false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position){
        SpeechSession session = sessions.get(position);
        // Date
        String date = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(session.timestamp));
        holder.tvDate.setText(date);

        // Version badge
        holder.tvVersion.setText("Ver. " + session.version);

        // Stats
        holder.tvWpm.setText(String.valueOf((int) session.averageWpm));
        holder.tvFillers.setText(String.valueOf(session.fillerWordCount));
        holder.tvEye.setText((int) session.eyeContactPercent + "%");
        holder.tvDuration.setText(String.valueOf(session.durationSeconds));

    }
    @Override
    public int getItemCount(){
        return sessions.size();
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvDate, tvVersion, tvWpm, tvFillers, tvEye, tvDuration;
        ViewHolder(@NonNull View itemView){
            super(itemView);
            tvDate    = itemView.findViewById(R.id.tvDate);
            tvVersion = itemView.findViewById(R.id.tvVersion);
            tvWpm     = itemView.findViewById(R.id.tvWpm);
            tvFillers = itemView.findViewById(R.id.tvFillers);
            tvEye     = itemView.findViewById(R.id.tvEye);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
