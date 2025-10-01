package org.LeetCode.ui;

import org.LeetCode.model.Card;

import javax.swing.*;
import java.awt.*;

public class StatsPanel extends JPanel {
    private final ChipLabel atk = new ChipLabel("ATK 0", Theme.CHIP_ATK);
    private final ChipLabel def = new ChipLabel("DEF 0", Theme.CHIP_DEF);
    private final ChipLabel misc = new ChipLabel("LV 0", Theme.CHIP_MISC);

    public StatsPanel() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 0));
        add(atk);
        add(def);
        add(misc);
    }

    public void updateFor(Card c) {
        int atkVal = c.atk != null ? c.atk : 0;
        int defVal = c.def != null ? c.def : 0;
        atk.setText("ATK " + atkVal);
        def.setText("DEF " + defVal);

        if (c.linkval != null) {
            misc.setText("LINK " + c.linkval);
        } else {
            misc.setText("LV " + (c.level != null ? c.level : 0));
        }
        repaint();
    }
}
