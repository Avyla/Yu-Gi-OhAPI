package org.LeetCode.util;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class ImageCache {

    private final Path cacheDir;
    private final HttpClient http;
    private final Map<String, SoftReference<BufferedImage>> memCache = new HashMap<>();

    public ImageCache() {
        this.cacheDir = Paths.get(System.getProperty("user.home"), ".ygo-cache");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            // Si no podemos crear, seguiremos sin cache en disco (solo memoria)
            System.err.println("[ImageCache] No se pudo crear el directorio: " + cacheDir + " -> " + e.getMessage());
        }
    }

    /** Devuelve la ruta donde se cachearía una URL (no asegura que exista). */
    public Path cachedFilePath(String url) {
        String name = sha256Hex(url);
        return cacheDir.resolve(name);
    }

    /** Obtiene la imagen (disco/memoria o descarga) como BufferedImage. */
    public BufferedImage getImage(String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            throw new IOException("URL de imagen vacío");
        }

        // 1) Cache en memoria
        SoftReference<BufferedImage> ref = memCache.get(url);
        if (ref != null) {
            BufferedImage img = ref.get();
            if (img != null) return img;
        }

        // 2) Cache en disco
        Path file = cachedFilePath(url);
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    memCache.put(url, new SoftReference<>(img));
                    return img;
                }
            } catch (IOException ex) {
                // Si no se puede leer, intentaremos descargar de nuevo
                System.err.println("[ImageCache] Error leyendo cache, re-descargando: " + ex.getMessage());
            }
        }

        // 3) Descargar
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "image/*")
                .header("User-Agent", "YgoMiniApp/1.0 (edu)")
                .GET()
                .build();

        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " al descargar imagen: " + url);
        }

        // Guardar en disco (si se puede)
        try {
            Files.write(file, resp.body());
        } catch (IOException ex) {
            System.err.println("[ImageCache] No se pudo escribir en disco: " + file + " -> " + ex.getMessage());
        }

        // Cargar a BufferedImage
        try (InputStream in = Files.newInputStream(file)) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("Formato de imagen no soportado: " + url);
            memCache.put(url, new SoftReference<>(img));
            return img;
        }
    }

    /** Crea un ImageIcon escalado para Swing, manteniendo proporción. */
    public ImageIcon getIcon(String url, int maxWidth, int maxHeight) throws IOException, InterruptedException {
        BufferedImage img = getImage(url);
        BufferedImage scaled = scaleToFit(img, maxWidth, maxHeight);
        return new ImageIcon(scaled);
    }

    /** Escala con alta calidad (no hace upscale si la imagen ya es pequeña). */
    public static BufferedImage scaleToFit(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min((double) maxW / w, (double) maxH / h);
        if (scale >= 1.0) {
            // No escalar hacia arriba; devuelve original
            return src;
        }
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));

        BufferedImage dst = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return dst;
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback rarísimo
            return Integer.toHexString(s.hashCode());
        }
    }
}
