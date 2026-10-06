package game.sanguo.mobile;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only actual focused Android widget inventory, accompanying normal screenshots. */
final class SessionAUiReadabilityAudit {
    private SessionAUiReadabilityAudit() {}

    static JSONObject collect(View root) throws Exception {
        JSONArray rows = new JSONArray();
        walk(root, rows);
        return new JSONObject().put("scope", "Actual focused shown widgets only; solid drawable color contrast is metadata, not measured screenshot pixels. Gradients/images/Canvas map labels require separate pixel evidence.")
            .put("actualFocusedRoot", root.hasWindowFocus()).put("rows", rows);
    }

    private static void walk(View view, JSONArray rows) throws Exception {
        if (!view.isShown()) return;
        Rect visible = new Rect();
        if (!view.getGlobalVisibleRect(visible) || visible.isEmpty()) return;
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if (text.getText().length() > 0) {
                int foreground = text.getCurrentTextColor();
                Integer background = solidBackground(view);
                JSONObject row = new JSONObject().put("class", view.getClass().getName())
                    .put("text", text.getText()).put("description", view.getContentDescription())
                    .put("enabled", view.isEnabled()).put("selected", view.isSelected())
                    .put("focused", view.isFocused()).put("viewAlpha", view.getAlpha())
                    .put("textArgb", argb(foreground)).put("textAlpha", Color.alpha(foreground))
                    .put("textSizePx", text.getTextSize()).put("visibleBounds", visible.flattenToString());
                if (background != null) {
                    row.put("solidBackgroundArgb", argb(background));
                    if (Color.alpha(foreground) == 255) row.put("solidContrast", contrast(foreground, background));
                } else row.put("backgroundEvidence", "gradient/image/transparent/unresolved; no computed pixel contrast");
                JSONArray spans = new JSONArray();
                if (text.getText() instanceof Spanned) {
                    Spanned value = (Spanned) text.getText();
                    for (ForegroundColorSpan span : value.getSpans(0, value.length(), ForegroundColorSpan.class)) {
                        int color = span.getForegroundColor();
                        JSONObject item = new JSONObject().put("start", value.getSpanStart(span))
                            .put("end", value.getSpanEnd(span)).put("argb", argb(color)).put("alpha", Color.alpha(color));
                        if (background != null && Color.alpha(color) == 255) item.put("solidContrast", contrast(color, background));
                        spans.put(item);
                    }
                }
                row.put("foregroundSpans", spans);
                rows.put(row);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i), rows);
        }
    }

    private static Integer solidBackground(View view) {
        for (View current = view; current != null; current = current.getParent() instanceof View ? (View) current.getParent() : null)
            if (current.getAlpha() != 1f) return null;
        for (View current = view; current != null;) {
            // An alpha layer requires composition; do not report an uncomposited contrast.
            if (current.getAlpha() != 1f) return null;
            Drawable background = current.getBackground();
            if (background != null) {
                Drawable active = background.getCurrent();
                Integer color = null;
                if (active instanceof ColorDrawable) color = ((ColorDrawable) active).getColor();
                else if (active instanceof GradientDrawable && ((GradientDrawable) active).getColors() == null
                    && ((GradientDrawable) active).getColor() != null)
                    color = ((GradientDrawable) active).getColor().getColorForState(current.getDrawableState(), ((GradientDrawable) active).getColor().getDefaultColor());
                if (color != null && Color.alpha(color) == 255 && active.getAlpha() == 255) return color;
                // A partially transparent/image/gradient surface may affect every ancestor color.
                return null;
            }
            current = current.getParent() instanceof View ? (View) current.getParent() : null;
        }
        return null;
    }

    private static String argb(int value) { return String.format(java.util.Locale.ROOT, "%08x", value); }
    private static double linear(int value) {
        double encoded = value / 255.0;
        return encoded <= .04045 ? encoded / 12.92 : Math.pow((encoded + .055) / 1.055, 2.4);
    }
    private static double luminance(int color) {
        return .2126 * linear(Color.red(color)) + .7152 * linear(Color.green(color)) + .0722 * linear(Color.blue(color));
    }
    private static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }
}
