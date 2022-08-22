package io.github.niestrat99.advancedteleport.commands.home;

import io.github.niestrat99.advancedteleport.api.ATFloodgatePlayer;
import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DelHomeCommand extends AbstractHomeCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!canProceed(sender))
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (sender.hasPermission("at.admin.delhome")) {
                    if (args.length > 1) {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                        delHome(target, player, args[1]);
                        return true;
                    }
                }
                delHome(player, args[0]);
            } else {
                CustomMessages.sendMessage(sender, "Error.noHomeInput");
            }
        } else {
            ATPlayer atPlayer = ATPlayer.getPlayer(player);
            if (atPlayer instanceof ATFloodgatePlayer && NewConfig.get().USE_FLOODGATE_FORMS.get()) {
                ((ATFloodgatePlayer) atPlayer).sendDeleteHomeForm();
            } else {
                CustomMessages.sendMessage(sender, "Error.noHomeInput");
            }
        }
        return true;
    }

    private void delHome(OfflinePlayer player, Player sender, String name) {
        ATPlayer atPlayer = ATPlayer.getPlayer(player);

        if (!atPlayer.hasHome(name)) {
            CustomMessages.sendMessage(sender, "Error.noSuchHome");
            return;
        }

        atPlayer.removeHome(name, sender).thenAccept(result ->
                CustomMessages.sendMessage(sender, result ? (sender.getUniqueId() == player.getUniqueId() ?
                                "Info.deletedHome" : "Info.deletedHomeOther") : "Error.deleteHomeFail",
                        "{home}", name, "{player}", player.getName()));
    }

    private void delHome(Player player, String name) {
        delHome(player, player, name);
    }

    @Override
    public String getPermission() {
        return "at.member.delhome";
    }
}
