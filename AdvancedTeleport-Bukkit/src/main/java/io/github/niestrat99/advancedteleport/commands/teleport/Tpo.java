package io.github.niestrat99.advancedteleport.commands.teleport;

import io.github.niestrat99.advancedteleport.commands.ATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class Tpo implements ATCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }
        if (!NewConfig.get().USE_BASIC_TELEPORT_FEATURES.get()) {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }

        if (!sender.hasPermission("at.admin.tpo")) {
            CustomMessages.sendMessage(sender, "Error.noPermission");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            CustomMessages.sendMessage(sender, "Error.noPlayerInput");
            return true;
        }
        if (args[0].equalsIgnoreCase(player.getName())) {
            CustomMessages.sendMessage(sender, "Error.requestSentToSelf");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            CustomMessages.sendMessage(sender, "Error.noSuchPlayer");
        } else {
            CustomMessages.sendMessage(sender, "Teleport.teleporting", "{player}", target.getName());
            PaperLib.teleportAsync(player, target.getLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        }
        return true;
    }
}
