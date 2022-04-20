package io.github.niestrat99.advancedteleport.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Represents
 */
public class BlockInfo {

    private final UUID receiverUUID;
    private final UUID blockedUUID;
    private String reason;
    private final long time;
    private final String formattedTime;

    public BlockInfo(UUID receiver, UUID blocked, String reason, long time) {
        receiverUUID = receiver;
        blockedUUID = blocked;
        this.reason = reason;
        this.time = time;

        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        formattedTime = format.format(date);
    }

    public OfflinePlayer getReceivingPlayer() {
        return Bukkit.getOfflinePlayer(receiverUUID);
    }

    public OfflinePlayer getBlockedPlayer() {
        return Bukkit.getOfflinePlayer(blockedUUID);
    }

    public UUID getReceiverUUID() {
        return receiverUUID;
    }

    public long getTime() {
        return time;
    }

    public UUID getBlockedUUID() {
        return blockedUUID;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getFormattedTime() {
        return formattedTime;
    }
}
