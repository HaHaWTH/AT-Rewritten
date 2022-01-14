package io.github.niestrat99.advancedteleport.commands.home;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.api.Home;
import io.github.niestrat99.advancedteleport.api.events.ATTeleportEvent;
import io.github.niestrat99.advancedteleport.commands.AsyncATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.github.niestrat99.advancedteleport.managers.CooldownManager;
import io.github.niestrat99.advancedteleport.managers.MovementManager;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class HomeCommand extends AbstractHomeCommand implements AsyncATCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }
        if (!NewConfig.get().USE_HOMES.get()) {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }
        if (!sender.hasPermission("at.member.home")) {
            CustomMessages.sendMessage(sender, "Error.noPermission");
            return true;
        }

        Player player = (Player) sender;
        ATPlayer atPlayer = ATPlayer.getPlayer(player);

        HashMap<String, Home> homes = atPlayer.getHomes();
        if (MovementManager.getMovement().containsKey(player.getUniqueId())) {
            CustomMessages.sendMessage(player, "Error.onCountdown");
            return true;
        }
        int cooldown = CooldownManager.secondsLeftOnCooldown("home", player);
        if (cooldown > 0) {
            CustomMessages.sendMessage(sender, "Error.onCooldown", "{time}", String.valueOf(cooldown));
            return true;
        }

        if (args.length == 0) {
            if (atPlayer.hasMainHome()) {
                teleport(player, atPlayer.getMainHome());
            } else if (homes.size() == 1) {
                String name = homes.keySet().iterator().next();
                Home home = homes.get(name);
                if (atPlayer.canAccessHome(home)) {
                    teleport(player, home);
                } else {
                    CustomMessages.sendMessage(sender, "Error.noAccessHome", "{home}", home.getName());
                }
            } else if (NewConfig.get().ADD_BED_TO_HOMES.get()) {
                Home home = atPlayer.getBedSpawn();
                if (home == null) {
                    if (homes.isEmpty()) {
                        CustomMessages.sendMessage(sender, "Error.noHomes");
                    } else {
                        CustomMessages.sendMessage(sender, "Error.noHomeInput");
                    }
                    return true;
                }
                teleport(player, home);
            } else if (homes.isEmpty()) {
                CustomMessages.sendMessage(sender, "Error.noHomes");
            } else {
                CustomMessages.sendMessage(sender, "Error.noHomeInput");
            }
            return true;
        }
        if (args.length > 1 && sender.hasPermission("at.admin.home")) {
            ATPlayer.getPlayerFuture(args[0]).thenAccept(target -> {
                HashMap<String, Home> homesOther = target.getHomes();
                Home home;
                switch (args[1].toLowerCase()) {
                    case "bed":
                        if (NewConfig.get().ADD_BED_TO_HOMES.get()) {
                            home = target.getBedSpawn();
                            if (home == null) {
                                CustomMessages.sendMessage(player, "Error.noBedHomeOther", "{player}", args[0]);
                                return;
                            }
                        } else {
                            if (homesOther.containsKey(args[1])) {
                                home = homesOther.get(args[1]);
                            } else {
                                CustomMessages.sendMessage(sender, "Error.noSuchHome");
                                return;
                            }
                        }
                        break;
                    case "list":
                        Bukkit.getScheduler().runTask(CoreClass.getInstance(), () -> Bukkit.dispatchCommand(sender, "advancedteleport:homes " + args[0]));
                        return;
                    default:
                        if (homesOther.containsKey(args[1])) {
                            home = homesOther.get(args[1]);
                        } else {
                            CustomMessages.sendMessage(sender, "Error.noSuchHome");
                            return;
                        }
                }
                Bukkit.getScheduler().runTask(CoreClass.getInstance(), () -> {
                    PaperLib.teleportAsync(player, home.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
                    CustomMessages.sendMessage(sender, "Teleport.teleportingToHomeOther", "{player}", args[0], "{home}", args[1]);
                });
            });
        }

        Home home;
        if (atPlayer.getHomes().containsKey(args[0])) {
            home = atPlayer.getHomes().get(args[0]);
        } else if (args[0].equalsIgnoreCase("bed") && NewConfig.get().ADD_BED_TO_HOMES.get()) {
            home = atPlayer.getBedSpawn();
            if (home == null) {
                CustomMessages.sendMessage(player, "Error.noBedHome");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            Bukkit.getScheduler().runTask(CoreClass.getInstance(), () -> Bukkit.dispatchCommand(sender, "advancedteleport:homes " + args[0]));
            return true;
        } else {
            CustomMessages.sendMessage(sender, "Error.noSuchHome");
            return true;
        }

        if (atPlayer.canAccessHome(home)) {
            teleport(player, home);
        } else {
            CustomMessages.sendMessage(sender, "Error.noAccessHome", "{home}", home.getName());
        }
        return true;
    }

    public static void teleport(Player player, Home home) {
        Bukkit.getScheduler().runTask(CoreClass.getInstance(), () -> {
            ATTeleportEvent event = new ATTeleportEvent(
                    player,
                    home.getLocation(),
                    player.getLocation(),
                    home.getName(),
                    ATTeleportEvent.TeleportType.HOME
            );
            ATPlayer.getPlayer(player).teleport(event, "home", "Teleport.teleportingToHome", NewConfig.get().WARM_UPS.HOME.get());
        });
    }
}
