package dev.ikteder.reachgrid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

import dev.ikteder.reachgrid.core.ReachComparison;
import dev.ikteder.reachgrid.core.ReachComparisonPlan;
import dev.ikteder.reachgrid.core.ReachHistory;
import dev.ikteder.reachgrid.core.ReachReport;
import dev.ikteder.reachgrid.core.ReachSession;

public final class MainActivity extends Activity {
    private static final int PAGE_BACKGROUND = Color.rgb(244, 246, 250);
    private static final int INK = Color.rgb(26, 43, 73);
    private static final String PREFERENCES = "reachgrid_local";
    private static final String SAVE_ENABLED = "save_enabled";
    private static final String HISTORY = "history_v1";

    private Spinner modeSpinner;
    private Spinner handSpinner;
    private Spinner sizeSpinner;
    private CheckBox saveHistoryCheck;
    private TextView historyText;
    private TextView statusText;
    private Button startButton;
    private Button resetButton;
    private Button shareButton;
    private Button shareHistoryButton;
    private Button clearHistoryButton;
    private ReachGridView gridView;
    private ReachSession session;
    private ReachReport report;
    private ReachReport firstPairReport;
    private ReachComparisonPlan comparisonPlan;
    private ReachComparison comparison;
    private int pairPhase = -1;
    private boolean awaitingSecondPhase;
    private int activeTargetRadiusDp;
    private String activeHand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("ReachGrid");
        buildInterface();
    }

    private void buildInterface() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(16), dp(18), dp(18));
        page.setBackgroundColor(PAGE_BACKGROUND);

        TextView title = text("ReachGrid", 28, true);
        page.addView(title, matchWrap());
        TextView intro = text(getString(R.string.session_intro), 14, false);
        intro.setPadding(0, dp(4), 0, dp(10));
        page.addView(intro, matchWrap());

        modeSpinner = spinner(new String[]{
                "Single session", "Pair 24 / 32 dp", "Pair 32 / 40 dp", "Pair 24 / 40 dp"});
        page.addView(modeSpinner, matchWrap());

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        settings.setGravity(Gravity.CENTER_VERTICAL);
        handSpinner = spinner(new String[]{"Right hand", "Left hand"});
        sizeSpinner = spinner(new String[]{"Single: 32 dp", "Single: 40 dp", "Single: 24 dp"});
        settings.addView(handSpinner, weighted());
        settings.addView(sizeSpinner, weighted());
        page.addView(settings, matchWrap());

        LinearLayout historySettings = new LinearLayout(this);
        historySettings.setOrientation(LinearLayout.HORIZONTAL);
        historySettings.setGravity(Gravity.CENTER_VERTICAL);
        saveHistoryCheck = new CheckBox(this);
        saveHistoryCheck.setText("Save completed sessions locally");
        SharedPreferences preferences = preferences();
        saveHistoryCheck.setChecked(preferences.getBoolean(SAVE_ENABLED, false));
        saveHistoryCheck.setOnCheckedChangeListener((button, checked) ->
                preferences.edit().putBoolean(SAVE_ENABLED, checked).apply());
        historyText = text("", 13, false);
        historyText.setGravity(Gravity.END);
        historySettings.addView(saveHistoryCheck, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        historySettings.addView(historyText, weighted());
        page.addView(historySettings, matchWrap());

        statusText = text("48 targets, two visits per zone", 14, true);
        statusText.setPadding(0, dp(10), 0, dp(8));
        page.addView(statusText, matchWrap());

        gridView = new ReachGridView(this);
        gridView.setListener(this::handleTap);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        page.addView(gridView, gridParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(12), 0, 0);
        startButton = button("Start session", view -> handleStart());
        resetButton = button("Reset", view -> resetSession());
        shareButton = button("Share JSON", view -> shareReport());
        resetButton.setEnabled(false);
        shareButton.setEnabled(false);
        actions.addView(startButton, weighted());
        actions.addView(resetButton, weighted());
        actions.addView(shareButton, weighted());
        page.addView(actions, matchWrap());

        LinearLayout historyActions = new LinearLayout(this);
        historyActions.setOrientation(LinearLayout.HORIZONTAL);
        shareHistoryButton = button("Share history", view -> shareHistory());
        clearHistoryButton = button("Clear history", view -> confirmClearHistory());
        historyActions.addView(shareHistoryButton, weighted());
        historyActions.addView(clearHistoryButton, weighted());
        page.addView(historyActions, matchWrap());

        setContentView(page);
        updateHistoryControls();
    }

    private void handleStart() {
        if (awaitingSecondPhase) {
            awaitingSecondPhase = false;
            pairPhase = 1;
            beginSession(comparisonPlan.radiusForPhase(1));
            return;
        }
        if (session != null || comparison != null) {
            clearMeasurementState();
        }
        activeHand = handSpinner.getSelectedItemPosition() == 0 ? "right" : "left";
        int mode = modeSpinner.getSelectedItemPosition();
        if (mode > 0) {
            int[][] pairs = {{24, 32}, {32, 40}, {24, 40}};
            int[] pair = pairs[mode - 1];
            comparisonPlan = new ReachComparisonPlan(dailySeed(), activeHand, pair[0], pair[1]);
            pairPhase = 0;
            beginSession(comparisonPlan.radiusForPhase(0));
            return;
        }
        int[] radii = {32, 40, 24};
        beginSession(radii[sizeSpinner.getSelectedItemPosition()]);
    }

    private void beginSession(int radiusDp) {
        activeTargetRadiusDp = radiusDp;
        long seed = comparisonPlan == null ? dailySeed() : comparisonPlan.seed();
        session = new ReachSession(seed);
        session.start(SystemClock.elapsedRealtime());
        report = null;
        comparison = null;
        setProtocolControlsEnabled(false);
        startButton.setEnabled(false);
        resetButton.setEnabled(true);
        shareButton.setEnabled(false);
        String prefix = comparisonPlan == null
                ? "Single session"
                : "Paired phase " + (pairPhase + 1) + " of 2, " + radiusDp + " dp";
        statusText.setText(prefix + ". Target 1 of " + session.totalTargets() + ".");
        gridView.startSession(session, activeTargetRadiusDp);
    }

    private void handleTap(ReachSession.TapResult result) {
        if (result.complete) {
            report = new ReachReport(session, activeHand, activeTargetRadiusDp);
            saveCompletedReport(report);
            gridView.showReport(report);
            shareButton.setEnabled(true);
            if (comparisonPlan != null && pairPhase == 0) {
                firstPairReport = report;
                awaitingSecondPhase = true;
                statusText.setText("Phase 1 complete. Rest, reset your grip, then run the assigned "
                        + comparisonPlan.radiusForPhase(1) + " dp phase.");
                startButton.setEnabled(true);
                startButton.setText("Start phase 2");
                return;
            }
            if (comparisonPlan != null && pairPhase == 1) {
                comparison = new ReachComparison(firstPairReport, report);
                gridView.showComparison(comparison);
                statusText.setText(String.format(Locale.ROOT,
                        "Pair complete. Second minus first: %+.1f ms, %+.1f score, %d misses.",
                        comparison.meanMedianLatencyDeltaMilliseconds(),
                        comparison.meanReachScoreDelta(), comparison.totalMissDelta()));
                finishProtocol();
                return;
            }
            statusText.setText("Complete. Scores combine latency and misses; raw evidence is in the JSON report.");
            finishProtocol();
            return;
        }
        if (result.hit) {
            String phase = comparisonPlan == null ? "" : "Phase " + (pairPhase + 1) + ". ";
            statusText.setText(phase + "Target " + (result.successfulTargets + 1)
                    + " of " + session.totalTargets());
        } else {
            statusText.setText("Miss " + result.missesOnCurrentTarget + " on this target. Try the same zone again.");
        }
    }

    private void finishProtocol() {
        setProtocolControlsEnabled(true);
        comparisonPlan = null;
        firstPairReport = null;
        pairPhase = -1;
        awaitingSecondPhase = false;
        startButton.setEnabled(true);
        startButton.setText("Start new run");
    }

    private void setProtocolControlsEnabled(boolean enabled) {
        modeSpinner.setEnabled(enabled);
        handSpinner.setEnabled(enabled);
        sizeSpinner.setEnabled(enabled);
    }

    private void clearMeasurementState() {
        session = null;
        report = null;
        firstPairReport = null;
        comparisonPlan = null;
        comparison = null;
        pairPhase = -1;
        awaitingSecondPhase = false;
        gridView.clearSession();
    }

    private void resetSession() {
        clearMeasurementState();
        setProtocolControlsEnabled(true);
        startButton.setEnabled(true);
        startButton.setText("Start session");
        resetButton.setEnabled(false);
        shareButton.setEnabled(false);
        statusText.setText("48 targets, two visits per zone");
    }

    private void saveCompletedReport(ReachReport completedReport) {
        if (!saveHistoryCheck.isChecked()) return;
        SharedPreferences preferences = preferences();
        String updated = ReachHistory.append(
                preferences.getString(HISTORY, ""), System.currentTimeMillis(), completedReport);
        preferences.edit().putString(HISTORY, updated).apply();
        updateHistoryControls();
    }

    private void updateHistoryControls() {
        int count = ReachHistory.count(preferences().getString(HISTORY, ""));
        historyText.setText(count + " saved");
        shareHistoryButton.setEnabled(count > 0);
        clearHistoryButton.setEnabled(count > 0);
    }

    private void shareHistory() {
        String stored = preferences().getString(HISTORY, "");
        if (ReachHistory.count(stored) == 0) return;
        shareJson("ReachGrid local history", ReachHistory.toJson(stored));
    }

    private void confirmClearHistory() {
        if (ReachHistory.count(preferences().getString(HISTORY, "")) == 0) return;
        new AlertDialog.Builder(this)
                .setTitle("Clear local history?")
                .setMessage("This removes saved ReachGrid sessions from this device. Shared copies are unaffected.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    preferences().edit().remove(HISTORY).apply();
                    updateHistoryControls();
                })
                .show();
    }

    private SharedPreferences preferences() {
        return getSharedPreferences(PREFERENCES, MODE_PRIVATE);
    }

    private void shareJson(String subject, String payload) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, payload);
        startActivity(Intent.createChooser(intent, "Share ReachGrid JSON"));
    }

    private void shareReport() {
        if (comparison != null) {
            shareJson("ReachGrid paired comparison " + comparison.firstReport().seed(), comparison.toJson());
            return;
        }
        if (report == null) return;
        shareJson("ReachGrid session " + report.seed(), report.toJson());
    }

    private int dailySeed() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) * 10_000
                + (calendar.get(Calendar.MONTH) + 1) * 100
                + calendar.get(Calendar.DAY_OF_MONTH);
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private TextView text(String value, int sizeSp, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(INK);
        if (bold) text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return text;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
