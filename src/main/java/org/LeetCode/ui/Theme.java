package org.LeetCode.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public final class Theme {
    private Theme() {}

    // Paleta base (oscuro suave)
    public static final Color BG_APP     = new Color(18, 22, 28);
    public static final Color BG_PANEL   = new Color(24, 28, 36);
    public static final Color FG_TEXT    = new Color(230, 235, 245);
    public static final Color FG_MUTED   = new Color(185, 195, 210);

    // Acentos
    public static final Color ACCENT_OK  = new Color(46, 204, 113);
    public static final Color ACCENT_BAD = new Color(231, 76, 60);
    public static final Color ACCENT_INFO= new Color(255, 193, 7); // dorado suave
    public static final Color CHIP_ATK   = new Color(220, 76, 76);
    public static final Color CHIP_DEF   = new Color(76, 132, 220);
    public static final Color CHIP_MISC  = new Color(155, 89, 182);

    // Mapa opcional para color por atributo (puedes ajustar)
    public static final Map<String, Color> ATTR_COLORS = Map.ofEntries(
            Map.entry("FIRE",  new Color(239, 83, 80)),
            Map.entry("WATER", new Color(66, 165, 245)),
            Map.entry("WIND",  new Color(102, 187, 106)),
            Map.entry("EARTH", new Color(141, 110, 99)),
            Map.entry("DARK",  new Color(92, 107, 192)),
            Map.entry("LIGHT", new Color(255, 235, 59)),
            Map.entry("DIVINE",new Color(255, 167, 38))
    );

    public static void applyNimbusTweaks() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        UIManager.put("control", BG_PANEL);
        UIManager.put("nimbusBase", new Color(40, 45, 55));
        UIManager.put("text", FG_TEXT);
        UIManager.put("nimbusLightBackground", BG_PANEL);
        UIManager.put("info", new Color(35, 40, 50));
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusSelectionBackground", new Color(63, 81, 181));
        UIManager.put("ScrollBar.thumb", new Color(60, 66, 78));
        UIManager.put("List.foreground", FG_TEXT);
        UIManager.put("List.background", BG_PANEL);
        UIManager.put("Table.background", BG_PANEL);
    }

    public static void stylePanel(JComponent c) {
        c.setOpaque(true);
        c.setBackground(BG_PANEL);
        c.setForeground(FG_TEXT);
    }

    public static void styleRoot(JFrame f) {
        f.getContentPane().setBackground(BG_APP);
    }

    public static void styleStatus(JLabel l) {
        l.setOpaque(true);
        l.setBackground(new Color(30, 35, 45));
        l.setForeground(FG_TEXT);
        l.setBorder(new EmptyBorder(8, 12, 8, 12));
    }

    public static void styleTitle(JLabel l) {
        l.setForeground(FG_TEXT);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
    }
}
