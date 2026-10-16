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
        this.mainG = g;
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
    /** Each mesh cell is drawn as two triangles, each an affine-mapped, clipped piece of the image. */
    public void imageMesh(Object im, int meshW, int meshH, float[] v) {
        apply();
        BufferedImage bi = (BufferedImage) im;
        if (g.getClip() == null && rasterMesh(bi, meshW, meshH, v)) return;
        float cw = bi.getWidth() / (float) meshW, ch = bi.getHeight() / (float) meshH;
        AffineTransform saved = g.getTransform();
        Shape savedClip = g.getClip();
        for (int j = 0; j < meshH; j++) {
            for (int i = 0; i < meshW; i++) {
                int a = (j * (meshW + 1) + i) * 2, b = a + 2, c = a + (meshW + 1) * 2, d = c + 2;
                float sx = i * cw, sy = j * ch;
                tri(bi, sx, sy, sx + cw, sy, sx, sy + ch, v[a], v[a + 1], v[b], v[b + 1], v[c], v[c + 1], saved, savedClip);
                tri(bi, sx + cw, sy, sx + cw, sy + ch, sx, sy + ch, v[b], v[b + 1], v[d], v[d + 1], v[c], v[c + 1], saved, savedClip);
            }
        }
        g.setTransform(saved);
        g.setClip(savedClip);
    }

    // ---------------------------------------------------------------- per-pixel mesh rasterizer

    /** The picture each Graphics draws into (the frame, or a layer while it is being painted). */
    private BufferedImage target() { return g == mainG ? img : layerTarget; }
    private Graphics2D mainG;
    private BufferedImage layerTarget;
    private static final java.util.Map<BufferedImage, int[]> SRC = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<BufferedImage, int[]>());

    private static int[] pixels(BufferedImage bi) {
        int[] p = SRC.get(bi);
        if (p == null) { p = bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, bi.getWidth()); SRC.put(bi, p); }
        return p;
    }

    /**
     * Draws the mesh pixel by pixel, like a graphics card: every destination pixel inside a triangle takes the
     * picture's colour at the exactly interpolated source point (bilinear), blended over what is there. Each
     * pixel belongs to exactly one triangle (top-left rule), so fine meshes and soft edges show no seams.
     */
    private boolean rasterMesh(BufferedImage bi, int meshW, int meshH, float[] v) {
        BufferedImage dst = target();
        if (dst == null) return false;
        java.awt.image.DataBuffer db = dst.getRaster().getDataBuffer();
        int type = dst.getType();
        byte[] bytes = null; int[] ints = null;
        if (type == BufferedImage.TYPE_3BYTE_BGR && db instanceof java.awt.image.DataBufferByte) bytes = ((java.awt.image.DataBufferByte) db).getData();
        else if ((type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB) && db instanceof java.awt.image.DataBufferInt) ints = ((java.awt.image.DataBufferInt) db).getData();
        else return false;
        boolean dstAlpha = type == BufferedImage.TYPE_INT_ARGB;
        int[] src = pixels(bi);
        int sw = bi.getWidth(), sh = bi.getHeight(), dw = dst.getWidth(), dh = dst.getHeight();
        AffineTransform T = g.getTransform();
        int n = (meshW + 1) * (meshH + 1);
        float[] d = new float[n * 2];
        T.transform(v, 0, d, 0, n);
        float cw = sw / (float) meshW, ch = sh / (float) meshH;
        int ga = Math.round(alpha * 256);
        for (int j = 0; j < meshH; j++) {
            for (int i = 0; i < meshW; i++) {
                int a = j * (meshW + 1) + i, b = a + 1, c = a + meshW + 1, e = c + 1;
                float sx = i * cw, sy = j * ch;
                triangle(d, a, b, c, sx, sy, sx + cw, sy, sx, sy + ch, src, sw, sh, bytes, ints, dstAlpha, dw, dh, ga);
                triangle(d, b, e, c, sx + cw, sy, sx + cw, sy + ch, sx, sy + ch, src, sw, sh, bytes, ints, dstAlpha, dw, dh, ga);
            }
        }
        return true;
    }

    private static void triangle(float[] d, int i0, int i1, int i2, float u0, float v0, float u1, float v1, float u2, float v2,
                                 int[] src, int sw, int sh, byte[] bytes, int[] ints, boolean dstAlpha, int dw, int dh, int ga) {
        float x0 = d[i0 * 2], y0 = d[i0 * 2 + 1], x1 = d[i1 * 2], y1 = d[i1 * 2 + 1], x2 = d[i2 * 2], y2 = d[i2 * 2 + 1];
        float area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        if (Math.abs(area) < 1e-6f) return;
        if (area < 0) {     // make it counter-clockwise in screen terms
            float tx = x1, ty = y1, tu = u1, tv = v1;
            x1 = x2; y1 = y2; u1 = u2; v1 = v2;
            x2 = tx; y2 = ty; u2 = tu; v2 = tv;
            area = -area;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)))), maxX = Math.min(dw - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)))), maxY = Math.min(dh - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        if (minX > maxX || minY > maxY) return;
        // edge functions w_k(p) = (b - a) x (p - a); top-left rule through a tiny bias on the other edges
        float b0 = topLeft(x1, y1, x2, y2) ? 0 : -1e-5f, b1 = topLeft(x2, y2, x0, y0) ? 0 : -1e-5f, b2 = topLeft(x0, y0, x1, y1) ? 0 : -1e-5f;
        float inv = 1f / area;
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = (x2 - x1) * (py - y1) - (y2 - y1) * (px - x1);
                float w1 = (x0 - x2) * (py - y2) - (y0 - y2) * (px - x2);
                float w2 = (x1 - x0) * (py - y0) - (y1 - y0) * (px - x0);
                if (w0 + b0 < 0 || w1 + b1 < 0 || w2 + b2 < 0) continue;
                w0 *= inv; w1 *= inv; w2 *= inv;
                float u = w0 * u0 + w1 * u1 + w2 * u2 - 0.5f, vv = w0 * v0 + w1 * v1 + w2 * v2 - 0.5f;
                // bilinear sample (non-premultiplied ARGB, weighted by alpha so edges stay clean)
                int ix = (int) Math.floor(u), iy = (int) Math.floor(vv);
                float fx = u - ix, fy = vv - iy;
                int xa = Math.max(0, Math.min(sw - 1, ix)), xb = Math.max(0, Math.min(sw - 1, ix + 1));
                int ya = Math.max(0, Math.min(sh - 1, iy)), yb = Math.max(0, Math.min(sh - 1, iy + 1));
                int c00 = src[ya * sw + xa], c10 = src[ya * sw + xb], c01 = src[yb * sw + xa], c11 = src[yb * sw + xb];
                float k00 = (1 - fx) * (1 - fy) * (c00 >>> 24), k10 = fx * (1 - fy) * (c10 >>> 24), k01 = (1 - fx) * fy * (c01 >>> 24), k11 = fx * fy * (c11 >>> 24);
                float al = k00 + k10 + k01 + k11;
                if (al < 0.5f) continue;
                float r = (k00 * ((c00 >> 16) & 255) + k10 * ((c10 >> 16) & 255) + k01 * ((c01 >> 16) & 255) + k11 * ((c11 >> 16) & 255)) / al;
                float gg = (k00 * ((c00 >> 8) & 255) + k10 * ((c10 >> 8) & 255) + k01 * ((c01 >> 8) & 255) + k11 * ((c11 >> 8) & 255)) / al;
                float bb = (k00 * (c00 & 255) + k10 * (c10 & 255) + k01 * (c01 & 255) + k11 * (c11 & 255)) / al;
                int A = (int) (al * ga) >> 8;          // 0..255
                if (A <= 0) continue;
                if (A > 255) A = 255;
                int k = y * dw + x;
                if (bytes != null) {
                    int o = k * 3;
                    int db0 = bytes[o] & 255, dg = bytes[o + 1] & 255, dr = bytes[o + 2] & 255;
                    bytes[o] = (byte) (db0 + ((int) bb - db0) * A / 255);
                    bytes[o + 1] = (byte) (dg + ((int) gg - dg) * A / 255);
                    bytes[o + 2] = (byte) (dr + ((int) r - dr) * A / 255);
                } else {
                    int dc = ints[k];
                    int da = dstAlpha ? dc >>> 24 : 255, dr = (dc >> 16) & 255, dg = (dc >> 8) & 255, dbb = dc & 255;
                    int oa = A + da * (255 - A) / 255;
                    int nr, ng, nb;
                    if (oa == 0) { nr = ng = nb = 0; }
                    else {
                        nr = ((int) r * A + dr * da * (255 - A) / 255) / oa;
                        ng = ((int) gg * A + dg * da * (255 - A) / 255) / oa;
                        nb = ((int) bb * A + dbb * da * (255 - A) / 255) / oa;
                    }
                    ints[k] = ((dstAlpha ? oa : 255) << 24) | (Math.min(255, nr) << 16) | (Math.min(255, ng) << 8) | Math.min(255, nb);
                }
            }
        }
    }

    /** A top edge (horizontal, going left in a counter-clockwise triangle) or a left edge (going down). */
    private static boolean topLeft(float ax, float ay, float bx, float by) {
        float ex = bx - ax, ey = by - ay;
        return (ey == 0 && ex < 0) || ey > 0;
    }

    private void tri(BufferedImage bi, float s0x, float s0y, float s1x, float s1y, float s2x, float s2y,
                     float d0x, float d0y, float d1x, float d1y, float d2x, float d2y, AffineTransform base, Shape clip) {
        // affine map source triangle -> destination triangle
        double ux = s1x - s0x, uy = s1y - s0y, vx = s2x - s0x, vy = s2y - s0y;
        double det = ux * vy - uy * vx;
        if (Math.abs(det) < 1e-9) return;
        double px = d1x - d0x, py = d1y - d0y, qx = d2x - d0x, qy = d2y - d0y;
        double m00 = (px * vy - qx * uy) / det, m01 = (qx * ux - px * vx) / det;
        double m10 = (py * vy - qy * uy) / det, m11 = (qy * ux - py * vx) / det;
        double tx = d0x - m00 * s0x - m01 * s0y, ty = d0y - m10 * s0x - m11 * s0y;
        // clip to the destination triangle, grown slightly so neighbours overlap without gaps
        float cx = (d0x + d1x + d2x) / 3, cy = (d0y + d1y + d2y) / 3;
        Path2D.Float t = new Path2D.Float();
        t.moveTo(grow(d0x, cx), grow(d0y, cy));
        t.lineTo(grow(d1x, cx), grow(d1y, cy));
        t.lineTo(grow(d2x, cx), grow(d2y, cy));
        t.closePath();
        g.setTransform(base);
        g.setClip(clip);
        g.clip(t);
        g.transform(new AffineTransform(m00, m10, m01, m11, tx, ty));
        int x0 = (int) Math.floor(Math.min(s0x, Math.min(s1x, s2x))) - 1, y0 = (int) Math.floor(Math.min(s0y, Math.min(s1y, s2y))) - 1;
        int x1 = (int) Math.ceil(Math.max(s0x, Math.max(s1x, s2x))) + 1, y1 = (int) Math.ceil(Math.max(s0y, Math.max(s1y, s2y))) + 1;
        x0 = Math.max(0, x0); y0 = Math.max(0, y0); x1 = Math.min(bi.getWidth(), x1); y1 = Math.min(bi.getHeight(), y1);
        g.drawImage(bi, x0, y0, x1, y1, x0, y0, x1, y1, null);
    }

    private static float grow(float p, float c) { return p + (p - c) * 0.04f + Math.signum(p - c) * 0.4f; }

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
            BufferedImage savedT = layerTarget; layerTarget = l;
            lg.scale(img.getWidth() / w, img.getHeight() / h);
            painter.paint(this);
            lg.dispose();
            g = saved; paint = sp; alpha = sa; layerTarget = savedT;
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
            BufferedImage savedT = layerTarget; layerTarget = l;
            lg.scale(lw / w, lh / h);
            painter.paint(this);
            lg.dispose();
            g = saved; paint = sp; alpha = sa; layerTarget = savedT;
            stack.clear(); stack.addAll(savedStack);
            int[] px = l.getRGB(0, 0, lw, lh, null, 0, lw);
            com.tarun.kahani.core.Blur.gauss(px, lw, lh, Math.max(1, Math.round(lw * 0.005f)));
            l.setRGB(0, 0, lw, lh, px, 0, lw);
            layers.put(key, l);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(l, 0, 0, Math.round(w), Math.round(h), null);
    }

    public void clearLayers() { layers.clear(); }
}
