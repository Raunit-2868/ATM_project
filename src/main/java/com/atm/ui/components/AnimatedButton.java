package com.atm.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * AnimatedButton - A JButton with smooth hover/press color-transition animation.
 * Renders itself with rounded corners using custom paintComponent.
 */
public class AnimatedButton extends JButton {

    private Color normalColor;
    private Color hoverColor;
    private Color pressColor;
    private Color currentColor;

    public void setColors(Color normal, Color hover) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.pressColor = hover.darker();
        this.currentColor = normal;
        repaint();
    }
    private static final int ARC = 24;
    private static final int ANIM_STEPS = 12;
    private static final int ANIM_DELAY_MS = 8;

    public AnimatedButton(String text, Color normalColor, Color hoverColor) {
        super(text);
        this.normalColor = normalColor;
        this.hoverColor  = hoverColor;
        this.pressColor  = hoverColor.darker();
        this.currentColor = normalColor;

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setForeground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e)  { animateTo(hoverColor); }
            @Override public void mouseExited(MouseEvent e)   { animateTo(normalColor); }
            @Override public void mousePressed(MouseEvent e)  { currentColor = pressColor; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { currentColor = hoverColor;  repaint(); }
        });
    }

    private void animateTo(Color target) {
        final Color start = currentColor;
        final int[] step = {0};
        Timer t = new Timer(ANIM_DELAY_MS, null);
        t.addActionListener(e -> {
            step[0]++;
            float p = Math.min(1f, (float) step[0] / ANIM_STEPS);
            currentColor = blend(start, target, p);
            repaint();
            if (step[0] >= ANIM_STEPS) {
                ((Timer) e.getSource()).stop();
                currentColor = target;
            }
        });
        t.start();
    }

    private static Color blend(Color a, Color b, float t) {
        int r = clamp((int) (a.getRed()   + (b.getRed()   - a.getRed())   * t));
        int g = clamp((int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t));
        int bl= clamp((int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
        return new Color(r, g, bl);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
        g2.dispose();
        super.paintComponent(g);
    }
}
