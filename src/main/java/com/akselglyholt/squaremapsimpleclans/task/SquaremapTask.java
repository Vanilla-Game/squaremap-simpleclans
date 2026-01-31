package com.akselglyholt.squaremapsimpleclans.task;

import com.akselglyholt.squaremapsimpleclans.ClanMap;
import com.akselglyholt.squaremapsimpleclans.hook.SimpleClansHook;
import com.akselglyholt.squaremapsimpleclans.hook.SquaremapHook;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

public final class SquaremapTask extends BukkitRunnable {
    private final World bukkitWorld;
    private final SimpleLayerProvider provider;
    private final ClanMap plugin;
    private final SimpleClansHook simpleClansHook;
    private final SquaremapHook squaremapHook;

    private boolean stop;

    public SquaremapTask(
            final ClanMap plugin,
            final MapWorld world,
            final SimpleLayerProvider provider,
            final SimpleClansHook simpleClansHook,
            final SquaremapHook squaremapHook) {
        this.plugin = plugin;
        this.bukkitWorld = BukkitAdapter.bukkitWorld(world);
        this.provider = provider;
        this.simpleClansHook = simpleClansHook;
        this.squaremapHook = squaremapHook;
    }

    @Override
    public void run() {
        if (this.stop) {
            this.cancel();
        }
        this.updateBases();
    }

    void updateBases() {
        this.provider.clearMarkers();

        // Make sure simpleClansHook is not null
        SimpleClans simpleClans = simpleClansHook.getSimpleClans();

        // Fetch clans from SimpleClans
        Clan[] clans = simpleClans.getClanManager().getClans()
                .toArray(new Clan[0]);

        if (clans.length == 0) {
            return;
        }

        // Register each clan marker on the map
        for (Clan clan : clans) {
            Location homeLocation = clan.getHomeLocation();
            final Key CUSTOM_CLAN_BASE_KEY = Key.of("clanhome-" + clan.getTag());

            if (homeLocation == null || homeLocation.getWorld() != this.bukkitWorld) {
                continue;
            }

            Point homePoint = Point.of(homeLocation.getX(), homeLocation.getZ());

            try {
                if (!SquaremapProvider.get().iconRegistry().hasEntry(CUSTOM_CLAN_BASE_KEY)) {
                    try {
                        SquaremapProvider.get().iconRegistry().register(CUSTOM_CLAN_BASE_KEY, squaremapHook.getClanImage(clan.getTag()));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Failed to register image for clan " + clan.getTag() + ": " + e.getMessage());
                    }
                }

                Key imageKey = Key.of("clanhome-" + clan.getTag());
                Icon iconMarker = Marker.icon(homePoint, imageKey, 32, 32);

                iconMarker.markerOptions(
                        MarkerOptions.builder()
                                .hoverTooltip(clan.getName() + " [" + clan.getTag() + "]" + "'s base")
                                .build());

                this.provider.addMarker(imageKey, iconMarker);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load image for clan " + clan.getTag() + ": " + e.getMessage());
                continue;
            }
        }
    }

    public void disable() {
        this.cancel();
        this.stop = true;
        this.provider.clearMarkers();
    }
}
