package com.atm.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * RoundedPanel - A JPanel that paints itself with rounded corners.
 * Used as the "card" component throughout the UI.
 */
public class RoundedPanel extends JPanel {

    private final int radius;
    private Color bgColor;

    public RoundedPanel(int radius, Color bgColor) {
        this.radius = radius;
        this.bgColor = bgColor;
        setOpaque(false);
    }

    public RoundedPanel(int radius) {
        this(radius, new Color(18, 18, 38));
    }

    public void setBgColor(Color color) {
        this.bgColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
        g2.dispose();
        super.paintComponent(g);
    }
}
