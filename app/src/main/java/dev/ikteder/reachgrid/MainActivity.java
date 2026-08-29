package dev.ikteder.reachgrid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

import dev.ikteder.reachgrid.core.ReachReport;
import dev.ikteder.reachgrid.core.ReachSession;

public final class MainActivity extends Activity {
    private static final int PAGE_BACKGROUND = Color.rgb(244, 246, 250);
    private static final int INK = Color.rgb(26, 43, 73);

    private Spinner handSpinner;
    private Spinner sizeSpinner;
    private TextView statusText;
    private Button startButton;
    private Button resetButton;
    private Button shareButton;
    private ReachGridView gridView;
    private ReachSession session;
    private ReachReport report;
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

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        settings.setGravity(Gravity.CENTER_VERTICAL);
        handSpinner = spinner(new String[]{"Right hand", "Left hand"});
        sizeSpinner = spinner(new String[]{"32 dp radius", "40 dp radius", "24 dp radius"});
        settings.addView(handSpinner, weighted());
        settings.addView(sizeSpinner, weighted());
        page.addView(settings, matchWrap());

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
        startButton = button("Start session", view -> startSession());
        resetButton = button("Reset", view -> resetSession());
        shareButton = button("Share JSON", view -> shareReport());
        resetButton.setEnabled(false);
        shareButton.setEnabled(false);
        actions.addView(startButton, weighted());
        actions.addView(resetButton, weighted());
        actions.addView(shareButton, weighted());
        page.addView(actions, matchWrap());

        setContentView(page);
    }

    private void startSession() {
        activeHand = handSpinner.getSelectedItemPosition() == 0 ? "right" : "left";
        int[] radii = {32, 40, 24};
        activeTargetRadiusDp = radii[sizeSpinner.getSelectedItemPosition()];
        session = new ReachSession(dailySeed());
        session.start(SystemClock.elapsedRealtime());
        report = null;
        handSpinner.setEnabled(false);
        sizeSpinner.setEnabled(false);
        startButton.setEnabled(false);
        resetButton.setEnabled(true);
        shareButton.setEnabled(false);
        statusText.setText("Target 1 of " + session.totalTargets() + ". Misses do not advance.");
        gridView.startSession(session, activeTargetRadiusDp);
    }

    private void handleTap(ReachSession.TapResult result) {
        if (result.complete) {
            report = new ReachReport(session, activeHand, activeTargetRadiusDp);
            gridView.showReport(report);
            statusText.setText("Complete. Scores combine latency and misses; raw evidence is in the JSON report.");
            shareButton.setEnabled(true);
            startButton.setEnabled(true);
            startButton.setText("Run again");
            return;
        }
        if (result.hit) {
            statusText.setText("Target " + (result.successfulTargets + 1) + " of " + session.totalTargets());
        } else {
            statusText.setText("Miss " + result.missesOnCurrentTarget + " on this target. Try the same zone again.");
        }
    }

    private void resetSession() {
        session = null;
        report = null;
        handSpinner.setEnabled(true);
        sizeSpinner.setEnabled(true);
        startButton.setEnabled(true);
        startButton.setText("Start session");
        resetButton.setEnabled(false);
        shareButton.setEnabled(false);
        statusText.setText("48 targets, two visits per zone");
        gridView.clearSession();
    }

    private void shareReport() {
        if (report == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_SUBJECT, "ReachGrid session " + session.seed());
        intent.putExtra(Intent.EXTRA_TEXT, report.toJson());
        startActivity(Intent.createChooser(intent, "Share ReachGrid JSON"));
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
