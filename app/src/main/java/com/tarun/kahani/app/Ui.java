package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the simple, large-button interface in code. */
final class Ui {
    static final int BG = 0xFFFFF8EC, PRIMARY = 0xFFE65100, PRIMARY_DARK = 0xFFBF360C, CARD = 0xFFFFFFFF,
            TEXT = 0xFF2B1B10, SUB = 0xFF6D5A4A, GREEN = 0xFF2E7D32, RED = 0xFFC62828, BLUE = 0xFF1565C0;

    private Ui() {}

    static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static GradientDrawable round(int color, float radius, int strokeColor, int strokeW) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeW > 0) d.setStroke(strokeW, strokeColor);
        return d;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        l.setBackground(round(CARD, dp(c, 16), 0x22000000, dp(c, 1)));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        l.setElevation(dp(c, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6));
        l.setLayoutParams(lp);
        return l;
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setLineSpacing(0, 1.12f);
        return t;
    }

    static TextView title(Context c, String s) {
        TextView t = text(c, s, 19, TEXT, true);
        t.setPadding(0, 0, 0, dp(c, 6));
        return t;
    }

    static Button button(Context c, String s, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), round(color, dp(c, 12), 0, 0), null));
        b.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        b.setMinHeight(dp(c, 52));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 5), 0, dp(c, 5));
        b.setLayoutParams(lp);
        return b;
    }

    static Button small(Context c, String s, int color, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(color);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), round(0xFFFFFFFF, dp(c, 10), color, dp(c, 1.5f)), null));
        b.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        b.setMinHeight(dp(c, 42));
        b.setMinimumHeight(dp(c, 42));
        b.setMaxLines(1);
        b.setEllipsize(TextUtils.TruncateAt.END);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(c, 3), dp(c, 3), dp(c, 3), dp(c, 3));
        b.setLayoutParams(lp);
        return b;
    }

    /** A quiet, light full-width button (for something that should not shout, like clearing the story). */
    static Button soft(Context c, String s, int textColor, int fill, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(textColor);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x14000000), round(fill, dp(c, 10), 0x22000000, dp(c, 1)), null));
        b.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        b.setMinHeight(dp(c, 40));
        b.setMinimumHeight(dp(c, 40));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 2));
        b.setLayoutParams(lp);
        return b;
    }

    static View space(Context c, int h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }
}
