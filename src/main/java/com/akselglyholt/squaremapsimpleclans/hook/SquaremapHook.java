package com.akselglyholt.squaremapsimpleclans.hook;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import com.akselglyholt.squaremapsimpleclans.ClanMap;
import com.akselglyholt.squaremapsimpleclans.task.ImageDownloadTask;
import com.akselglyholt.squaremapsimpleclans.task.SquaremapTask;

public final class SquaremapHook {
  public static final Key CLAN_BASE_LAYER_KEY = Key.of("clans");
  public static BufferedImage CLAN_BASE_IMAGE;

  private final Map<WorldIdentifier, SquaremapTask> tasks = new HashMap<>();
  private final Map<String, BufferedImage> clanImageCache = new HashMap<>();
  private final ClanMap plugin;
  private ImageDownloadTask imageDownloadTask;

  public SquaremapHook(final ClanMap plugin, final SimpleClansHook simpleClansHook) {
    this.plugin = plugin;

    // Load default clanhome image from resources folder
    try {
      CLAN_BASE_IMAGE = ImageIO.read(new File(plugin.getDataFolder(), "clanhome.png"));
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load image from resources folder: " + e.getMessage());
      return;
    }

    // Create a SimpleLayerProvider for clan markers, in each world
    for (final MapWorld world : SquaremapProvider.get().mapWorlds()) {
      SimpleLayerProvider provider = SimpleLayerProvider.builder("Clan Bases")
          .defaultHidden(false)
          .showControls(true)
          .layerPriority(99)
          .zIndex(1000)
          .build();

      // Register the layer with Squaremap
      world.layerRegistry().register(CLAN_BASE_LAYER_KEY, provider);

      // Task for periodically updating markers every 5 minutes
      final SquaremapTask task = new SquaremapTask(plugin, world, provider, simpleClansHook, this);
      task.runTaskTimerAsynchronously(plugin, 20L, 20L * 300); // 300 seconds - 5 minutes

      this.tasks.put(world.identifier(), task);
    }

    // Start periodic image download task
    this.imageDownloadTask = new ImageDownloadTask(plugin, this);
    this.imageDownloadTask.start();
  }

  public void disable() {
    this.tasks.values().forEach(SquaremapTask::disable);
    this.tasks.clear();
    this.clanImageCache.clear();
    if (this.imageDownloadTask != null) {
      this.imageDownloadTask.disable();
    }
  }

  /**
   * Gets the image for a specific clan. If a custom image exists for the clan tag,
   * it will be used. Otherwise, the default clanhome.png is returned.
   */
  public BufferedImage getClanImage(String clanTag) {
    if (clanImageCache.containsKey(clanTag)) {
      return clanImageCache.get(clanTag);
    }

    File customImage = new File(plugin.getDataFolder(), clanTag + ".png");
    if (customImage.exists()) {
      try {
        BufferedImage image = ImageIO.read(customImage);
        clanImageCache.put(clanTag, image);
        plugin.getLogger().info("Loaded custom image for clan: " + clanTag);
        return image;
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to load custom image for clan " + clanTag + ": " + e.getMessage());
      }
    }

    clanImageCache.put(clanTag, CLAN_BASE_IMAGE);
    return CLAN_BASE_IMAGE;
  }

  public void clearClanImageCache(String clanTag) {
    clanImageCache.remove(clanTag);
  }

  public void clearAllImageCache() {
    clanImageCache.clear();
  }
}
