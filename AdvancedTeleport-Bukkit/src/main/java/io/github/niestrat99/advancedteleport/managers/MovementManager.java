package io.github.niestrat99.advancedteleport.managers;

import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.MainConfig;
import io.github.niestrat99.advancedteleport.folia.CancellableRunnable;
import io.github.niestrat99.advancedteleport.folia.RunnableManager;
import io.github.niestrat99.advancedteleport.payments.PaymentManager;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class MovementManager implements Listener {

    private static final HashMap<UUID, ImprovedRunnable> movement = new HashMap<>();

    @EventHandler
    public void onMovement(PlayerMoveEvent event) {
        boolean cancelOnRotate = MainConfig.get().CANCEL_WARM_UP_ON_ROTATION.get();
        boolean cancelOnMove = MainConfig.get().CANCEL_WARM_UP_ON_MOVEMENT.get();
        if (!cancelOnRotate) {
            Location locTo = event.getTo();
            Location locFrom = event.getFrom();
            if (locTo.getBlockX() == locFrom.getBlockX() // If the player rotated instead of moved
                    && locTo.getBlockY() == locFrom.getBlockY()
                    && locTo.getBlockZ() == locFrom.getBlockZ()) {
                return;
            }
        }
        UUID uuid = event.getPlayer().getUniqueId();
        if ((cancelOnRotate || cancelOnMove) && movement.containsKey(uuid)) {
            ImprovedRunnable timer = movement.get(uuid);
            timer.runnable.cancel();
            CustomMessages.sendMessage(event.getPlayer(), "Teleport.eventMovement");
            ParticleManager.removeParticles(event.getPlayer(), timer.command);
            movement.remove(uuid);
        }
    }

    public static HashMap<UUID, ImprovedRunnable> getMovement() {
        return movement;
    }

    public static void createMovementTimer(
        Player teleportingPlayer,
        Location location,
        String command,
        String message,
        int warmUp,
        TagResolver... placeholders
    ) {
        createMovementTimer(teleportingPlayer, location, command, message, warmUp, teleportingPlayer, placeholders);
    }

    public static void createMovementTimer(
        Player teleportingPlayer,
        Location location,
        String command,
        String message,
        int warmUp,
        Player payingPlayer,
        TagResolver... placeholders
    ) {
        UUID uuid = teleportingPlayer.getUniqueId();

        // When this config is enabled the teleporting player will receive a blindness effect until it gets teleported.
        if (MainConfig.get().BLINDNESS_ON_WARMUP.get()) {
            teleportingPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, warmUp * 20 + 20, 0, false, false));
        }

        // Apply the plugin particles.
        ParticleManager.applyParticles(teleportingPlayer, command);

        // Starts the movement checker.
        ImprovedRunnable runnable = new ImprovedRunnable(command,
                RunnableManager.setupRunnerDelayed(teleportingPlayer, task -> {

                    // If the player can't pay for the
                    if (!PaymentManager.getInstance().canPay(command, payingPlayer)) return;
                    ParticleManager.onTeleport(teleportingPlayer, command);
                    PaperLib.teleportAsync(teleportingPlayer, location, PlayerTeleportEvent.TeleportCause.COMMAND);
                    movement.remove(uuid);
                    CustomMessages.sendMessage(teleportingPlayer, message, placeholders);
                    PaymentManager.getInstance().withdraw(command, payingPlayer);

                    // If the cooldown is to be applied after only after a teleport takes place, apply it now
                    if (MainConfig.get().APPLY_COOLDOWN_AFTER.get().equalsIgnoreCase("teleport")) {
                        CooldownManager.addToCooldown(command, payingPlayer);
                    }
                }, () -> {}, warmUp * 20L));
        movement.put(uuid, runnable);
        if (MainConfig.get().CANCEL_WARM_UP_ON_MOVEMENT.get() || MainConfig.get().CANCEL_WARM_UP_ON_ROTATION.get()) {
            CustomMessages.sendMessage(teleportingPlayer, "Teleport.eventBeforeTP", Placeholder.unparsed("countdown", String.valueOf(warmUp)));
        } else {
            CustomMessages.sendMessage(teleportingPlayer, "Teleport.eventBeforeTPMovementAllowed", Placeholder.unparsed("countdown", String.valueOf(warmUp)));
        }

    }

    public static class ImprovedRunnable {

        private final String command;
        private final @NotNull CancellableRunnable runnable;

        ImprovedRunnable(String command, @NotNull CancellableRunnable runnable) {
            this.command = command;
            this.runnable = runnable;
        }

        public String getCommand() {
            return command;
        }
    }
}
