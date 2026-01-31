package com.akselglyholt.squaremapsimpleclans.task;

import com.akselglyholt.squaremapsimpleclans.hook.SquaremapHook;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ImageDownloadTask extends BukkitRunnable {
    private static final String INDEX_URL = "https://vanilla-game.ru/clans/index.json";
    private static final String BASE_URL = "https://vanilla-game.ru/clans/";
    private static final long INTERVAL_TICKS = 20L * 60 * 60 * 12; // 12 hours

    private final JavaPlugin plugin;
    private final SquaremapHook squaremapHook;
    private boolean stop;

    public ImageDownloadTask(JavaPlugin plugin, SquaremapHook squaremapHook) {
        this.plugin = plugin;
        this.squaremapHook = squaremapHook;
    }

    public void start() {
        this.runTaskTimerAsynchronously(plugin, 20L * 5, INTERVAL_TICKS);
    }

    @Override
    public void run() {
        if (stop) {
            cancel();
            return;
        }

        try {
            plugin.getLogger().info("Fetching clan image index...");
            String json = fetchString(INDEX_URL);
            String[] filenames = parseJsonArray(json);
            plugin.getLogger().info("Found " + filenames.length + " images to download.");

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            for (String filename : filenames) {
                if (filename.isEmpty()) continue;
                try {
                    downloadFile(BASE_URL + filename, new File(dataFolder, filename));
                    plugin.getLogger().info("Downloaded: " + filename);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to download " + filename + ": " + e.getMessage());
                }
            }

            squaremapHook.clearAllImageCache();
            plugin.getLogger().info("Image cache cleared after download.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch image index: " + e.getMessage());
        }
    }

    public void disable() {
        this.stop = true;
        this.cancel();
    }

    private String fetchString(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private void downloadFile(String urlStr, File dest) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Parses a simple JSON array of strings, e.g. ["a.png","b.png"]
     */
    private String[] parseJsonArray(String json) {
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
        if (json.trim().isEmpty()) return new String[0];

        String[] parts = json.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].trim().replace("\"", "");
        }
        return result;
    }
}
