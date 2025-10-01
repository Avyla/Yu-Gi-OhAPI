package org.LeetCode.ui;

import org.LeetCode.api.YgoApiClient;
import org.LeetCode.game.Duel;
import org.LeetCode.game.Player;
import org.LeetCode.game.RoundResult;
import org.LeetCode.game.Winner;
import org.LeetCode.model.Card;
import org.LeetCode.util.ImageCache;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.List;

public class DuelFrame extends JFrame {

    // --- Tamaños fijos ---
    private static final int BUTTON_W = 160;   // miniatura
    private static final int BUTTON_H = 230;
    private static final int PREVIEW_W = 360;  // carta grande
    private static final int PREVIEW_H = 520;

    // Servicios / estado
    private final YgoApiClient api = new YgoApiClient();
    private final ImageCache imageCache = new ImageCache();
    private Duel duel;
    private Player human;
    private Player cpu;
    private int roundCounter = 1;

    // UI raíz
    private final JButton btnNewDuel = new JButton("Nuevo duelo");
    private final JLabel lblScore = new JLabel("Marcador: Jugador 0 - 0 CPU");
    private final JLabel lblStatus = new JLabel("Listo.");

    // Mano jugador (columna con scroll)
    private final JPanel panelHand = new JPanel();
    private final JScrollPane handScroll = new JScrollPane(panelHand);

    // Arena: dos cartas grandes + “VS”
    private final JLabel lblPlayerCard = new JLabel();
    private final JLabel lblCpuCard = new JLabel();
    private final JLabel lblVs = new JLabel("VS", SwingConstants.CENTER);

    // Historial de rondas (derecha)
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    // Bordes (para resaltar ganador)
    private final TitledBorder tBorderPlayer = BorderFactory.createTitledBorder("Jugador");
    private final TitledBorder tBorderCpu = BorderFactory.createTitledBorder("CPU");

    // Overlay de banner (glass pane)
    private final RoundBannerOverlay banner = new RoundBannerOverlay();

    private boolean inputLocked = false;

