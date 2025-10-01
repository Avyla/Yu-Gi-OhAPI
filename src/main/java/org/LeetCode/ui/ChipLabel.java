package org.LeetCode.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChipLabel extends JLabel {
    private Color chipColor = Theme.CHIP_MISC;

    public ChipLabel(String text, Color color) {
        super(text, SwingConstants.CENTER);
        this.chipColor = color;
        setOpaque(false);
        setForeground(Color.WHITE);
        setBorder(new EmptyBorder(2, 10, 2, 10));
        setFont(getFont().deriveFont(Font.BOLD, 12f));
    }

    public void setChipColor(Color c) { this.chipColor = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 215));
            g2.fillRoundRect(0, 0, w, h, h, h);
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
