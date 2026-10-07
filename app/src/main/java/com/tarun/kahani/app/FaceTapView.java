package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * Shows a character picture; the user taps the mouth, then the left eye, then the right eye.
 * Points are kept as fractions of the picture so lip-sync and blinking line up exactly.
 */
final class FaceTapView extends View {
    interface Listener { void changed(int step); }

    Bitmap bmp;
    final float[] pts = new float[6]; // mouth x,y, left eye x,y, right eye x,y (fractions)
    int step; // next point to set: 0 mouth, 1 left eye, 2 right eye, 3 done
    float zoom = 1f, panY;
    Listener listener;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF dst = new RectF();

    FaceTapView(Context c) { super(c); }

    void set(Bitmap b, float[] initial) {
        bmp = b;
        if (initial != null) System.arraycopy(initial, 0, pts, 0, 6);
        step = initial != null && initial[0] > 0 ? 3 : 0;
        // start zoomed on the head (top part of the picture)
        zoom = 2.2f;
        panY = 0;
        invalidate();
    }

    private void layoutDst() {
        if (bmp == null) return;
        float vw = getWidth(), vh = getHeight();
        float s = Math.min(vw / bmp.getWidth(), vh / bmp.getHeight()) * zoom;
        float w = bmp.getWidth() * s, h = bmp.getHeight() * s;
        float headY = (step == 3 || pts[1] == 0 ? 0.2f : pts[1]) * h;
        float top = vh * 0.35f - headY + panY;
        if (top > 0) top = 0;
        if (top + h < vh) top = Math.min(0, vh - h);
        dst.set((vw - w) / 2, top, (vw + w) / 2, top + h);
    }

    protected void onDraw(Canvas c) {
        c.drawColor(0xFF37474F);
        if (bmp == null || bmp.isRecycled()) return;
        layoutDst();
        c.drawBitmap(bmp, null, dst, p);
        String[] lbl = {"मुँह", "बाईं आँख", "दाईं आँख"};
        int[] col = {0xFFFF1744, 0xFF2979FF, 0xFF2979FF};
        for (int i = 0; i < 3; i++) {
            if (pts[i * 2] <= 0) continue;
            float x = dst.left + pts[i * 2] * dst.width(), y = dst.top + pts[i * 2 + 1] * dst.height();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(getContext(), 3));
            p.setColor(col[i]);
            c.drawCircle(x, y, Ui.dp(getContext(), 14), p);
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(Ui.dp(getContext(), 14));
            c.drawText(lbl[i], x + Ui.dp(getContext(), 16), y, p);
        }
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (bmp == null) return false;
        if (e.getAction() == MotionEvent.ACTION_UP && step < 3) {
            layoutDst();
            float fx = (e.getX() - dst.left) / dst.width(), fy = (e.getY() - dst.top) / dst.height();
            if (fx < 0 || fx > 1 || fy < 0 || fy > 1) return true;
            pts[step * 2] = fx;
            pts[step * 2 + 1] = fy;
            step++;
            invalidate();
            if (listener != null) listener.changed(step);
            performClick();
        }
        return true;
    }

    public boolean performClick() { return super.performClick(); }

    void restart() {
        for (int i = 0; i < 6; i++) pts[i] = 0;
        step = 0;
        invalidate();
        if (listener != null) listener.changed(step);
    }
}
