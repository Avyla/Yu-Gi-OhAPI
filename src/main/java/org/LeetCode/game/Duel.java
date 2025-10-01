package org.LeetCode.game;

import org.LeetCode.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Administra un duelo best-of-3.
 * - Reparte 3 cartas únicas a cada jugador.
 * - En cada ronda: jugador elige índice; CPU elige al azar uno de los suyos.
 * - Suma victorias; termina cuando alguien llega a 2.
 */
public class Duel {

    private final Player player;
    private final Player cpu;

    public Duel(Player player, Player cpu) {
        this.player = player;
        this.cpu = cpu;
    }

    public Player getPlayer() { return player; }
    public Player getCpu() { return cpu; }

    /** Reparte 3 cartas únicas a cada uno a partir de un pool >= 6. */
    public void dealHands(List<Card> pool6plus) {
        if (pool6plus == null || pool6plus.size() < 6) {
            throw new IllegalArgumentException("Se requieren al menos 6 cartas para repartir.");
        }
        // Barajar y tomar 6 primeras únicas
        List<Card> copy = new ArrayList<>(pool6plus);
        Collections.shuffle(copy);
        List<Card> six = copy.subList(0, 6);

        List<Card> handP = new ArrayList<>(six.subList(0, 3));
        List<Card> handC = new ArrayList<>(six.subList(3, 6));

        player.setHand(handP);
        cpu.setHand(handC);
    }

    public boolean isOver() {
        return player.hasWonMatch() || cpu.hasWonMatch() || player.getHand().isEmpty() || cpu.getHand().isEmpty();
    }

    public Winner getMatchWinnerOrNull() {
        if (player.hasWonMatch()) return Winner.PLAYER;
        if (cpu.hasWonMatch()) return Winner.CPU;
        return null;
    }

    /**
     * Juega una ronda:
     * - playerIndex: índice de la carta elegida por el jugador (0..mano-1).
     * - CPU elige aleatoriamente de sus restantes.
     * Retorna el resultado de la ronda y actualiza las victorias.
     */
    public RoundResult playRound(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= player.getHand().size()) {
            throw new IllegalArgumentException("Índice de carta del jugador inválido: " + playerIndex);
        }
        // Jugador elige su carta
        Card p = player.removeFromHand(playerIndex);

        // CPU elige una al azar de su mano
        int cpuIndex = ThreadLocalRandom.current().nextInt(cpu.getHand().size());
        Card c = cpu.removeFromHand(cpuIndex);

        RoundResult rr = CardComparator.duel(p, c);
        if (rr.winner == Winner.PLAYER) {
            player.addWin();
        } else {
            cpu.addWin();
        }
        return rr;
    }
}
