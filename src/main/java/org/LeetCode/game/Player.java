package org.LeetCode.game;

import org.LeetCode.model.Card;

import java.util.ArrayList;
import java.util.List;

public class Player {
    public final String name;
    private final List<Card> hand = new ArrayList<>(3);
    private int wins = 0;

    public Player(String name) {
        this.name = name;
    }

    public void setHand(List<Card> cards3) {
        hand.clear();
        if (cards3 != null) hand.addAll(cards3);
    }

    public List<Card> getHand() {
        return hand;
    }

    public Card removeFromHand(int index) {
        return hand.remove(index);
    }

    public int getWins() {
        return wins;
    }

    public void addWin() {
        wins++;
    }

    public boolean hasWonMatch() {
        return wins >= 2;
    }
}
