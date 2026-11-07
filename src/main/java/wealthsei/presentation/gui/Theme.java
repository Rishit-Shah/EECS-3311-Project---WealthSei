package wealthsei.presentation.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import wealthsei.domain.Category;
import wealthsei.domain.Money;

/** Colours, fonts and number formats shared by every screen. Dark by default, with a light mode. */
public final class Theme {

    /** One full set of colours. */
    public record Palette(Color background, Color sidebar, Color surface, Color surfaceRaised, Color border,
                          Color text, Color muted, Color accent, Color accentSoft, Color good, Color warn,
                          Color danger, Color info, Color gridLine) {
    }

    /** Cards stay dark in both modes (as in the design); only the page behind them changes. */
    private static final Palette CARDS = new Palette(
            new Color(0x0C0D0D), new Color(0x0F1111), new Color(0x161918), new Color(0x1F2423), new Color(0x262C2A),
            new Color(0xF4F1EA), new Color(0x8E9692), new Color(0xFF8F3F), new Color(0x3A2616),
            new Color(0x3EE0B5), new Color(0xFFC247), new Color(0xFF5C7A), new Color(0x5B9BFF), new Color(0x232827));

    private static boolean dark = true;
    private static String family;

    private Theme() {
    }

    /** Colours for anything drawn on a card. */
    public static Palette p() {
        return CARDS;
    }

    // ---- the page behind the cards ----

    public static Color pageText() {
        return dark ? new Color(0xF5F2EC) : new Color(0x151515);
    }

    public static Color pageMuted() {
        return dark ? new Color(0x8E9692) : new Color(0x6B645B);
    }

    public static Color navBackground() {
        return dark ? new Color(0x0F1111) : new Color(0xFCF8F1);
    }

    public static Color navBorder() {
        return dark ? new Color(0x1F2423) : new Color(0xE9E0D3);
    }

    /** The selected tab in the top bar. */
    public static Color tabSelected() {
        return dark ? new Color(0x222726) : new Color(0x151515);
    }

    public static Color tabSelectedText() {
        return new Color(0xF4F1EA);
    }

    public static Color tabText() {
        return dark ? new Color(0x9AA29E) : new Color(0x5C554C);
    }

    public static Color pageButton() {
        return dark ? new Color(0x161918) : new Color(0xFFFFFF);
    }

    public static Color pageButtonBorder() {
        return dark ? new Color(0x262C2A) : new Color(0xE6DDCF);
    }

    /** Dark text for use on the orange parts. */
    public static final Color ON_ORANGE = new Color(0x1A1208);

    /** Paints the page background: flat colour with a soft warm glow at the top right. */
    public static void paintPage(Graphics2D g, int w, int h) {
        g.setColor(dark ? new Color(0x0B0C0C) : new Color(0xF4EEE4));
        g.fillRect(0, 0, w, h);
        Color glow = dark ? new Color(0x5A3516) : new Color(0xFFC99B);
        int a = dark ? 120 : 150;
        float r = Math.max(w, h) * 0.62f;
        g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Float(w * 0.93f, h * 0.02f), r,
                new float[] {0f, 1f}, new Color[] {alpha(glow, a), alpha(glow, 0)}));
        g.fillRect(0, 0, w, h);
    }

    /** The font with letters pulled together (negative) or spread out (positive), as a fraction of the size. */
    public static Font tracked(Font f, float tracking) {
        return f.deriveFont(java.util.Map.of(java.awt.font.TextAttribute.TRACKING, tracking));
    }

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean value) {
        dark = value;
    }

    // ---- fonts ----

    private static String family() {
        if (family == null) {
            Set<String> have = new HashSet<>(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames()));
            family = Font.SANS_SERIF;
            for (String name : new String[] {"Inter", "SF Pro Text", ".AppleSystemUIFont", "Helvetica Neue", "Segoe UI",
                "Roboto", "DejaVu Sans"}) {
                if (have.contains(name)) {
                    family = name;
                    break;
                }
            }
        }
        return family;
    }

    public static Font font(int style, float size) {
        return new Font(family(), style, Math.round(size)).deriveFont(style, size);
    }

    public static Font regular(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font(Font.BOLD, size);
    }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    // ---- numbers ----

    /** $1,234.50 */
    public static String money(Money m) {
        DecimalFormat f = new DecimalFormat("#,##0.00");
        f.setRoundingMode(RoundingMode.HALF_UP);
        String s = f.format(m.amount().abs());
        return (m.isNegative() ? "-$" : "$") + s;
    }

    /** $1,235 (no cents) */
    public static String moneyWhole(Money m) {
        DecimalFormat f = new DecimalFormat("#,##0");
        f.setRoundingMode(RoundingMode.HALF_UP);
        return (m.isNegative() ? "-$" : "$") + f.format(m.amount().abs());
    }

    /** $1.2k for chart axes. */
    public static String compact(double value) {
        double a = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (a >= 1000) {
            double k = a / 1000;
            return sign + "$" + (k >= 10 ? String.valueOf(Math.round(k)) : String.format("%.1f", k)) + "k";
        }
        return sign + "$" + Math.round(a);
    }

    // ---- colours ----

    private static final Map<String, Color> CATEGORY_COLOURS = Map.ofEntries(
            Map.entry("groceries", new Color(0x3EE0B5)), Map.entry("dining", new Color(0xFF8F3F)),
            Map.entry("transport", new Color(0x5B9BFF)), Map.entry("rent", new Color(0xA48BFF)),
            Map.entry("utilities", new Color(0x35C9DC)), Map.entry("subscriptions", new Color(0xF472B6)),
            Map.entry("shopping", new Color(0xFF6B8B)), Map.entry("entertainment", new Color(0xB48CFF)),
            Map.entry("health", new Color(0x9BE564)), Map.entry("income", new Color(0x3EE0B5)));

    public static Color categoryColour(Category c) {
        return CATEGORY_COLOURS.getOrDefault(c.id(), new Color(0x7B8794));
    }

    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color mix(Color a, Color b, double t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
