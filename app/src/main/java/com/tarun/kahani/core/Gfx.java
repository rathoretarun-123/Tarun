package com.tarun.kahani.core;

/**
 * Minimal 2D drawing surface. Implemented with android.graphics.Canvas in the app
 * and with java.awt.Graphics2D in the desktop test harness, so the exact same
 * drawing code is verified on both.
 */
public interface Gfx {
    int width();
    int height();

    void save();
    void restore();
    void translate(float x, float y);
    void scale(float sx, float sy);
    void rotate(float degrees);

    /** Multiplies the alpha of everything drawn until restore(). */
    void setAlpha(float a);

    void color(int argb);
    void linear(float x0, float y0, float x1, float y1, int c0, int c1);
    void radial(float cx, float cy, float r, int c0, int c1);

    void rect(float x, float y, float w, float h);
    void roundRect(float x, float y, float w, float h, float r);
    void oval(float cx, float cy, float rx, float ry);
    void line(float x0, float y0, float x1, float y1, float width);
    void strokeOval(float cx, float cy, float rx, float ry, float width);

    // Path building
    void begin();
    void moveTo(float x, float y);
    void lineTo(float x, float y);
    void quadTo(float cx, float cy, float x, float y);
    void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y);
    void close();
    void fillPath();
    void strokePath(float width);

    /** align: 0 left, 1 centre, 2 right. y is the baseline. */
    void text(String s, float x, float y, float size, boolean bold, int align);
    float textWidth(String s, float size, boolean bold);

    /** Draws an image (opaque handle created by the platform) into the destination rect. */
    void image(Object img, float x, float y, float w, float h);
    /** Draws the source rectangle of an image into the destination rectangle. */
    void imageRect(Object img, float sx, float sy, float sw, float sh, float dx, float dy, float dw, float dh);
    int imageWidth(Object img);
    int imageHeight(Object img);

    /**
     * Draws a cached layer covering the rectangle (0,0,w,h) in current coordinates.
     * If key is not cached, painter is called once to paint it in (0,0,w,h) coordinates;
     * implementations cache it as a bitmap the size of the surface.
     */
    void layer(String key, float w, float h, Painter painter);

    /** Like layer() but cached at a fraction of the resolution and drawn scaled up (a cheap blur, for depth of field). */
    void layerLow(String key, float w, float h, float scale, Painter painter);

    interface Painter { void paint(Gfx g); }
}
