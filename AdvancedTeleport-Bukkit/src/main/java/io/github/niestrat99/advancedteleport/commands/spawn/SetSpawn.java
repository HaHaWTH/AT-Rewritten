package io.github.niestrat99.advancedteleport.commands.spawn;

import io.github.niestrat99.advancedteleport.api.AdvancedTeleportAPI;
import io.github.niestrat99.advancedteleport.commands.SpawnATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawn extends SpawnATCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }
        if (!NewConfig.get().USE_SPAWN.get()) {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }
        if (!sender.hasPermission("at.admin.setspawn")) {
            CustomMessages.sendMessage(sender, "Error.noPermission");
            return true;
        }

        Player player = (Player) sender;
        String name = player.getWorld().getName();
        String message = "Info.setSpawn";
        if (args.length > 0 && sender.hasPermission("at.admin.setspawn.other")) {
            if (!args[0].matches("^[0-9a-zA-Z_\\-]+$")) {
                CustomMessages.sendMessage(sender, "Error.nonAlphanumericSpawn");
                return false;
            }
            name = args[0];
            message = "Info.setSpawnSpecial";
        }
        String finalName = name;
        String finalMessage = message;
        AdvancedTeleportAPI.setSpawn(name, player, player.getLocation()).thenAcceptAsync(result ->
                CustomMessages.sendMessage(sender, finalMessage, "{spawn}", finalName));

        return true;
    }

    @Override
    public String getPermission() {
        return "at.admin.setspawn";
    }
}
