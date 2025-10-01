package org.LeetCode.ui;

import org.LeetCode.api.YgoApiClient;
import org.LeetCode.model.Card;
import org.LeetCode.util.ImageCache;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.ImageIcon;
import java.nio.file.Path;
import java.util.List;

public class ImageCacheSmokeTest {

    public static void main(String[] args) {
        try {
            YgoApiClient api = new YgoApiClient();
            ImageCache cache = new ImageCache();

            System.out.println("Obteniendo 3 Monsters (para probar imagen)...");
            List<Card> cards = api.fetchMonstersFast(3, 120, 0, 500, 1000);

            if (cards.isEmpty()) {
                System.err.println("No se obtuvieron cartas.");
                return;
            }

            Card c = cards.get(0);
            String url = null;
            if (c.card_images != null && !c.card_images.isEmpty()) {
                // Preferimos la miniatura si existe; si no, la grande
                if (c.card_images.get(0).image_url_small != null && !c.card_images.get(0).image_url_small.isBlank()) {
                    url = c.card_images.get(0).image_url_small;
                } else {
                    url = c.card_images.get(0).image_url;
                }
            }
            if (url == null) {
                System.err.println("La carta no tiene imagen: " + c.name);
                return;
            }

            // Descargar / leer desde caché y mostrar
            ImageIcon icon = cache.getIcon(url, 350, 350);
            Path cached = cache.cachedFilePath(url);
            System.out.println("Carta: " + c.name + " | Imagen cacheada en: " + cached);

            final String title = "Prueba de imagen - " + c.name;
            final ImageIcon iconFinal = icon;

            SwingUtilities.invokeLater(() -> {
                JFrame f = new JFrame(title);
                f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                f.add(new JLabel(iconFinal));
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            });

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fallo en ImageCacheSmokeTest.");
        }
    }
}
