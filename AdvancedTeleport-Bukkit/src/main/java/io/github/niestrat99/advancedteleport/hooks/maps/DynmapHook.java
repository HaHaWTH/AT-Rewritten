package io.github.niestrat99.advancedteleport.hooks.maps;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.Home;
import io.github.niestrat99.advancedteleport.api.Warp;
import io.github.niestrat99.advancedteleport.config.Spawn;
import io.github.niestrat99.advancedteleport.hooks.MapPlugin;
import io.github.niestrat99.advancedteleport.managers.MapAssetManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

public class DynmapHook extends MapPlugin {

    private DynmapAPI api;
    private MarkerAPI markerAPI;

    private MarkerSet WARPS;
    private MarkerSet HOMES;
    private MarkerSet SPAWNS;

    private HashMap<String, MarkerIcon> icons;

    @Override
    public boolean canEnable() {
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        return dynmap != null && dynmap.isEnabled();
    }

    @Override
    public void enable() {
        CoreClass.getInstance().getLogger().info("Found Dynmap, hooking...");
        icons = new HashMap<>();
        api = (DynmapAPI) Bukkit.getPluginManager().getPlugin("dynmap");
        if (api == null) throw new NoClassDefFoundError("You fool.");
        markerAPI = api.getMarkerAPI();
        // Create the warps
        WARPS = getSet("advancedteleport_warps", "Warps");
        // Create the homes
        HOMES = getSet("advancedteleport_homes", "Homes");
        // Create the spawns
        SPAWNS = getSet("advancedteleport_spawns", "Spawns");
    }

    @Override
    public void addWarp(Warp warp) {
        addMarker(warp.getName(), "warp", WARPS, warp.getLocation(), null);
    }

    @Override
    public void addHome(Home home) {
        addMarker(home.getOwner() + "_" + home.getName(), "home", HOMES, home.getLocation(), home.getOwner());
    }

    @Override
    public void addSpawn(String name, Location location) {
        addMarker(name, "spawn", SPAWNS, location, null);
    }

    @Override
    public void removeWarp(Warp warp) {
        removeMarker("advancedteleport_warp_" + warp.getName(), WARPS);
    }

    @Override
    public void removeHome(Home home) {
        removeMarker("advancedteleport_home_" + home.getOwner() + "_" + home.getName(), HOMES);
    }

    @Override
    public void removeSpawn(String name) {
        removeMarker("advancedteleport_spawn_" + name, SPAWNS);
    }

    @Override
    public void moveWarp(Warp warp) {
        moveMarker(warp.getName(), "warp", WARPS, warp.getLocation(), null);
    }

    @Override
    public void moveHome(Home home) {
        moveMarker(home.getOwner() + "_" + home.getName(), "home", HOMES, home.getLocation(), home.getOwner());
    }

    @Override
    public void moveSpawn(String name, Location location) {
        moveMarker(name, "spawn", SPAWNS, Spawn.get().getSpawn(name), null);
    }

    @Override
    public void registerImage(String name, InputStream stream) {
        MarkerIcon icon = markerAPI.getMarkerIcon(name);
        if (icon == null) {
            icon = markerAPI.createMarkerIcon(name, name, stream);
        }
        icons.put(name, icon);
    }

    private void addMarker(String name, String type, MarkerSet set, Location location, UUID owner) {
        MapAssetManager.getIconInfo(name, type, owner).handleAsync((result, e) -> {
            if (e != null) {
                e.printStackTrace();
            }
            return result;
        }).thenAcceptAsync(key -> {
            MarkerIcon icon = icons.get(key.getImageKey());
            if (icon == null) icon = markerAPI.getMarkerIcon(key.getImageKey());
            if (icon == null) icon = markerAPI.getMarkerIcon("default");

            set.createMarker("advancedteleport_" + type + "_" + name, key.getHoverTooltip(), true, location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), icon, false);
            CoreClass.getInstance().getLogger().info("Marker: " + icon.getMarkerIconID() + ", " + key.getImageKey());
        }, task -> Bukkit.getScheduler().runTask(CoreClass.getInstance(), task));
    }

    private void removeMarker(String name, MarkerSet set) {
        for (Marker marker : set.getMarkers()) {
            if (marker.getMarkerID().equals(name)) {
                marker.deleteMarker();
            }
        }
    }

    private void moveMarker(String name, String type, MarkerSet set, Location location, UUID owner) {
        removeMarker(name, set);
        addMarker(name, type, set, location, owner);
    }

    private MarkerSet getSet(String id, String label) {
        MarkerSet set = markerAPI.getMarkerSet(id);
        if (set == null) set = markerAPI.createMarkerSet(id, label, null, false);
        return set;
    }
}
