package org.LeetCode.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.LeetCode.model.Card;
import org.LeetCode.model.CardInfoResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class YgoApiClient {

    private static final String BASE = "https://db.ygoprodeck.com/api/v7";
    private static final String RANDOM_CARD = BASE + "/randomcard.php";
    private static final String CARD_INFO   = BASE + "/cardinfo.php";

    private final HttpClient http;
    private final Gson gson;

    public YgoApiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new Gson();
    }

    // ---------- RUTA RANDOM (la dejamos por si quieres compararla) ----------

    public Card getRandomCard() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(RANDOM_CARD))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .header("User-Agent", "YgoMiniApp/1.2 (edu)")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code != 200) throw new IOException("HTTP " + code + " from randomcard: " + resp.body());
        try {
            return gson.fromJson(resp.body(), Card.class);
        } catch (JsonSyntaxException ex) {
            throw new IOException("JSON inválido de randomcard", ex);
        }
    }

    public Card fetchRandomMonsterViaRandom(int maxTries) throws IOException, InterruptedException {
        int tries = Math.max(1, maxTries);
        for (int i = 1; i <= tries; i++) {
            System.out.printf("  [randomcard] intento %d/%d...%n", i, tries);
            try {
                Card c = getRandomCard();
                if (c != null && c.isMonster() && c.atk != null) return c;
            } catch (IOException | InterruptedException ex) {
                System.out.println("    aviso: " + ex.getMessage());
            }
            Thread.sleep(120);
        }
        throw new IOException("No se pudo obtener Monster vía randomcard tras " + tries + " intentos.");
    }

    // ---------- RUTA CARDINFO (batch + filtro local) ----------

    private CardInfoResponse getCardInfoPage(int num, int offset) throws IOException, InterruptedException {
        String url = CARD_INFO + "?num=" + num + "&offset=" + offset;
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "YgoMiniApp/1.2 (edu)")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " from cardinfo: " + resp.body());
        }
        try {
            return gson.fromJson(resp.body(), CardInfoResponse.class);
        } catch (JsonSyntaxException ex) {
            throw new IOException("JSON inválido de cardinfo", ex);
        }
    }

    /** Devuelve una lista de Monsters (ATK != null) a partir de una página de cardinfo. */
    private List<Card> extractMonsters(CardInfoResponse page) {
        if (page == null || page.data == null) return Collections.emptyList();
        List<Card> out = new ArrayList<>();
        for (Card c : page.data) {
            if (c != null && c.isMonster() && c.atk != null) out.add(c);
        }
        return out;
    }

    /**
     * Fast path recomendado: trae uno o más bloques de cardinfo y arma un pool local.
     * - needed: cuántos Monsters únicos necesitas (p.ej., 6).
     * - num: tamaño de página (recomiendo 120 para buen ratio de Monsters).
     * - offsets: candidatos para “diversificar” el pool sin consultar totalRows.
     */
    public List<Card> fetchMonstersFast(int needed, int num, int... offsets) throws IOException, InterruptedException {
        if (needed <= 0) return Collections.emptyList();

        // Para evitar duplicados por id:
        Map<Integer, Card> poolById = new LinkedHashMap<>();

        for (int offset : offsets) {
            CardInfoResponse page = getCardInfoPage(num, offset);
            for (Card c : extractMonsters(page)) {
                poolById.putIfAbsent(c.id, c);
                if (poolById.size() >= needed * 3) break; // pool generoso (x3) para barajar
            }
            if (poolById.size() >= needed * 3) break;
        }

        // Si aún es chico, pide otra página desde offset “aleatorio básico”
        if (poolById.size() < needed) {
            int extraOffset = (int)(Math.random() * 1500); // heurística simple
            CardInfoResponse page = getCardInfoPage(num, extraOffset);
            for (Card c : extractMonsters(page)) poolById.putIfAbsent(c.id, c);
        }

        List<Card> pool = new ArrayList<>(poolById.values());
        if (pool.isEmpty())
            throw new IOException("No se logró armar pool de Monsters desde cardinfo.");

        // Barajar y tomar 'needed'
        Collections.shuffle(pool);
        return pool.stream().limit(needed).collect(Collectors.toList());
    }

    // Wrapper conveniente: “dame un Monster cualquiera” usando fast path
    public Card fetchRandomMonsterFast() throws IOException, InterruptedException {
        List<Card> one = fetchMonstersFast(1, 120, 0, 500, 1000);
        return one.get(0);
    }

    // Utilidad para búsquedas exactas (opcional)
    public CardInfoResponse findByName(String exactName) throws IOException, InterruptedException {
        String url = CARD_INFO + "?name=" + encode(exactName);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .header("User-Agent", "YgoMiniApp/1.2 (edu)")
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " from cardinfo: " + resp.body());
        }
        try {
            return gson.fromJson(resp.body(), CardInfoResponse.class);
        } catch (JsonSyntaxException ex) {
            throw new IOException("JSON inválido de cardinfo", ex);
        }
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
