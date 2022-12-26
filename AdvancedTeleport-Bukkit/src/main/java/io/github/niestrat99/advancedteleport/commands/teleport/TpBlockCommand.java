package io.github.niestrat99.advancedteleport.commands.teleport;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.ATFloodgatePlayer;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.commands.TeleportATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpBlockCommand extends TeleportATCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
                             @NotNull String[] args) {
        if (!canProceed(sender)) return true;
        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }

        Player player = (Player) sender;
        ATPlayer atPlayer = ATPlayer.getPlayer(player);

        if (args.length == 0) {
            if (atPlayer instanceof ATFloodgatePlayer && NewConfig.get().USE_FLOODGATE_FORMS.get()) {
                ((ATFloodgatePlayer) atPlayer).sendBlockForm();
            } else {
                CustomMessages.sendMessage(sender, "Error.noPlayerInput");
            }
            return true;
        }
        // Don't block ourselves lmao
        if (args[0].equalsIgnoreCase(player.getName())) {
            CustomMessages.sendMessage(sender, "Error.blockSelf");
            return true;
        }
        // Must be async due to searching for offline player
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            //
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

            if (atPlayer.hasBlocked(target)) {
                CustomMessages.sendMessage(sender, "Error.alreadyBlocked");
                return;
            }

            if (args.length > 1) {
                StringBuilder reason = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    reason.append(args[i]).append(" ");
                }
                atPlayer.blockUser(target, reason.toString().trim()).handle((x, e) -> {
                    if (e != null) e.printStackTrace();

                    CustomMessages.sendMessage(sender, e == null ? "Info.blockPlayer" : "Error.blockFail",
                            "{player}", args[0]);
                    return x;
                });
            } else {
                atPlayer.blockUser(target).handle((x, e) -> {
                    if (e != null) e.printStackTrace();

                    CustomMessages.sendMessage(sender, e == null ? "Info.blockPlayer" : "Error.blockFail",
                            "{player}", args[0]);
                    return x;
                });
            }
        });
        return true;
    }

    @Override
    public String getPermission() {
        return "at.member.block";
    }
}
