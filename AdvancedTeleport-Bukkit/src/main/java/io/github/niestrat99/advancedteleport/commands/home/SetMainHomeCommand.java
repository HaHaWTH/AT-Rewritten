package io.github.niestrat99.advancedteleport.commands.home;

import io.github.niestrat99.advancedteleport.api.ATPlayer;
import io.github.niestrat99.advancedteleport.api.Home;
import io.github.niestrat99.advancedteleport.commands.AsyncATCommand;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import io.github.niestrat99.advancedteleport.sql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetMainHomeCommand extends AbstractHomeCommand implements AsyncATCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            CustomMessages.sendMessage(sender, "Error.notAPlayer");
            return true;
        }
        if (!NewConfig.get().USE_HOMES.get()) {
            CustomMessages.sendMessage(sender, "Error.featureDisabled");
            return true;
        }
        if (!sender.hasPermission("at.member.setmainhome")) {
            CustomMessages.sendMessage(sender, "Error.noPermission");
            return true;
        }

        Player player = (Player) sender;
        ATPlayer atPlayer = ATPlayer.getPlayer(player);
        if (args.length == 0) {
            CustomMessages.sendMessage(sender, "Error.noHomeInput");
            return true;
        }
        if (args.length > 1 && sender.hasPermission("at.admin.setmainhome")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target != player) {
                ATPlayer atTarget = ATPlayer.getPlayer(target);
                String homeName = args[1];

                if (atTarget.hasHome(homeName)) {
                    atTarget.setMainHome(homeName, SQLManager.SQLCallback.getDefaultCallback(
                            sender, "Info.setMainHomeOther", "Error.setMainHomeFail", "{home}", homeName, "{player}", args[0]));
                } else {
                    atTarget.addHome(homeName, player.getLocation(), callback ->
                            atTarget.setMainHome(homeName, SQLManager.SQLCallback.getDefaultCallback(
                                    sender, "Info.setAndMadeMainHomeOther", "Error.setMainHomeFail", "{home}", homeName, "{player}", args[0])));
                }
                return true;
            }
        }
        String homeName = args[0];
        if (atPlayer.hasHome(homeName)) {
            Home home = atPlayer.getHome(homeName);
            if (atPlayer.canAccessHome(home)) {
                atPlayer.setMainHome(homeName, SQLManager.SQLCallback.getDefaultCallback(
                        sender, "Info.setMainHome", "Error.setMainHomeFail", "{home}", homeName));

            } else {
                CustomMessages.sendMessage(sender, "Error.noAccessHome", "{home}", home.getName());
            }
        } else {
            if (atPlayer.canSetMoreHomes()) {
                atPlayer.addHome(homeName, player.getLocation(), callback ->
                        atPlayer.setMainHome(homeName, SQLManager.SQLCallback.getDefaultCallback(
                                sender, "Info.setAndMadeMainHome", "Error.setMainHomeFail", "{home}", homeName)));
            }
        }


        return true;
    }
}
