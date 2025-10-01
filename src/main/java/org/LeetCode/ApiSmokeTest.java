package org.LeetCode;

import org.LeetCode.api.YgoApiClient;
import org.LeetCode.model.Card;

import java.util.List;

public class ApiSmokeTest {
    public static void main(String[] args) {
        try {
            YgoApiClient api = new YgoApiClient();
            System.out.println("Obteniendo 6 Monsters con fast path (cardinfo)...");
            // Pedimos 6, con páginas de 120 y offsets para variedad
            List<Card> six = api.fetchMonstersFast(6, 120, 0, 500, 1000, 1500);

            int i = 1;
            for (Card c : six) {
                System.out.printf("#%d: %s | %s | ATK=%s DEF=%s LV=%s LINK=%s%n",
                        i++, c.name, c.type,
                        String.valueOf(c.atk),
                        String.valueOf(c.def),
                        String.valueOf(c.level),
                        String.valueOf(c.linkval));
                if (c.card_images != null && !c.card_images.isEmpty()) {
                    System.out.println("  img: " + c.card_images.get(0).image_url_small);
                }
            }
            System.out.println("OK ✅");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fallo en la prueba de API. Revisa tu conexión o el mapeo JSON.");
        }
    }
}