    public DuelFrame() {
        super("Mini Duelo Yu-Gi-Oh! (best-of-3)");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ---------- TOP ----------
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(btnNewDuel, BorderLayout.WEST);
        lblScore.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(lblScore, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---------- WEST: Mano del jugador (columna con tamaños fijos) ----------
        panelHand.setLayout(new BoxLayout(panelHand, BoxLayout.Y_AXIS));
        panelHand.setBorder(BorderFactory.createTitledBorder("Tu mano"));
        handScroll.setBorder(null);
        handScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        handScroll.setPreferredSize(new Dimension(BUTTON_W + 40, 0));
        add(handScroll, BorderLayout.WEST);

        // ---------- CENTER: Arena fija ----------
        JPanel arena = new JPanel(new GridBagLayout());
        add(arena, BorderLayout.CENTER);

        // Labels de cartas con tamaño fijo
        fixSize(lblPlayerCard, PREVIEW_W, PREVIEW_H);
        fixSize(lblCpuCard, PREVIEW_W, PREVIEW_H);
        lblPlayerCard.setHorizontalAlignment(SwingConstants.CENTER);
        lblCpuCard.setHorizontalAlignment(SwingConstants.CENTER);
        resetCardBorders();

        // “VS” fijo
        lblVs.setFont(lblVs.getFont().deriveFont(Font.BOLD, 36f));
        lblVs.setForeground(new Color(90, 90, 100));
        fixSize(lblVs, 60, PREVIEW_H);

        // Posicionar: [Jugador] [VS] [CPU]
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0; gc.insets = new Insets(5, 5, 5, 5);
        gc.gridx = 0; arena.add(lblPlayerCard, gc);
        gc.gridx = 1; arena.add(lblVs, gc);
        gc.gridx = 2; arena.add(lblCpuCard, gc);

        // ---------- EAST: Historial ----------
        JPanel east = new JPanel(new BorderLayout(6, 6));
        JLabel lblHist = new JLabel("Historial de rondas", SwingConstants.CENTER);
        east.add(lblHist, BorderLayout.NORTH);
        historyList.setVisibleRowCount(14);
        historyList.setFont(historyList.getFont().deriveFont(Font.PLAIN, 12f));
        east.add(new JScrollPane(historyList), BorderLayout.CENTER);
        east.setPreferredSize(new Dimension(300, 0));
        add(east, BorderLayout.EAST);

        // ---------- SOUTH: Status ----------
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(lblStatus, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Acción: Nuevo duelo
        btnNewDuel.addActionListener(e -> startNewDuel());

        // GlassPane para overlay del banner
        setGlassPane(banner);
        banner.setVisible(false); // se muestra solo cuando hay mensaje

        setMinimumSize(new Dimension(1220, 760));
        setLocationRelativeTo(null);
        // setResizable(false); // opcional
        startNewDuel();
    }

    /** Tamaños fijos para un componente (mín/preferida/máx) */
    private static void fixSize(JComponent c, int w, int h) {
        Dimension d = new Dimension(w, h);
        c.setMinimumSize(d);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
    }

    /** Inicia/reinicia un duelo: reparte y arma la mano en background. */
    private void startNewDuel() {
        if (inputLocked) return;
        lockInput(true);
        setStatus("Repartiendo cartas...");
        historyModel.clear();
        roundCounter = 1;

        lblPlayerCard.setIcon(null);
        lblPlayerCard.setText("");
        setCpuBack();

        panelHand.removeAll();
        panelHand.add(space(0,8));
        panelHand.add(new JLabel("Cargando..."));
        panelHand.add(space(0,8));
        panelHand.revalidate();
        panelHand.repaint();

        resetCardBorders();

        SwingWorker<List<Card>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                return api.fetchMonstersFast(6, 120, 0, 500, 1000, 1500);
            }

            @Override
            protected void done() {
                try {
                    List<Card> six = get();
                    human = new Player("Jugador");
                    cpu   = new Player("CPU");
                    duel  = new Duel(human, cpu);
                    duel.dealHands(six);

                    updateScore();
                    buildHandButtons();
                    setStatus("Elige una carta para jugar la ronda.");
                } catch (Exception ex) {
                    showError("No se pudieron obtener cartas.\n" + ex.getMessage());
                    panelHand.removeAll();
                    panelHand.add(new JLabel("Fallo al cargar. Intenta 'Nuevo duelo'."));
                    panelHand.revalidate();
                    panelHand.repaint();
                    setStatus("Error al repartir.");
                } finally {
                    lockInput(false);
                }
            }
        };
        worker.execute();
    }

