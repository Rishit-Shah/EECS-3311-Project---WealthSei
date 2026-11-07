package wealthsei.presentation.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Small line icons drawn in code, so there are no image files to ship. */
public final class Icons {

    private Icons() {
    }

    /** Which icon to draw. */
    public enum Kind { DASHBOARD, TRANSACTIONS, GOALS, PROPOSALS, ASSISTANT, REPORTS, SUN, MOON, IMPORT, LOGO, SPARKLE, SEND, CLOSE }

    public static void draw(Graphics2D g0, Kind kind, double x, double y, double s, Color colour) {
        Graphics2D g = (Graphics2D) g0.create();
        Theme.quality(g);
        g.translate(x, y);
        g.scale(s / 24.0, s / 24.0);
        g.setColor(colour);
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case DASHBOARD -> {
                g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 9, 2.2, 2.2));
                g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 5, 2.2, 2.2));
                g.draw(new RoundRectangle2D.Double(13.5, 11.5, 7, 9, 2.2, 2.2));
                g.draw(new RoundRectangle2D.Double(3.5, 15.5, 7, 5, 2.2, 2.2));
            }
            case TRANSACTIONS -> {
                Path2D p = new Path2D.Double();
                p.moveTo(4, 8);
                p.lineTo(18, 8);
                p.moveTo(14, 4);
                p.lineTo(18, 8);
                p.lineTo(14, 12);
                p.moveTo(20, 16);
                p.lineTo(6, 16);
                p.moveTo(10, 12);
                p.lineTo(6, 16);
                p.lineTo(10, 20);
                g.draw(p);
            }
            case GOALS -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Ellipse2D.Double(7.5, 7.5, 9, 9));
                g.fill(new Ellipse2D.Double(10.6, 10.6, 2.8, 2.8));
            }
            case PROPOSALS -> {
                g.draw(new RoundRectangle2D.Double(3.5, 3.5, 17, 17, 5, 5));
                Path2D p = new Path2D.Double();
                p.moveTo(8, 12.4);
                p.lineTo(11, 15.4);
                p.lineTo(16.2, 9);
                g.draw(p);
            }
            case ASSISTANT -> {
                Path2D p = new Path2D.Double();
                p.moveTo(5, 5);
                p.lineTo(19, 5);
                p.quadTo(21, 5, 21, 7);
                p.lineTo(21, 14);
                p.quadTo(21, 16, 19, 16);
                p.lineTo(11, 16);
                p.lineTo(6.5, 20);
                p.lineTo(6.5, 16);
                p.lineTo(5, 16);
                p.quadTo(3, 16, 3, 14);
                p.lineTo(3, 7);
                p.quadTo(3, 5, 5, 5);
                g.draw(p);
                g.fill(new Ellipse2D.Double(7.2, 9.6, 2, 2));
                g.fill(new Ellipse2D.Double(11, 9.6, 2, 2));
                g.fill(new Ellipse2D.Double(14.8, 9.6, 2, 2));
            }
            case REPORTS -> {
                g.draw(new RoundRectangle2D.Double(4, 3.5, 16, 17, 3.5, 3.5));
                Path2D p = new Path2D.Double();
                p.moveTo(8.2, 16);
                p.lineTo(8.2, 12);
                p.moveTo(12, 16);
                p.lineTo(12, 8.5);
                p.moveTo(15.8, 16);
                p.lineTo(15.8, 10.5);
                g.draw(p);
            }
            case SUN -> {
                g.draw(new Ellipse2D.Double(7.5, 7.5, 9, 9));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    Path2D p = new Path2D.Double();
                    p.moveTo(12 + Math.cos(a) * 7.5, 12 + Math.sin(a) * 7.5);
                    p.lineTo(12 + Math.cos(a) * 10, 12 + Math.sin(a) * 10);
                    g.draw(p);
                }
            }
            case MOON -> {
                Path2D p = new Path2D.Double();
                p.moveTo(19.5, 14.5);
                p.curveTo(12, 17.5, 5.5, 11, 8.8, 4);
                p.curveTo(3.5, 5.2, 2.2, 11.6, 5.6, 16.2);
                p.curveTo(9, 20.8, 16.6, 20.6, 19.5, 14.5);
                g.draw(p);
            }
            case IMPORT -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 4);
                p.lineTo(12, 15);
                p.moveTo(7.5, 10.8);
                p.lineTo(12, 15.3);
                p.lineTo(16.5, 10.8);
                p.moveTo(4.5, 19.5);
                p.lineTo(19.5, 19.5);
                g.draw(p);
            }
            case LOGO -> {
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D p = new Path2D.Double();
                p.moveTo(3.5, 8);
                p.lineTo(8, 17);
                p.lineTo(12, 9);
                p.lineTo(16, 17);
                p.lineTo(20.5, 8);
                g.draw(p);
            }
            case SPARKLE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(10, 3);
                p.curveTo(10.6, 8.2, 12, 9.6, 17, 10.4);
                p.curveTo(12, 11.2, 10.6, 12.6, 10, 17.8);
                p.curveTo(9.4, 12.6, 8, 11.2, 3, 10.4);
                p.curveTo(8, 9.6, 9.4, 8.2, 10, 3);
                g.fill(p);
                Path2D q = new Path2D.Double();
                q.moveTo(18.2, 14.4);
                q.curveTo(18.4, 16.4, 18.8, 16.8, 20.8, 17.1);
                q.curveTo(18.8, 17.4, 18.4, 17.8, 18.2, 19.8);
                q.curveTo(18, 17.8, 17.6, 17.4, 15.6, 17.1);
                q.curveTo(17.6, 16.8, 18, 16.4, 18.2, 14.4);
                g.fill(q);
            }
            case SEND -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 19);
                p.lineTo(12, 5.5);
                p.moveTo(6.2, 11.2);
                p.lineTo(12, 5.4);
                p.lineTo(17.8, 11.2);
                g.draw(p);
            }
            case CLOSE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(6.5, 6.5);
                p.lineTo(17.5, 17.5);
                p.moveTo(17.5, 6.5);
                p.lineTo(6.5, 17.5);
                g.draw(p);
            }
            default -> {
            }
        }
        g.dispose();
    }
}
