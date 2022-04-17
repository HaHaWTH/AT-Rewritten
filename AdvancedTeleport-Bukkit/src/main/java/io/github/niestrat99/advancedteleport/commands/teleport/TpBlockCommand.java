package io.github.niestrat99.advancedteleport.commands.teleport;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.commands.AsyncATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpBlockCommand implements AsyncATCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s,
                             @NotNull String[] args) {
        // If teleporting features are enabled...
        if (NewConfig.get().USE_BASIC_TELEPORT_FEATURES.get()) {
            // If the user has permission...
            if (sender.hasPermission("at.member.block")) {
                // If the sender is a player...
                if (sender instanceof Player){
                    // Get the sender as a player.
                    Player player = (Player)sender;
                    // Make sure we've included a player name.
                    if (args.length>0){
                        // Don't block ourselves lmao
                        if (args[0].equalsIgnoreCase(player.getName())){
                            CustomMessages.sendMessage(sender, "Error.blockSelf");
                            return true;
                        }
                        ATPlayer atPlayer = ATPlayer.getPlayer(player);
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
                                atPlayer.blockUser(target, reason.toString().trim()).thenAcceptAsync(result ->
                                        CustomMessages.sendMessage(sender, result ? "Info.blockPlayer" : "Error.blockFail",
                                        "{player}", args[0]));
                            } else {
                                atPlayer.blockUser(target).thenAcceptAsync(result ->
                                        CustomMessages.sendMessage(sender, result ? "Info.blockPlayer" : "Error.blockFail",
                                                "{player}", args[0]));
                            }

                        });
                    } else {
                        CustomMessages.sendMessage(sender, "Error.noPlayerInput");
                    }
                    return true;
                } else {
                    CustomMessages.sendMessage(sender, "Error.notAPlayer");
                }
            }
        } else {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }
        return true;
    }
}
