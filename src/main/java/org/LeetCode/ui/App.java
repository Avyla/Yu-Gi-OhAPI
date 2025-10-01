package org.LeetCode.ui;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // (Opcional) Look & Feel Nimbus
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            DuelFrame f = new DuelFrame();
            f.setVisible(true);
        });
    }
}
