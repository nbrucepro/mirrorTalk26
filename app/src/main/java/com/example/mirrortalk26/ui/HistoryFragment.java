package com.example.mirrortalk26.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.repository.SessionRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recyclerSessions);
        TextView tvEmpty      = view.findViewById(R.id.tvEmpty);
        LineChart lineChart   = view.findViewById(R.id.lineChart);

        // Back button
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        SessionAdapter adapter = new SessionAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // ── Session click → navigate to detail ─────────────
        adapter.setOnSessionClickListener(sessionId -> {
            Bundle bundle = new Bundle();
            bundle.putLong("sessionId", sessionId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_history_to_result, bundle);
        });
        // ── Swipe left to delete
        SessionRepository repository = new SessionRepository(requireActivity().getApplication());
        new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                        0,androidx.recyclerview.widget.ItemTouchHelper.LEFT
                ){

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        SpeechSession session = adapter.getSessionAt(position);

                        // Show confirmation dialog
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Delete Session")
                                .setMessage("Delete this session from " +
                                        new java.text.SimpleDateFormat("MMM dd, HH:mm",
                                                java.util.Locale.getDefault())
                                                .format(new java.util.Date(session.timestamp)) + "?")

                                .setPositiveButton("Delete", (dialog, which) ->
                                        repository.deleteById(session.id))
                                .setNegativeButton("Cancel", (dialog, which) ->
                                        adapter.notifyItemChanged(position)) // restore on cancel
                                .setOnCancelListener(d ->
                                        adapter.notifyItemChanged(position))
                                .show();
                    }
                    @Override
                    public void onChildDraw(@NonNull android.graphics.Canvas c,
                                            @NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            float dX, float dY, int state, boolean active) {
                        // Draw red background while swiping
                        android.graphics.Paint paint = new android.graphics.Paint();
                        paint.setColor(0xFFFF4444);
                        c.drawRect(vh.itemView.getRight() + dX, vh.itemView.getTop(),
                                vh.itemView.getRight(), vh.itemView.getBottom(), paint);

                        // Draw trash icon text
                        paint.setColor(0xFFFFFFFF);
                        paint.setTextSize(36f);
                        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
                        float textY = vh.itemView.getTop() +
                                (vh.itemView.getHeight() / 2f) + 12f;
                        c.drawText("🗑", vh.itemView.getRight() - 60, textY, paint);

                        super.onChildDraw(c, rv, vh, dX, dY, state, active);
                    }
                }
        ).attachToRecyclerView(recycler);


        AppDatabase.getInstance(requireContext())
                .sessionDao()
                .getAllSessions()
                .observe(getViewLifecycleOwner(), sessions -> {

                    if (sessions == null || sessions.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recycler.setVisibility(View.GONE);
                        lineChart.setVisibility(View.GONE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    lineChart.setVisibility(View.VISIBLE);
                    adapter.setSessions(sessions);

                    // Chart — oldest to newest
                    List<SpeechSession> chronological = new ArrayList<>(sessions);
                    Collections.reverse(chronological);

                    List<Entry> wpmEntries = new ArrayList<>();
                    for (int i = 0; i < chronological.size(); i++) {
                        wpmEntries.add(new Entry(i, chronological.get(i).averageWpm));
                    }

                    LineDataSet dataSet = new LineDataSet(wpmEntries, "WPM per session");
                    dataSet.setColor(Color.parseColor("#7F77DD"));
                    dataSet.setCircleColor(Color.parseColor("#7F77DD"));
                    dataSet.setLineWidth(2.5f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(false);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSet.setDrawFilled(true);
                    dataSet.setFillColor(Color.parseColor("#3C3489"));
                    dataSet.setFillAlpha(80);

                    lineChart.setData(new LineData(dataSet));
                    lineChart.setBackgroundColor(Color.parseColor("#1A1A2E"));
                    lineChart.getDescription().setEnabled(false);
                    lineChart.getLegend().setTextColor(Color.WHITE);
                    lineChart.getAxisLeft().setTextColor(Color.parseColor("#AAAACC"));
                    lineChart.getAxisLeft().setGridColor(Color.parseColor("#2A2A40"));
                    lineChart.getAxisRight().setEnabled(false);
                    lineChart.getXAxis().setTextColor(Color.parseColor("#AAAACC"));
                    lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                    lineChart.getXAxis().setGridColor(Color.parseColor("#2A2A40"));
                    lineChart.setTouchEnabled(true);
                    lineChart.invalidate();
                });
    }
}