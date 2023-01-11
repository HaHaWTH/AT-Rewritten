package io.github.niestrat99.advancedteleport.listeners;

import io.github.niestrat99.advancedteleport.api.Warp;
import io.github.niestrat99.advancedteleport.api.events.warps.WarpDeleteEvent;
import io.github.niestrat99.advancedteleport.api.events.warps.WarpMoveEvent;
import io.github.niestrat99.advancedteleport.api.events.warps.WarpPostCreateEvent;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.github.niestrat99.advancedteleport.managers.PluginHookManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MapEventListeners implements Listener {

    @EventHandler
    public void onWarpAdd(WarpPostCreateEvent event) {

        // If warps aren't meant to be put on maps, stop there
        if (!NewConfig.get().MAP_WARPS.isEnabled()) return;

        // Add the warp to each map plugin.
        Warp warp = event.getWarp();
        PluginHookManager.get().getMapPlugins().values().forEach(mapPlugin -> {
            if (!mapPlugin.canEnable()) return;
            mapPlugin.addWarp(warp);
        });
    }

    @EventHandler
    public void onWarpRemove(WarpDeleteEvent event) {

        // Regardless of whether warps should be on maps,
        PluginHookManager.get().getMapPlugins().values().forEach(mapPlugin -> {
            if (!mapPlugin.canEnable()) return;
            mapPlugin.removeWarp(event.getWarp());
        });
    }

    @EventHandler
    public void onWarpMove(WarpMoveEvent event) {

        // If warps aren't meant to be put on maps, stop there.
        if (!NewConfig.get().MAP_WARPS.isEnabled()) return;

        // Move the warp accordingly.
        PluginHookManager.get().getMapPlugins().values().forEach(mapPlugin -> {
            if (!mapPlugin.canEnable()) return;
            mapPlugin.moveWarp(event.getWarp());
        });
    }
}
