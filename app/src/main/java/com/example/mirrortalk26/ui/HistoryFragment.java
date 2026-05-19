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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_home,container,false);

    }
    @Override
    public void onViewCreated(@NonNull View view,@Nullable Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recyclerSessions);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);
        LineChart lineChart   = view.findViewById(R.id.lineChart);

        SessionAdapter adapter = new SessionAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // Load sessions from DB on background thread
        AppDatabase.getInstance(requireContext())
                .sessionDao()
                .getAllSessions()
                .observe(getViewLifecycleOwner(), sessions -> {
                    if(sessions == null || sessions.isEmpty()){
                        tvEmpty.setVisibility(View.VISIBLE);
                        recycler.setVisibility(View.GONE);
                        lineChart.setVisibility(View.GONE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    lineChart.setVisibility(View.VISIBLE);
                    // Update list
                    adapter.setSesions(sessions);

                    // Build WPM chart — oldest to newest
                    List<SpeechSession> chronological = new ArrayList<>(sessions);
                    Collections.reverse(chronological);

                    List<Entry> wpmEntries = new ArrayList<>();
                    for (int i = 0; i<chronological.size(); i++){
                        wpmEntries.add(new Entry(i,chronological.get(i).averageWpm));
                    }

                    LineDataSet dataSet = new LineDataSet(wpmEntries, "WPM per session");
                    dataSet.setColor(Color.parseColor("#7F77DD"));
                    dataSet.setCircleColor(Color.parseColor("#7F77DD"));
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(false);
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    dataSet.setDrawFilled(true);
                    dataSet.setFillColor(Color.parseColor("#3C3489"));
                    dataSet.setFillAlpha(60);

                    // Style the chart
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
                    lineChart.invalidate();

                });
    }
}
