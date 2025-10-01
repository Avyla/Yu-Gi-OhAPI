package org.LeetCode.game;

import org.LeetCode.model.Card;

public class RoundResult {
    public final Card playerCard;
    public final Card cpuCard;
    public final Winner winner;
    public final String reason;

    public RoundResult(Card playerCard, Card cpuCard, Winner winner, String reason) {
        this.playerCard = playerCard;
        this.cpuCard = cpuCard;
        this.winner = winner;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "Ganador: " + winner +
                " | Motivo: " + reason +
                " | Player: " + playerCard.name +
                " vs CPU: " + cpuCard.name;
    }
}
