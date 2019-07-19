package io.github.at.commands.teleport;

import io.github.at.config.Config;
import io.github.at.config.CustomMessages;
import io.github.at.config.LastLocations;
import io.github.at.events.TeleportTrackingManager;
import io.github.at.utilities.DistanceLimiter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Back implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (Config.isFeatureEnabled("teleport")) {
            if (sender.hasPermission("teleport")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Location loc = TeleportTrackingManager.getLastLocation(player);
                    if (loc == null) {
                        loc = LastLocations.getLocation(player);
                        if (loc == null) {
                            sender.sendMessage(CustomMessages.getString("Error.noLocation"));
                            return false;
                        }
                    }
                    while (loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.WATER) {
                        loc.add(0.0, 1.0, 0.0);
                    }
                    if (!DistanceLimiter.canTeleport(player.getLocation(), loc, "back") && !player.hasPermission("at.admin.bypass.distance-limit")) {
                        player.sendMessage(CustomMessages.getString("Error.tooFarAway"));
                        return false;
                    }
                    player.teleport(loc);
                    player.sendMessage(CustomMessages.getString("Teleport.teleportingToLastLoc"));
                } else {
                    sender.sendMessage(CustomMessages.getString("Error.notAPlayer"));
                }
            } else {
                sender.sendMessage(CustomMessages.getString("Error.noPermission"));
                return false;
            }
        } else {
            sender.sendMessage(CustomMessages.getString("Error.featureDisabled"));
            return false;
        }
        return false;
    }
}
