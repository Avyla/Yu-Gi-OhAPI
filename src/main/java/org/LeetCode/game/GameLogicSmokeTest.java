package org.LeetCode.game;

import org.LeetCode.api.YgoApiClient;
import org.LeetCode.game.Duel;
import org.LeetCode.game.RoundResult;
import org.LeetCode.game.Winner;
import org.LeetCode.model.Card;
import org.LeetCode.game.Player;

import java.util.List;

public class GameLogicSmokeTest {

    public static void main(String[] args) {
        try {
            // 1) Traer 6 Monsters
            YgoApiClient api = new YgoApiClient();
            List<Card> six = api.fetchMonstersFast(6, 120, 0, 500, 1000, 1500);

            // 2) Armar duelo
            Player human = new Player("Jugador");
            Player cpu   = new Player("CPU");
            Duel duel = new Duel(human, cpu);
            duel.dealHands(six);

            // Mostrar manos
            System.out.println("=== Mano del Jugador ===");
            for (int i = 0; i < human.getHand().size(); i++) {
                Card c = human.getHand().get(i);
                System.out.printf("[%d] %s | %s | ATK=%s DEF=%s LV=%s LINK=%s%n",
                        i, c.name, c.type,
                        String.valueOf(c.atk),
                        String.valueOf(c.def),
                        String.valueOf(c.level),
                        String.valueOf(c.linkval));
            }
            System.out.println("=== Mano de la CPU (oculta) === (" + cpu.getHand().size() + " cartas)\n");

            // 3) Jugar hasta que alguien llegue a 2 victorias (best-of-3)
            int round = 1;
            while (!duel.isOver()) {
                // Estrategia simple para el test: el jugador siempre elige el índice 0 de su mano restante.
                int playerChoice = 0;

                System.out.println(">> Ronda " + round + " | Jugador elige índice " + playerChoice);
                RoundResult rr = duel.playRound(playerChoice);

                System.out.printf("   Jugador: %s (ATK=%s DEF=%s LV=%s LINK=%s)%n",
                        rr.playerCard.name,
                        String.valueOf(rr.playerCard.atk),
                        String.valueOf(rr.playerCard.def),
                        String.valueOf(rr.playerCard.level),
                        String.valueOf(rr.playerCard.linkval));

                System.out.printf("   CPU:     %s (ATK=%s DEF=%s LV=%s LINK=%s)%n",
                        rr.cpuCard.name,
                        String.valueOf(rr.cpuCard.atk),
                        String.valueOf(rr.cpuCard.def),
                        String.valueOf(rr.cpuCard.level),
                        String.valueOf(rr.cpuCard.linkval));

                System.out.println("   Resultado: " + rr);

                System.out.printf("   Marcador -> Jugador: %d | CPU: %d%n%n",
                        human.getWins(), cpu.getWins());

                round++;
            }

            Winner mw = duel.getMatchWinnerOrNull();
            System.out.println("=== FIN DEL DUELO ===");
            if (mw == Winner.PLAYER) {
                System.out.println("🏆 ¡El Jugador ganó la partida (best-of-3)!");
            } else if (mw == Winner.CPU) {
                System.out.println("🤖 La CPU ganó la partida (best-of-3).");
            } else {
                System.out.println("Partida finalizada sin ganador claro (esto no debería ocurrir).");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fallo en GameLogicSmokeTest.");
        }
    }
}
