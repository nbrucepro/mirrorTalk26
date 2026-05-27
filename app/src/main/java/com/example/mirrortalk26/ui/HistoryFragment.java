package com.example.mirrortalk26.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mirrortalk26.R;
import com.example.mirrortalk26.data.AppDatabase;
import com.example.mirrortalk26.data.SpeechSession;
import com.example.mirrortalk26.repository.SessionRepository;
import com.example.mirrortalk26.util.ScoreUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    // FIX: create Paint once — not on every draw frame during swipe
    private final Paint bgPaint   = new Paint();
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler     = view.findViewById(R.id.recyclerSessions);
        TextView     tvEmpty      = view.findViewById(R.id.tvEmpty);
        TextView     tvCompareHint = view.findViewById(R.id.tvCompareHint);
        LineChart    chart        = view.findViewById(R.id.lineChart);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
        view.findViewById(R.id.btnExport).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.exportFragment));

        SessionAdapter adapter = new SessionAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        adapter.setOnSessionClickListener(sessionId -> {
            Bundle bundle = new Bundle();
            bundle.putLong("sessionId", sessionId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_history_to_result, bundle);
        });

        adapter.setOnCompareSelectedListener((idA, idB) -> {
            Bundle bundle = new Bundle();
            bundle.putLong("sessionIdA", idA);
            bundle.putLong("sessionIdB", idB);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_history_to_compare, bundle);
        });

        // ── Swipe-to-delete ────────────────────────────────────────────────────
        bgPaint.setColor(0xFFFF4444);

        // Load ic_delete as a bitmap once for canvas drawing
        Drawable deleteDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete);
        Bitmap   deleteBitmap   = null;
        if (deleteDrawable != null) {
            int size = (int)(24 * getResources().getDisplayMetrics().density);
            deleteDrawable.setBounds(0, 0, size, size);
            deleteBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas bmpCanvas = new Canvas(deleteBitmap);
            deleteDrawable.setTint(0xFFFFFFFF);
            deleteDrawable.draw(bmpCanvas);
        }
        final Bitmap finalDeleteBitmap = deleteBitmap;

        SessionRepository repo = new SessionRepository(requireActivity().getApplication());

        new androidx.recyclerview.widget.ItemTouchHelper(
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                    0, androidx.recyclerview.widget.ItemTouchHelper.LEFT) {

                @Override public boolean onMove(@NonNull RecyclerView rv,
                                                @NonNull RecyclerView.ViewHolder vh,
                                                @NonNull RecyclerView.ViewHolder t) { return false; }

                @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                    int pos = vh.getAdapterPosition();
                    SpeechSession s = adapter.getSessionAt(pos);
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Delete Session")
                            .setMessage("Delete this session from " +
                                    new java.text.SimpleDateFormat("MMM dd, HH:mm",
                                            java.util.Locale.getDefault())
                                            .format(new java.util.Date(s.timestamp)) + "?")
                            .setPositiveButton("Delete", (d, w) -> repo.deleteById(s.id))
                            .setNegativeButton("Cancel", (d, w) -> adapter.notifyItemChanged(pos))
                            .setOnCancelListener(d -> adapter.notifyItemChanged(pos))
                            .show();
                }

                @Override public void onChildDraw(@NonNull Canvas c,
                                                   @NonNull RecyclerView rv,
                                                   @NonNull RecyclerView.ViewHolder vh,
                                                   float dX, float dY, int state, boolean active) {
                    // Red background
                    c.drawRect(vh.itemView.getRight() + dX, vh.itemView.getTop(),
                            vh.itemView.getRight(), vh.itemView.getBottom(), bgPaint);

                    // Delete icon (vector bitmap, no emoji)
                    if (finalDeleteBitmap != null) {
                        float iconTop = vh.itemView.getTop()
                                + (vh.itemView.getHeight() - finalDeleteBitmap.getHeight()) / 2f;
                        float iconLeft = vh.itemView.getRight() - finalDeleteBitmap.getWidth() - 32;
                        c.drawBitmap(finalDeleteBitmap, iconLeft, iconTop, iconPaint);
                    }

                    super.onChildDraw(c, rv, vh, dX, dY, state, active);
                }
            }
        ).attachToRecyclerView(recycler);

        // ── Observe sessions ──────────────────────────────────────────────────
        AppDatabase.getInstance(requireContext())
                .sessionDao()
                .getAllSessions()
                .observe(getViewLifecycleOwner(), sessions -> {

                    if (sessions == null || sessions.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvCompareHint.setVisibility(View.GONE);
                        recycler.setVisibility(View.GONE);
                        chart.setVisibility(View.GONE);
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    recycler.setVisibility(View.VISIBLE);
                    chart.setVisibility(View.VISIBLE);
                    tvCompareHint.setVisibility(sessions.size() >= 2 ? View.VISIBLE : View.GONE);

                    adapter.setSessions(sessions);

                    // ── Chart (oldest → newest) ──────────────────────────────
                    List<SpeechSession> chron = new ArrayList<>(sessions);
                    Collections.reverse(chron);

                    List<Entry> wpmEntries   = new ArrayList<>();
                    List<Entry> scoreEntries = new ArrayList<>();

                    for (int i = 0; i < chron.size(); i++) {
                        SpeechSession s = chron.get(i);
                        wpmEntries.add(new Entry(i, s.averageWpm));
                        // FIX: use ScoreUtils so chart confidence matches ResultFragment exactly
                        float score = ScoreUtils.computeScore(s.averageWpm, s.fillerWordCount, s.eyeContactPercent);
                        scoreEntries.add(new Entry(i, score));
                    }

                    LineDataSet wpmSet = new LineDataSet(wpmEntries, "WPM");
                    wpmSet.setColor(Color.parseColor("#7F77DD"));
                    wpmSet.setCircleColor(Color.parseColor("#7F77DD"));
                    wpmSet.setLineWidth(2.5f);
                    wpmSet.setCircleRadius(4f);
                    wpmSet.setDrawValues(false);
                    wpmSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    wpmSet.setDrawFilled(true);
                    wpmSet.setFillColor(Color.parseColor("#3C3489"));
                    wpmSet.setFillAlpha(60);
                    wpmSet.setAxisDependency(YAxis.AxisDependency.LEFT);

                    LineDataSet scoreSet = new LineDataSet(scoreEntries, "Confidence");
                    scoreSet.setColor(Color.parseColor("#1D9E75"));
                    scoreSet.setCircleColor(Color.parseColor("#1D9E75"));
                    scoreSet.setLineWidth(2f);
                    scoreSet.setCircleRadius(3.5f);
                    scoreSet.setDrawValues(false);
                    scoreSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    scoreSet.setDrawFilled(true);
                    scoreSet.setFillColor(Color.parseColor("#1D9E75"));
                    scoreSet.setFillAlpha(30);
                    scoreSet.enableDashedLine(10f, 4f, 0f);
                    scoreSet.setAxisDependency(YAxis.AxisDependency.RIGHT);

                    chart.setData(new LineData(wpmSet, scoreSet));
                    chart.setBackgroundColor(Color.parseColor("#1A1A2E"));
                    chart.getDescription().setEnabled(false);
                    chart.setTouchEnabled(true);

                    YAxis left = chart.getAxisLeft();
                    left.setTextColor(Color.parseColor("#7F77DD"));
                    left.setGridColor(Color.parseColor("#2A2A40"));
                    left.setAxisMinimum(0f);
                    left.setAxisMaximum(350f);

                    YAxis right = chart.getAxisRight();
                    right.setEnabled(true);
                    right.setTextColor(Color.parseColor("#1D9E75"));
                    right.setGridColor(Color.TRANSPARENT);
                    right.setAxisMinimum(0f);
                    right.setAxisMaximum(100f);
                    right.setLabelCount(6, true);

                    XAxis xAxis = chart.getXAxis();
                    xAxis.setTextColor(Color.parseColor("#AAAACC"));
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setGridColor(Color.parseColor("#2A2A40"));
                    xAxis.setGranularity(1f);

                    Legend legend = chart.getLegend();
                    legend.setTextColor(Color.WHITE);
                    legend.setTextSize(11f);
                    legend.setForm(Legend.LegendForm.LINE);

                    chart.animateX(500);
                    chart.invalidate();
                });
    }
}
