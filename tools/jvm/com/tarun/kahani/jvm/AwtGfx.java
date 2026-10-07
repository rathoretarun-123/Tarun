package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Gfx;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Desktop implementation of Gfx so the same drawing code can be rendered and inspected off-device. */
public class AwtGfx implements Gfx {
    private final BufferedImage img;
    private Graphics2D g;
    private Path2D.Float path = new Path2D.Float();
    private Paint paint = Color.BLACK;
    private float alpha = 1f;
    private final ArrayDeque<Object[]> stack = new ArrayDeque<Object[]>();
    private final Map<String, BufferedImage> layers = new LinkedHashMap<String, BufferedImage>() {
        protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) { return size() > 6; }
    };
    private final Map<String, Font> fonts = new HashMap<String, Font>();
    private static Font base;

    public AwtGfx(BufferedImage img) {
        this.img = img;
        this.g = img.createGraphics();
        setup(g);
    }

    static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    public BufferedImage image() { return img; }

    public int width() { return img.getWidth(); }
    public int height() { return img.getHeight(); }

    public void save() { stack.push(new Object[]{g.getTransform(), alpha, paint}); }
    public void restore() {
        Object[] s = stack.pop();
        g.setTransform((AffineTransform) s[0]);
        alpha = (Float) s[1];
        paint = (Paint) s[2];
    }
    public void translate(float x, float y) { g.translate(x, y); }
    public void scale(float sx, float sy) { g.scale(sx, sy); }
    public void rotate(float d) { g.rotate(Math.toRadians(d)); }
    public void setAlpha(float a) { alpha *= Math.max(0, Math.min(1, a)); }

    private void apply() {
        g.setPaint(paint);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    }

    public void color(int argb) { paint = new Color(argb, true); }
    public void linear(float x0, float y0, float x1, float y1, int c0, int c1) {
        if (x0 == x1 && y0 == y1) y1 += 0.01f;
        paint = new GradientPaint(x0, y0, new Color(c0, true), x1, y1, new Color(c1, true));
    }
    public void radial(float cx, float cy, float r, int c0, int c1) {
        paint = new RadialGradientPaint(cx, cy, Math.max(0.01f, r), new float[]{0f, 1f}, new Color[]{new Color(c0, true), new Color(c1, true)});
    }

    public void rect(float x, float y, float w, float h) { apply(); g.fill(new Rectangle2D.Float(x, y, w, h)); }
    public void roundRect(float x, float y, float w, float h, float r) { apply(); g.fill(new RoundRectangle2D.Float(x, y, w, h, r * 2, r * 2)); }
    public void oval(float cx, float cy, float rx, float ry) { apply(); g.fill(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2)); }
    public void line(float x0, float y0, float x1, float y1, float w) {
        apply();
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Float(x0, y0, x1, y1));
    }
    public void strokeOval(float cx, float cy, float rx, float ry, float w) {
        apply();
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Ellipse2D.Float(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    public void begin() { path = new Path2D.Float(); }
    public void moveTo(float x, float y) { path.moveTo(x, y); }
    public void lineTo(float x, float y) { path.lineTo(x, y); }
    public void quadTo(float cx, float cy, float x, float y) { path.quadTo(cx, cy, x, y); }
    public void cubicTo(float a, float b, float c, float d, float x, float y) { path.curveTo(a, b, c, d, x, y); }
    public void close() { path.closePath(); }
    public void fillPath() { apply(); g.fill(path); }
    public void strokePath(float w) {
        apply();
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(path);
    }

    private static boolean hasDevanagari(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= '\u0900' && s.charAt(i) <= '\u097F') return true;
        return false;
    }

    private Font fontFor(String s, float size, boolean bold) {
        if (hasDevanagari(s)) return font(size, bold);
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 1).deriveFont(size);
    }

    private Font font(float size, boolean bold) {
        if (base == null) {
            try {
                base = Font.createFont(Font.TRUETYPE_FONT, new java.io.File("/usr/share/fonts/truetype/noto/NotoSansDevanagari-Bold.ttf"));
            } catch (Exception e) {
                base = new Font("SansSerif", Font.BOLD, 12);
            }
        }
        String k = size + "/" + bold;
        Font f = fonts.get(k);
        if (f == null) { f = base.deriveFont(bold ? Font.BOLD : Font.PLAIN, size); fonts.put(k, f); }
        return f;
    }

    public void text(String s, float x, float y, float size, boolean bold, int align) {
        apply();
        Font f = fontFor(s, size, bold);
        g.setFont(f);
        float w = (float) f.getStringBounds(s, g.getFontRenderContext()).getWidth();
        float dx = align == 1 ? -w / 2 : align == 2 ? -w : 0;
        g.drawString(s, x + dx, y);
    }
    public float textWidth(String s, float size, boolean bold) {
        Font f = fontFor(s, size, bold);
        return (float) f.getStringBounds(s, g.getFontRenderContext()).getWidth();
    }

    public void image(Object im, float x, float y, float w, float h) {
        apply();
        g.drawImage((Image) im, Math.round(x), Math.round(y), Math.round(w), Math.round(h), null);
    }
    public void imageRect(Object im, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh) {
        apply();
        AffineTransform saved = g.getTransform();
        g.translate(dx, dy);
        g.scale(dw / sw, dh / sh);
        g.drawImage((Image) im, 0, 0, Math.round(sw), Math.round(sh), Math.round(sx), Math.round(sy), Math.round(sx + sw), Math.round(sy + sh), null);
        g.setTransform(saved);
    }
    public int imageWidth(Object im) { return ((BufferedImage) im).getWidth(); }
    public int imageHeight(Object im) { return ((BufferedImage) im).getHeight(); }

    public void layer(String key, float w, float h, Painter painter) {
        BufferedImage l = layers.get(key);
        if (l == null) {
            l = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = l.createGraphics();
            setup(lg);
            Graphics2D saved = g;
            ArrayDeque<Object[]> savedStack = new ArrayDeque<Object[]>(stack);
            Paint sp = paint; float sa = alpha;
            g = lg; alpha = 1f;
            lg.scale(img.getWidth() / w, img.getHeight() / h);
            painter.paint(this);
            lg.dispose();
            g = saved; paint = sp; alpha = sa;
            stack.clear(); stack.addAll(savedStack);
            layers.put(key, l);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(l, 0, 0, Math.round(w), Math.round(h), null);
    }

    public void layerLow(String key, float w, float h, float scale, Painter painter) {
        BufferedImage l = layers.get(key);
        if (l == null) {
            int lw = Math.max(8, Math.round(img.getWidth() * scale)), lh = Math.max(8, Math.round(img.getHeight() * scale));
            l = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D lg = l.createGraphics();
            setup(lg);
            Graphics2D saved = g;
            ArrayDeque<Object[]> savedStack = new ArrayDeque<Object[]>(stack);
            Paint sp = paint; float sa = alpha;
            g = lg; alpha = 1f;
            lg.scale(lw / w, lh / h);
            painter.paint(this);
            lg.dispose();
            g = saved; paint = sp; alpha = sa;
            stack.clear(); stack.addAll(savedStack);
            layers.put(key, l);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(l, 0, 0, Math.round(w), Math.round(h), null);
    }

    public void clearLayers() { layers.clear(); }
}
