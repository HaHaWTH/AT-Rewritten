package io.github.niestrat99.advancedteleport.commands.teleport;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.api.events.ATTeleportEvent;
import io.github.niestrat99.advancedteleport.commands.ATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.github.niestrat99.advancedteleport.managers.CooldownManager;
import io.github.niestrat99.advancedteleport.managers.MovementManager;
import io.github.niestrat99.advancedteleport.managers.RTPManager;
import io.github.niestrat99.advancedteleport.payments.PaymentManager;
import io.github.niestrat99.advancedteleport.utilities.ConditionChecker;
import io.github.niestrat99.advancedteleport.utilities.RandomTPAlgorithms;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Tpr implements ATCommand {

    private static final List<UUID> searchingPlayers = new ArrayList<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<Player> players;
        if (!NewConfig.get().USE_RANDOMTP.get()) {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }
        if (!sender.hasPermission("at.member.tpr")) {
            CustomMessages.sendMessage(sender, "Error.noPermission");
            return true;
        }
        if (args.length > 1 && sender.hasPermission("at.admin.tpr.other")) {
            if (sender.hasPermission("at.admin.tpr.other.selector")) {
                players = new ArrayList<>();
                for (Entity e : Bukkit.selectEntities(sender, args[1])) {
                if (e instanceof Player) {
                    players.add((Player) e);
                }
            } else {
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    CustomMessages.sendMessage(sender, "Error.noSuchPlayer");
                    return true;
                }
                players = Collections.singletonList(player);
            }
            if (players.isEmpty()) {
                CustomMessages.sendMessage(sender, "Error.noSuchPlayer");
                return true;
            }
        } else if (sender instanceof Player) {
            players = Collections.singletonList((Player) sender);
        } else {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }

        for (Player player : players) {
            if (MovementManager.getMovement().containsKey(player.getUniqueId())) {
                CustomMessages.sendMessage(player, "Error.onCountdown");
                continue;
            }
            World world = player.getWorld();
            if (args.length > 0 && sender.hasPermission("at.member.tpr.other")) {
                World otherWorld = Bukkit.getWorld(args[0]);
                if (otherWorld != null) {
                    world = otherWorld;
                } else {
                    CustomMessages.sendMessage(sender, "Error.noSuchWorld");
                    return true;
                }
            }
            randomTeleport(player, sender, world);
        }
    }

    public static boolean randomTeleport(Player player, World world) {
        return randomTeleport(player, player, world);
    }

    public static boolean randomTeleport(Player player, CommandSender sender, World world) {
        int cooldown = CooldownManager.secondsLeftOnCooldown("tpr", player);
        if (cooldown > 0) {
            CustomMessages.sendMessage(sender, "Error.onCooldown", "{time}", String.valueOf(cooldown));
            return true;
        }
        if (NewConfig.get().WHITELIST_WORLD.get()) {
            List<String> allowedWorlds = NewConfig.get().ALLOWED_WORLDS.get();
            if (!allowedWorlds.contains(world.getName())) {
                if (!sender.hasPermission("at.admin.rtp.bypass-world")) {
                    if (allowedWorlds.isEmpty() || !NewConfig.get().REDIRECT_TO_WORLD.get()) {
                        CustomMessages.sendMessage(sender, "Error.cantTPToWorld");
                        return true;
                    } else {
                        for (String worldName : allowedWorlds) {
                            world = Bukkit.getWorld(worldName);
                            String conditionResult = ConditionChecker.canTeleport(new Location(player.getWorld(), 0, 0, 0), new Location(world, 0, 0, 0), "tpr", player);
                            if (world != null && conditionResult.isEmpty()) break;
                        }
                        if (world == null) {
                            CustomMessages.sendMessage(sender, "Error.cantTPToWorld");
                            return true;
                        }
                    }
                }
            }
        }

        if (searchingPlayers.contains(player.getUniqueId())) {
            CustomMessages.sendMessage(sender, "Error.alreadySearching");
            return true;
        }

        String conditionResult = ConditionChecker.canTeleport(new Location(player.getWorld(), 0, 0, 0), new Location(world, 0, 0, 0), "tpr", player);
        if (!conditionResult.isEmpty()) {
            CustomMessages.sendMessage(player, conditionResult, "{world}", world.getName());
            return true;
        }

        if (!PaymentManager.getInstance().canPay("tpr", player)) return false;

        if (NewConfig.get().RAPID_RESPONSE.get() && PaperLib.isPaper()) {
            Location nextLoc = RTPManager.getLocationUrgently(world);
            if (nextLoc != null) {
                ATPlayer atPlayer = ATPlayer.getPlayer(player);
                ATTeleportEvent event = new ATTeleportEvent(player, nextLoc, player.getLocation(), "", ATTeleportEvent.TeleportType.TPR);
                atPlayer.teleport(event, "tpr", "Teleport.teleportingToRandomPlace", NewConfig.get().WARM_UPS.TPR.get());
            } else {
                CustomMessages.sendMessage(player, "Info.searching");
                searchingPlayers.add(player.getUniqueId());
                RTPManager.getNextAvailableLocation(world).thenAccept(location -> {
                    searchingPlayers.remove(player.getUniqueId());
                    ATPlayer atPlayer = ATPlayer.getPlayer(player);
                    ATTeleportEvent event = new ATTeleportEvent(player, location, player.getLocation(), "", ATTeleportEvent.TeleportType.TPR);
                    atPlayer.teleport(event, "tpr", "Teleport.teleportingToRandomPlace", NewConfig.get().WARM_UPS.TPR.get());
                });
            }
        } else {
            CustomMessages.sendMessage(player, "Info.searching");
            searchingPlayers.add(player.getUniqueId());
            RandomTPAlgorithms.getAlgorithms().get("binary").fire(player, world, location -> Bukkit.getScheduler().runTask(CoreClass.getInstance(), () -> {
                searchingPlayers.remove(player.getUniqueId());
                ATPlayer atPlayer = ATPlayer.getPlayer(player);
                ATTeleportEvent event = new ATTeleportEvent(player, location, player.getLocation(), "", ATTeleportEvent.TeleportType.TPR);
                atPlayer.teleport(event, "tpr", "Teleport.teleportingToRandomPlace", NewConfig.get().WARM_UPS.TPR.get());
            }));
        }

        return true;
    }
}
