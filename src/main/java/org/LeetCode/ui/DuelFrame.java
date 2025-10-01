package org.LeetCode.ui;

import org.LeetCode.api.YgoApiClient;
import org.LeetCode.game.Duel;
import org.LeetCode.game.Player;
import org.LeetCode.game.RoundResult;
import org.LeetCode.game.Winner;
import org.LeetCode.model.Card;
import org.LeetCode.util.ImageCache;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DuelFrame extends JFrame {

    // Servicios / estado
    private final YgoApiClient api = new YgoApiClient();
    private final ImageCache imageCache = new ImageCache();
    private Duel duel;
    private Player human;
    private Player cpu;

    // UI
    private final JButton btnNewDuel = new JButton("Nuevo duelo");
    private final JLabel lblScore = new JLabel("Marcador: Jugador 0 - 0 CPU");
    private final JLabel lblStatus = new JLabel("Listo.");
    private final JPanel panelHand = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
    private final JLabel lblPlayerCard = new JLabel(); // carta grande jugador
    private final JLabel lblCpuCard = new JLabel();    // carta grande CPU

    // Para deshabilitar entradas mientras carga o resuelve ronda
    private boolean inputLocked = false;

    public DuelFrame() {
        super("Mini Duelo Yu-Gi-Oh! (best-of-3)");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // TOP: controles + marcador
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(btnNewDuel, BorderLayout.WEST);
        lblScore.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(lblScore, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // CENTER: arena (dos cartas grandes)
        JPanel arena = new JPanel(new GridLayout(1, 2, 10, 10));
        lblPlayerCard.setHorizontalAlignment(SwingConstants.CENTER);
        lblCpuCard.setHorizontalAlignment(SwingConstants.CENTER);
        lblPlayerCard.setBorder(BorderFactory.createTitledBorder("Jugador"));
        lblCpuCard.setBorder(BorderFactory.createTitledBorder("CPU"));
        arena.add(lblPlayerCard);
        arena.add(lblCpuCard);
        add(arena, BorderLayout.CENTER);

        // BOTTOM: mano del jugador + status
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(panelHand, BorderLayout.CENTER);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(lblStatus, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        // Acción: Nuevo duelo
        btnNewDuel.addActionListener(e -> startNewDuel());

        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(null);
        startNewDuel();
    }

    /** Inicia/reinicia un duelo: trae 6 monsters, reparte y arma la mano (en background). */
    private void startNewDuel() {
        if (inputLocked) return;
        lockInput(true);
        setStatus("Repartiendo cartas...");
        lblPlayerCard.setIcon(null);
        lblPlayerCard.setText("");
        lblCpuCard.setIcon(null);
        lblCpuCard.setText("");

        panelHand.removeAll();
        panelHand.add(new JLabel("Cargando..."));
        panelHand.revalidate();
        panelHand.repaint();

        // Carga en background para no bloquear el EDT
        SwingWorker<List<Card>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                // 6 monsters con fast path
                return api.fetchMonstersFast(6, 120, 0, 500, 1000, 1500);
            }

            @Override
            protected void done() {
                try {
                    List<Card> six = get();
                    // Crear jugadores nuevos para resetear marcador
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

    /** Crea los 3 botones con miniaturas de la mano del jugador. */
    private void buildHandButtons() {
        panelHand.removeAll();

        for (int i = 0; i < human.getHand().size(); i++) {
            final int idx = i;
            Card c = human.getHand().get(i);

            JButton btn = new JButton("<html><center>" + escape(c.name) + "</center></html>");
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setToolTipText(statLine(c));

            // Carga de icono en background (para no congelar)
            btn.setEnabled(false);
            btn.addActionListener(e -> onPlayerChoose(idx));

            panelHand.add(btn);

            SwingWorker<ImageIcon, Void> iconLoader = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() throws Exception {
                    String url = imageUrlForButton(c);
                    return imageCache.getIcon(url, 180, 260); // miniatura
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icon = get();
                        btn.setIcon(icon);
                    } catch (Exception ignored) {
                        // Si falla la imagen, dejamos solo el texto
                    } finally {
                        btn.setEnabled(true);
                    }
                }
            };
            iconLoader.execute();
        }

        panelHand.revalidate();
        panelHand.repaint();
    }

    /** Cuando el jugador elige una carta (índice en su mano). */
    private void onPlayerChoose(int playerIndex) {
        if (inputLocked || duel == null || duel.isOver()) return;
        lockInput(true);
        setStatus("Resolviendo ronda...");

        // Jugar y actualizar UI
        SwingWorker<RoundResult, Void> roundWorker = new SwingWorker<>() {
            @Override
            protected RoundResult doInBackground() {
                return duel.playRound(playerIndex);
            }

            @Override
            protected void done() {
                try {
                    RoundResult rr = get();

                    // Mostrar cartas grandes en la arena
                    setCardPreview(lblPlayerCard, rr.playerCard);
                    setCardPreview(lblCpuCard, rr.cpuCard);

                    // Actualizar marcador
                    updateScore();

                    // Reconstruir mano (quitar la carta usada)
                    buildHandButtons();

                    // Mensaje
                    String msg = (rr.winner == Winner.PLAYER ? "Ganaste la ronda. " : "La CPU ganó la ronda. ")
                            + rr.reason;
                    setStatus(msg);

                    // ¿Fin del duelo?
                    if (duel.isOver()) {
                        Winner mw = duel.getMatchWinnerOrNull();
                        if (mw == Winner.PLAYER) {
                            setStatus("🏆 ¡Has ganado el duelo best-of-3! Pulsa 'Nuevo duelo' para jugar otra vez.");
                        } else if (mw == Winner.CPU) {
                            setStatus("🤖 La CPU ganó el duelo best-of-3. Pulsa 'Nuevo duelo' para reintentar.");
                        } else {
                            setStatus("Duelo finalizado.");
                        }
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

    private void setStatus(String text) {
        lblStatus.setText(text);
    }

    private void updateScore() {
        lblScore.setText("Marcador: Jugador " + human.getWins() + " - " + cpu.getWins() + " CPU");
    }

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

        // Carga en background para no bloquear
        SwingWorker<ImageIcon, Void> w = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                String url = imageUrlForPreview(c);
                return imageCache.getIcon(url, 360, 520); // grande
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
        return ""; // dejará solo texto si no hay imagen
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
}
