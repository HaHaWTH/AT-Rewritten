package io.github.niestrat99.advancedteleport.commands.warp;

import io.github.niestrat99.advancedteleport.api.ATFloodgatePlayer;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.api.AdvancedTeleportAPI;
import io.github.niestrat99.advancedteleport.api.Warp;
import io.github.niestrat99.advancedteleport.api.events.ATTeleportEvent;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.github.niestrat99.advancedteleport.managers.CooldownManager;
import io.github.niestrat99.advancedteleport.managers.MovementManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WarpCommand extends AbstractWarpCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
                             @NotNull String[] args) {
        if (!canProceed(sender)) return true;
        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            ATPlayer atPlayer = ATPlayer.getPlayer(player);
            if (atPlayer instanceof ATFloodgatePlayer && NewConfig.get().USE_FLOODGATE_FORMS.get()) {
                ((ATFloodgatePlayer) atPlayer).sendWarpForm();
            } else {
                CustomMessages.sendMessage(sender, "Error.noWarpInput");
            }
            return true;
        }

        int cooldown = CooldownManager.secondsLeftOnCooldown("warp", player);
        if (cooldown > 0) {
            CustomMessages.sendMessage(sender, "Error.onCooldown", "{time}", String.valueOf(cooldown));
            return true;
        }
        if (AdvancedTeleportAPI.getWarps().containsKey(args[0])) {
            if (MovementManager.getMovement().containsKey(player.getUniqueId())) {
                CustomMessages.sendMessage(player, "Error.onCountdown");
                return true;
            }
            Warp warp = AdvancedTeleportAPI.getWarps().get(args[0]);
            warp(warp, player);
        } else {
            CustomMessages.sendMessage(sender, "Error.noSuchWarp");
        }
        return true;
    }

    public static void warp(Warp warp, Player player) {
        boolean found = player.hasPermission("at.member.warp.*");
        if (player.isPermissionSet("at.member.warp." + warp.getName().toLowerCase())) {
            found = player.hasPermission("at.member.warp." + warp.getName().toLowerCase());
        }
        if (!found) {
            CustomMessages.sendMessage(player, "Error.noPermissionWarp", "{warp}", warp.getName());
            return;
        }
        ATTeleportEvent event = new ATTeleportEvent(player, warp.getLocation(), player.getLocation(), warp.getName(), ATTeleportEvent.TeleportType.WARP);
        ATPlayer.getPlayer(player).teleport(event, "warp", "Teleport.teleportingToWarp");
    }

    @Override
    public String getPermission() {
        return "at.member.warp";
    }
}
