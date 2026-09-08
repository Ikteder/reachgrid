package dev.ikteder.reachgrid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import dev.ikteder.reachgrid.core.ReachReport;
import dev.ikteder.reachgrid.core.ReachComparison;
import dev.ikteder.reachgrid.core.ReachSession;

public final class ReachGridView extends View {
    public interface Listener {
        void onTap(ReachSession.TapResult result);
    }

    private static final int BACKGROUND = Color.rgb(238, 242, 249);
    private static final int GRID = Color.rgb(187, 198, 218);
    private static final int INK = Color.rgb(26, 43, 73);
    private static final int TARGET = Color.rgb(255, 176, 0);
    private static final int TARGET_RING = Color.rgb(23, 52, 107);
    private static final int MISS = Color.rgb(194, 52, 64);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cellRect = new RectF();
    private ReachSession session;
    private ReachReport report;
    private ReachComparison comparison;
    private Listener listener;
    private float targetRadiusPixels;
    private boolean missFlash;

    public ReachGridView(Context context) {
        super(context);
        initialize();
    }

    public ReachGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        setBackgroundColor(BACKGROUND);
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        updateDescription();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void startSession(ReachSession session, int targetRadiusDp) {
        this.session = session;
        this.report = null;
        this.comparison = null;
        this.targetRadiusPixels = targetRadiusDp * getResources().getDisplayMetrics().density;
        this.missFlash = false;
        updateDescription();
        invalidate();
    }

    public void showReport(ReachReport report) {
        this.report = report;
        this.comparison = null;
        updateDescription();
        invalidate();
    }

    public void showComparison(ReachComparison comparison) {
        this.comparison = comparison;
        this.report = null;
        updateDescription();
        invalidate();
    }

    public void clearSession() {
        session = null;
        report = null;
        comparison = null;
        missFlash = false;
        updateDescription();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;
        drawGrid(canvas);
        if (comparison != null) {
            drawComparison(canvas);
        } else if (report != null) {
            drawHeatmap(canvas);
        } else if (session != null && session.isStarted() && !session.isComplete()) {
            drawTarget(canvas);
        } else {
            drawCenteredMessage(canvas, "Start when your grip feels natural");
        }
    }

    private void drawGrid(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(GRID);
        for (int column = 1; column < ReachSession.COLUMNS; column += 1) {
            float x = column * getWidth() / (float) ReachSession.COLUMNS;
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int row = 1; row < ReachSession.ROWS; row += 1) {
            float y = row * getHeight() / (float) ReachSession.ROWS;
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawTarget(Canvas canvas) {
        ReachSession.Target target = session.currentTarget();
        float centerX = (float) (target.centerX() * getWidth());
        float centerY = (float) (target.centerY() * getHeight());
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(missFlash ? MISS : TARGET);
        canvas.drawCircle(centerX, centerY, targetRadiusPixels, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(TARGET_RING);
        canvas.drawCircle(centerX, centerY, targetRadiusPixels + dp(4), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(13));
        canvas.drawText("TAP", centerX, centerY + dp(4), paint);
    }

    private void drawHeatmap(Canvas canvas) {
        float cellWidth = getWidth() / (float) ReachSession.COLUMNS;
        float cellHeight = getHeight() / (float) ReachSession.ROWS;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachReport.CellSummary summary = report.summary(row, column);
                float left = column * cellWidth;
                float top = row * cellHeight;
                cellRect.set(left + dp(3), top + dp(3), left + cellWidth - dp(3), top + cellHeight - dp(3));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(scoreColor(summary.reachScore));
                canvas.drawRoundRect(cellRect, dp(10), dp(10), paint);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(summary.reachScore < 45 ? Color.WHITE : INK);
                paint.setTextSize(dp(18));
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                canvas.drawText(Integer.toString(summary.reachScore), left + cellWidth / 2, top + cellHeight / 2 - dp(2), paint);
                paint.setTextSize(dp(11));
                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                String detail = summary.medianLatencyMilliseconds + " ms  " + summary.misses + " miss";
                canvas.drawText(detail, left + cellWidth / 2, top + cellHeight / 2 + dp(16), paint);
            }
        }
    }

    private void drawComparison(Canvas canvas) {
        float cellWidth = getWidth() / (float) ReachSession.COLUMNS;
        float cellHeight = getHeight() / (float) ReachSession.ROWS;
        for (int row = 0; row < ReachSession.ROWS; row += 1) {
            for (int column = 0; column < ReachSession.COLUMNS; column += 1) {
                ReachComparison.CellDelta delta = comparison.delta(row, column);
                float left = column * cellWidth;
                float top = row * cellHeight;
                cellRect.set(left + dp(3), top + dp(3), left + cellWidth - dp(3), top + cellHeight - dp(3));
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(deltaColor(delta.reachScoreDelta));
                canvas.drawRoundRect(cellRect, dp(10), dp(10), paint);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Math.abs(delta.reachScoreDelta) >= 12 ? Color.WHITE : INK);
                paint.setTextSize(dp(14));
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                canvas.drawText(signed(delta.reachScoreDelta) + " pts",
                        left + cellWidth / 2, top + cellHeight / 2 - dp(2), paint);
                paint.setTextSize(dp(11));
                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                canvas.drawText(signed(delta.medianLatencyDeltaMilliseconds) + " ms",
                        left + cellWidth / 2, top + cellHeight / 2 + dp(16), paint);
            }
        }
    }

