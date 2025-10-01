package org.LeetCode.game;

import org.LeetCode.model.Card;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Reglas de comparación:
 * 1) Mayor ATK gana.
 * 2) Si empatan en ATK: mayor DEF gana (tratamos DEF null como 0).
 * 3) Si empatan: mayor Level o LinkVal gana (tomamos linkval si no es null, si no level; null -> 0).
 * 4) Si sigue empate total: moneda al aire (aleatorio).
 *
 * Devuelve Winner.PLAYER o Winner.CPU y una explicación en texto.
 */
public final class CardComparator {

    private CardComparator() {}

    private static int val(Integer x) { return x == null ? 0 : x; }

    private static int levelOrLink(Card c) {
        if (c == null) return 0;
        if (c.linkval != null) return c.linkval;
        if (c.level   != null) return c.level;
        return 0;
    }

    public static RoundResult duel(Card player, Card cpu) {
        int pATK = val(player.atk);
        int cATK = val(cpu.atk);

        if (pATK > cATK) {
            return new RoundResult(player, cpu, Winner.PLAYER, "Mayor ATK (" + pATK + " > " + cATK + ")");
        } else if (cATK > pATK) {
            return new RoundResult(player, cpu, Winner.CPU, "Mayor ATK (" + cATK + " > " + pATK + ")");
        }

        // Empate en ATK -> DEF
        int pDEF = val(player.def);
        int cDEF = val(cpu.def);

        if (pDEF > cDEF) {
            return new RoundResult(player, cpu, Winner.PLAYER, "Empate en ATK; mayor DEF (" + pDEF + " > " + cDEF + ")");
        } else if (cDEF > pDEF) {
            return new RoundResult(player, cpu, Winner.CPU, "Empate en ATK; mayor DEF (" + cDEF + " > " + pDEF + ")");
        }

        // Empate en ATK y DEF -> LV o LINK
        int pLVL = levelOrLink(player);
        int cLVL = levelOrLink(cpu);

        if (pLVL > cLVL) {
            return new RoundResult(player, cpu, Winner.PLAYER, "Empate en ATK/DEF; mayor LV/LINK (" + pLVL + " > " + cLVL + ")");
        } else if (cLVL > pLVL) {
            return new RoundResult(player, cpu, Winner.CPU, "Empate en ATK/DEF; mayor LV/LINK (" + cLVL + " > " + pLVL + ")");
        }

        // Empate total -> moneda
        boolean coin = ThreadLocalRandom.current().nextBoolean();
        return new RoundResult(
                player,
                cpu,
                coin ? Winner.PLAYER : Winner.CPU,
                "Empate total; desempate aleatorio"
        );
    }
}