    /** Crea los 3 botones de la mano del jugador en columna, con tamaños fijos. */
    private void buildHandButtons() {
        panelHand.removeAll();
        panelHand.add(space(0,8));

        for (int i = 0; i < human.getHand().size(); i++) {
            final int idx = i;
            Card c = human.getHand().get(i);

            JButton btn = new JButton("<html><center>" + escape(c.name) + "</center></html>");
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setToolTipText(statLine(c));
            btn.setEnabled(false);

            // Tamaño fijo del botón (incluye icono + texto)
            fixSize(btn, BUTTON_W, BUTTON_H + 48);

            btn.addActionListener(e -> onPlayerChoose(idx));
            panelHand.add(btn);
            panelHand.add(space(0,8));

            // Carga de miniatura en background
            SwingWorker<ImageIcon, Void> iconLoader = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    String url = imageUrlForButton(c);
                    return imageCache.getIcon(url, BUTTON_W, BUTTON_H);
                }
                @Override
                protected void done() {
                    try {
                        btn.setIcon(get());
                    } catch (Exception ignored) {
                    } finally {
                        btn.setEnabled(true);
                    }
                }
            };
            iconLoader.execute();
        }

        // Relleno para empujar hacia arriba si sobra espacio
        panelHand.add(Box.createVerticalGlue());

        panelHand.revalidate();
        panelHand.repaint();
    }

    /** Al elegir una carta. */
    private void onPlayerChoose(int playerIndex) {
        if (inputLocked || duel == null || duel.isOver()) return;
        lockInput(true);
        setStatus("Resolviendo ronda...");
        resetCardBorders();
        setCpuBack();

        SwingWorker<RoundResult, Void> roundWorker = new SwingWorker<>() {
            @Override
            protected RoundResult doInBackground() {
                return duel.playRound(playerIndex);
            }

            @Override
            protected void done() {
                try {
                    RoundResult rr = get();

                    setCardPreview(lblPlayerCard, rr.playerCard);
                    setCardPreview(lblCpuCard, rr.cpuCard);

                    updateScore();

                    String hist = String.format(
                            "R%d: %s vs %s → %s (%s)",
                            roundCounter,
                            rr.playerCard.name,
                            rr.cpuCard.name,
                            rr.winner == Winner.PLAYER ? "Jugador" : "CPU",
                            rr.reason
                    );
                    historyModel.addElement(hist);
                    roundCounter++;

                    buildHandButtons();

                    String msg = (rr.winner == Winner.PLAYER ? "Ganaste la ronda." : "La CPU ganó la ronda.")
                            + " " + rr.reason;
                    setStatus(msg);

                    highlightWinner(rr.winner);

                    // Banner grande de RONDA
                    showRoundBanner(rr.winner, rr.reason);

                    if (duel.isOver()) {
                        Winner mw = duel.getMatchWinnerOrNull();
                        if (mw == Winner.PLAYER) {
                            setStatus("🏆 ¡Has ganado el duelo best-of-3! Pulsa 'Nuevo duelo' para jugar otra vez.");
                        } else if (mw == Winner.CPU) {
                            setStatus("🤖 La CPU ganó el duelo best-of-3. Pulsa 'Nuevo duelo' para reintentar.");
                        } else {
                            setStatus("Duelo finalizado.");
                        }

                        // Banner grande de COMBATE (con dim del fondo)
                        showMatchBanner(mw);

                        // Deshabilitar mano
                        for (Component comp : panelHand.getComponents()) {
                            if (comp instanceof JButton) comp.setEnabled(false);
                        }
                    }

                } catch (Exception ex) {
                    showError("Error al jugar la ronda.\n" + ex.getMessage());
                    setStatus("Error en la ronda.");
                } finally {
                    lockInput(false);
                }
            }
        };
        roundWorker.execute();
    }

    // ------------------------ Helpers UI ------------------------

    private void setStatus(String text) { lblStatus.setText(text); }
    private void updateScore() { lblScore.setText("Marcador: Jugador " + human.getWins() + " - " + cpu.getWins() + " CPU"); }

    private void lockInput(boolean lock) {
        inputLocked = lock;
        btnNewDuel.setEnabled(!lock);
        for (Component comp : panelHand.getComponents()) {
            if (comp instanceof JButton) comp.setEnabled(!lock);
        }
    }

    private void setCardPreview(JLabel label, Card c) {
        label.setText("");
        label.setIcon(null);
        if (c == null) return;

        SwingWorker<ImageIcon, Void> w = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                String url = imageUrlForPreview(c);
                return imageCache.getIcon(url, PREVIEW_W, PREVIEW_H);
            }
            @Override
            protected void done() {
                try {
                    label.setIcon(get());
                } catch (Exception e) {
                    label.setText("<html><center>" + escape(c.name) + "<br/>(sin imagen)</center></html>");
                }
            }
        };
        w.execute();
    }

    private static String imageUrlForButton(Card c) {
        if (c.card_images != null && !c.card_images.isEmpty()) {
            var ci = c.card_images.get(0);
            if (ci.image_url_small != null && !ci.image_url_small.isBlank()) return ci.image_url_small;
            if (ci.image_url != null && !ci.image_url.isBlank()) return ci.image_url;
        }
        return "";
    }

    private static String imageUrlForPreview(Card c) {
        if (c.card_images != null && !c.card_images.isEmpty()) {
            var ci = c.card_images.get(0);
            if (ci.image_url != null && !ci.image_url.isBlank()) return ci.image_url;
            if (ci.image_url_small != null && !ci.image_url_small.isBlank()) return ci.image_url_small;
        }
        return "";
    }

    private static String statLine(Card c) {
        return "ATK=" + c.atk + " | DEF=" + c.def + " | LV=" + c.level + " | LINK=" + c.linkval;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // --------- Dorso CPU y bordes de ganador ---------

    private void setCpuBack() {
        lblCpuCard.setText("");
        lblCpuCard.setIcon(makeCardBackIcon("CPU", PREVIEW_W, PREVIEW_H));
    }

    private static ImageIcon makeCardBackIcon(String text, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(22, 26, 34), w, h, new Color(44, 54, 73)));
            g.fillRoundRect(0, 0, w, h, 28, 28);

            g.setColor(new Color(200, 200, 210));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(4, 4, w - 8, h - 8, 24, 24);

            g.setFont(new Font("SansSerif", Font.BOLD, 36));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            g.setColor(new Color(235, 235, 245));
            g.drawString(text, (w - tw) / 2, (h + th) / 2 - 8);
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    private void resetCardBorders() {
        Border outer = new LineBorder(new Color(180, 180, 185), 1, true);
        lblPlayerCard.setBorder(new CompoundBorder(outer, tBorderPlayer));
        lblCpuCard.setBorder(new CompoundBorder(outer, tBorderCpu));
    }

    private void highlightWinner(Winner w) {
        Border win = new LineBorder(new Color(46, 204, 113), 3, true); // verde
        Border lose = new LineBorder(new Color(180, 180, 185), 1, true);
        if (w == Winner.PLAYER) {
            lblPlayerCard.setBorder(new CompoundBorder(win, tBorderPlayer));
            lblCpuCard.setBorder(new CompoundBorder(lose, tBorderCpu));
        } else {
            lblPlayerCard.setBorder(new CompoundBorder(lose, tBorderPlayer));
            lblCpuCard.setBorder(new CompoundBorder(win, tBorderCpu));
        }
    }

    // Pequeño separador vertical
    private static Component space(int w, int h) {
        return Box.createRigidArea(new Dimension(w, h));
    }

    // ======================= OVERLAY DE BANNER =======================

    /**
     * GlassPane que pinta un banner central con fade-in/out.
     * Soporta 2 modos: ROUND (aviso de ronda) y MATCH (victoria del combate).
     */
    private class RoundBannerOverlay extends JComponent implements ActionListener {

        private enum Kind { ROUND, MATCH }

        private Kind kind = Kind.ROUND;
        private String title = "";
        private String subtitle = "";
        private Color main = new Color(46, 204, 113); // verde por defecto
        private float alpha = 0f;
        private int phase = 0; // 0=idle,1=in,2=hold,3=out
        private final Timer timer = new Timer(16, this); // ~60 FPS

        // tiempos (ajustables por modo)
        private int FADE_IN = 180;
        private int HOLD = 1050;
        private int FADE_OUT = 220;

        private long t0;

        RoundBannerOverlay() {
            setOpaque(false);
            setVisible(false);
        }

        void showRoundResult(Winner w, String reason) {
            kind = Kind.ROUND;
            FADE_IN = 180; HOLD = 1050; FADE_OUT = 220;
            if (w == Winner.PLAYER) {
                this.title = "¡Ronda para el Jugador!";
                this.main = new Color(46, 204, 113);
            } else {
                this.title = "¡Ronda para la CPU!";
                this.main = new Color(231, 76, 60);
            }
            this.subtitle = reason != null ? reason : "";
            startAnim();
        }

        void showMatchResult(Winner w) {
            kind = Kind.MATCH;
            FADE_IN = 220; HOLD = 1600; FADE_OUT = 280;
            if (w == Winner.PLAYER) {
                this.title = "¡Victoria del Jugador!";
                this.main = new Color(46, 204, 113);
            } else {
                this.title = "¡Victoria de la CPU!";
                this.main = new Color(231, 76, 60);
            }
            this.subtitle = "Ganó 2 de 3 rondas.";
            startAnim();
        }

        private void startAnim() {
            alpha = 0f; phase = 1; t0 = System.currentTimeMillis();
            setVisible(true);
            timer.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isVisible()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int W = getWidth();
                int H = getHeight();

                // En MATCH oscurecemos todo el fondo suavemente
                if (kind == Kind.MATCH) {
                    g2.setComposite(AlphaComposite.SrcOver.derive(0.35f * alpha));
                    g2.setColor(Color.BLACK);
                    g2.fillRect(0, 0, W, H);
                }

                // Dimensiones del banner (más grande en MATCH)
                int bw = (kind == Kind.MATCH) ? 720 : 620;
                int bh = (kind == Kind.MATCH) ? 180 : 150;
                int x = (W - bw) / 2;
                int y = (H - bh) / 2;

                // sombra suave
                g2.setComposite(AlphaComposite.SrcOver.derive(0.20f * alpha));
                g2.setColor(Color.BLACK);
                g2.fillRoundRect(x + 6, y + 8, bw, bh, 22, 22);

                // tarjeta
                g2.setComposite(AlphaComposite.SrcOver.derive(0.92f * alpha));
                g2.setColor(new Color(25, 28, 34, 235));
                g2.fillRoundRect(x, y, bw, bh, 22, 22);

                // borde
                g2.setComposite(AlphaComposite.SrcOver.derive(0.95f * alpha));
                g2.setColor(main);
                g2.setStroke(new BasicStroke(3f));
                g2.drawRoundRect(x, y, bw, bh, 22, 22);

                // textos
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g2.setColor(new Color(240, 244, 248));
                Font fTitle = getFont().deriveFont(Font.BOLD, (kind == Kind.MATCH) ? 30f : 26f);
                Font fSub   = getFont().deriveFont(Font.PLAIN, (kind == Kind.MATCH) ? 18f : 16f);

                g2.setFont(fTitle);
                FontMetrics fmT = g2.getFontMetrics();
                int tx = x + (bw - fmT.stringWidth(title)) / 2;
                int ty = y + (kind == Kind.MATCH ? 70 : 60);
                g2.drawString(title, tx, ty);

                g2.setFont(fSub);
                FontMetrics fmS = g2.getFontMetrics();
                int sx = x + (bw - fmS.stringWidth(subtitle)) / 2;
                int sy = y + (kind == Kind.MATCH ? 112 : 100);
                g2.drawString(subtitle, sx, sy);

            } finally {
                g2.dispose();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            long dt = (int) (System.currentTimeMillis() - t0);
            switch (phase) {
                case 1: // fade in
                    if (dt >= FADE_IN) { alpha = 1f; phase = 2; t0 = System.currentTimeMillis(); }
                    else alpha = dt / (float) FADE_IN;
                    break;
                case 2: // hold
                    alpha = 1f;
                    if (dt >= HOLD) { phase = 3; t0 = System.currentTimeMillis(); }
                    break;
                case 3: // fade out
                    if (dt >= FADE_OUT) {
                        alpha = 0f; phase = 0; timer.stop(); setVisible(false);
                    } else {
                        alpha = 1f - dt / (float) FADE_OUT;
                    }
                    break;
                default:
                    timer.stop(); setVisible(false); alpha = 0f;
            }
            repaint();
        }
    }

    /** Muestra el banner redondeado con fade para el ganador de la ronda. */
    private void showRoundBanner(Winner w, String reason) {
        banner.showRoundResult(w, reason);
    }

    /** Muestra el banner de victoria del combate (dim de fondo + tipografía mayor). */
    private void showMatchBanner(Winner w) {
        banner.showMatchResult(w);
    }
}