    private void drawCenteredMessage(Canvas canvas, String message) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(INK);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(dp(16));
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || session == null || session.isComplete()) {
            return true;
        }
        double normalizedX = event.getX() / getWidth();
        double normalizedY = event.getY() / getHeight();
        ReachSession.TapResult result = session.tap(
                normalizedX,
                normalizedY,
                targetRadiusPixels / getWidth(),
                targetRadiusPixels / getHeight(),
                SystemClock.elapsedRealtime());
        if (!result.hit) {
            missFlash = true;
            setContentDescription("Miss. Target unchanged.");
            postDelayed(() -> {
                missFlash = false;
                updateDescription();
                invalidate();
            }, 140);
        } else {
            performClick();
            updateDescription();
        }
        if (listener != null) listener.onTap(result);
        invalidate();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateDescription() {
        if (comparison != null) {
            setContentDescription("Paired reach comparison. Each cell shows second-minus-first score and latency changes.");
        } else if (report != null) {
            setContentDescription("Reach heatmap. Each cell shows score, median latency, and misses.");
        } else if (session != null && session.isStarted() && !session.isComplete()) {
            ReachSession.Target target = session.currentTarget();
            setContentDescription(
                    "Target row " + (target.row + 1) + " of " + ReachSession.ROWS
                            + ", column " + (target.column + 1) + " of " + ReachSession.COLUMNS
                            + ", progress " + session.successfulTargets() + " of " + session.totalTargets());
        } else {
            setContentDescription("ReachGrid session area. Start a session to show targets.");
        }
    }

    private int scoreColor(int score) {
        if (score >= 80) return Color.rgb(105, 198, 150);
        if (score >= 60) return Color.rgb(180, 218, 123);
        if (score >= 40) return Color.rgb(250, 196, 92);
        return Color.rgb(194, 52, 64);
    }

    private int deltaColor(int scoreDelta) {
        if (scoreDelta >= 12) return Color.rgb(43, 132, 91);
        if (scoreDelta > 0) return Color.rgb(168, 219, 181);
        if (scoreDelta <= -12) return Color.rgb(174, 54, 67);
        if (scoreDelta < 0) return Color.rgb(244, 174, 153);
        return Color.rgb(218, 224, 235);
    }

    private String signed(long value) {
        return value > 0 ? "+" + value : Long.toString(value);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
