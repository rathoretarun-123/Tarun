package com.tarun.kahani.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.tarun.kahani.core.Gfx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Gfx on android.graphics.Canvas (software bitmap so frames can be encoded). */
public final class AndroidGfx implements Gfx {
    private final Bitmap bmp;
    private Canvas c;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint img = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private Path path = new Path();
    private int color = 0xFF000000;
    private Shader shader;
    private float alpha = 1f;
    private final ArrayList<float[]> alphaStack = new ArrayList<float[]>();
    private final ArrayList<Shader> shaderStack = new ArrayList<Shader>();
    private final ArrayList<Integer> colorStack = new ArrayList<Integer>();
    private final RectF rf = new RectF();
    private final Rect src = new Rect();
    private final Map<String, Bitmap> layers;
    private static final Typeface BOLD = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);

    public AndroidGfx(Bitmap bmp, final int maxLayers) {
        this.bmp = bmp;
        this.c = new Canvas(bmp);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        layers = new LinkedHashMap<String, Bitmap>(8, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, Bitmap> e) {
                if (size() > maxLayers) { e.getValue().recycle(); return true; }
                return false;
            }
        };
    }

    public Bitmap bitmap() { return bmp; }

    public void release() {
        for (Bitmap b : layers.values()) b.recycle();
        layers.clear();
    }

    public int width() { return bmp.getWidth(); }
    public int height() { return bmp.getHeight(); }

    public void save() {
        c.save();
        alphaStack.add(new float[]{alpha});
        shaderStack.add(shader);
        colorStack.add(color);
    }

    public void restore() {
        c.restore();
        int n = alphaStack.size() - 1;
        if (n >= 0) {
            alpha = alphaStack.remove(n)[0];
            shader = shaderStack.remove(n);
            color = colorStack.remove(n);
        }
    }

    public void translate(float x, float y) { c.translate(x, y); }
    public void scale(float sx, float sy) { c.scale(sx, sy); }
    public void rotate(float d) { c.rotate(d); }
    public void setAlpha(float a) { alpha *= Math.max(0, Math.min(1, a)); }

    public void color(int argb) { color = argb; shader = null; }
    public void linear(float x0, float y0, float x1, float y1, int c0, int c1) {
        if (x0 == x1 && y0 == y1) y1 += 0.01f;
        shader = new LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP);
        color = 0xFFFFFFFF;
    }
    public void radial(float cx, float cy, float r, int c0, int c1) {
        shader = new RadialGradient(cx, cy, Math.max(0.01f, r), c0, c1, Shader.TileMode.CLAMP);
        color = 0xFFFFFFFF;
    }

    private Paint f() {
        fill.setShader(shader);
        fill.setColor(shader != null ? 0xFFFFFFFF : color);
        int a = shader != null ? 255 : (color >>> 24);
        fill.setAlpha((int) (a * alpha));
        return fill;
    }

    private Paint s(float w) {
        stroke.setShader(shader);
        stroke.setColor(shader != null ? 0xFFFFFFFF : color);
        int a = shader != null ? 255 : (color >>> 24);
        stroke.setAlpha((int) (a * alpha));
        stroke.setStrokeWidth(w);
        return stroke;
    }

    public void rect(float x, float y, float w, float h) { c.drawRect(x, y, x + w, y + h, f()); }
    public void roundRect(float x, float y, float w, float h, float r) { rf.set(x, y, x + w, y + h); c.drawRoundRect(rf, r, r, f()); }
    public void oval(float cx, float cy, float rx, float ry) { rf.set(cx - rx, cy - ry, cx + rx, cy + ry); c.drawOval(rf, f()); }
    public void line(float x0, float y0, float x1, float y1, float w) { c.drawLine(x0, y0, x1, y1, s(w)); }
    public void strokeOval(float cx, float cy, float rx, float ry, float w) { rf.set(cx - rx, cy - ry, cx + rx, cy + ry); c.drawOval(rf, s(w)); }

    public void begin() { path = new Path(); }
    public void moveTo(float x, float y) { path.moveTo(x, y); }
    public void lineTo(float x, float y) { path.lineTo(x, y); }
    public void quadTo(float cx, float cy, float x, float y) { path.quadTo(cx, cy, x, y); }
    public void cubicTo(float a, float b, float cc, float d, float x, float y) { path.cubicTo(a, b, cc, d, x, y); }
    public void close() { path.close(); }
    public void fillPath() { c.drawPath(path, f()); }
    public void strokePath(float w) { c.drawPath(path, s(w)); }

    public void text(String s, float x, float y, float size, boolean bold, int align) {
        text.setTypeface(bold ? BOLD : Typeface.DEFAULT);
        text.setTextSize(size);
        text.setShader(shader);
        text.setColor(shader != null ? 0xFFFFFFFF : color);
        int a = shader != null ? 255 : (color >>> 24);
        text.setAlpha((int) (a * alpha));
        text.setTextAlign(align == 1 ? Paint.Align.CENTER : align == 2 ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(s, x, y, text);
    }

    public float textWidth(String s, float size, boolean bold) {
        text.setTypeface(bold ? BOLD : Typeface.DEFAULT);
        text.setTextSize(size);
        return text.measureText(s);
    }

    public void image(Object im, float x, float y, float w, float h) {
        Bitmap b = (Bitmap) im;
        if (b == null || b.isRecycled()) return;
        img.setAlpha((int) (255 * alpha));
        rf.set(x, y, x + w, y + h);
        c.drawBitmap(b, null, rf, img);
    }

    public void imageRect(Object im, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) {
        Bitmap b = (Bitmap) im;
        if (b == null || b.isRecycled()) return;
        img.setAlpha((int) (255 * alpha));
        src.set(Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh));
        rf.set(dx, dy, dx + dw, dy + dh);
        c.drawBitmap(b, src, rf, img);
    }

    public int imageWidth(Object im) { return ((Bitmap) im).getWidth(); }
    public int imageHeight(Object im) { return ((Bitmap) im).getHeight(); }

    public void layerLow(String key, float w, float h, float scale, Painter painter) {
        Bitmap l = layers.get(key);
        if (l == null || l.isRecycled()) {
            int lw = Math.max(8, Math.round(bmp.getWidth() * scale)), lh = Math.max(8, Math.round(bmp.getHeight() * scale));
            l = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888);
            Canvas saved = c;
            float sa = alpha;
            Shader ss = shader;
            int sc = color;
            int depth = alphaStack.size();
            c = new Canvas(l);
            alpha = 1f;
            c.scale(lw / w, lh / h);
            painter.paint(this);
            while (alphaStack.size() > depth) restore();
            c = saved; alpha = sa; shader = ss; color = sc;
            layers.put(key, l);
        }
        img.setAlpha((int) (255 * alpha));
        rf.set(0, 0, w, h);
        c.drawBitmap(l, null, rf, img);
    }

    public void layer(String key, float w, float h, Painter painter) {
        Bitmap l = layers.get(key);
        if (l == null || l.isRecycled()) {
            l = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas saved = c;
            float sa = alpha;
            Shader ss = shader;
            int sc = color;
            int depth = alphaStack.size();
            c = new Canvas(l);
            alpha = 1f;
            c.scale(bmp.getWidth() / w, bmp.getHeight() / h);
            painter.paint(this);
            while (alphaStack.size() > depth) restore();
            c = saved; alpha = sa; shader = ss; color = sc;
            layers.put(key, l);
        }
        img.setAlpha((int) (255 * alpha));
        rf.set(0, 0, w, h);
        c.drawBitmap(l, null, rf, img);
    }
}
