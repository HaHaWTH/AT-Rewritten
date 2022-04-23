package io.github.niestrat99.advancedteleport.utilities.nbt;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import net.kyori.adventure.nbt.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NBTReader {

    public static void getLocation(String name, NBTCallback<Location> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            try {
                Location location = getLocation(player);
                if (location == null) {
                    callback.onFail(CustomMessages.getString("Error.noOfflineLocation", "{player}", name));
                    return;
                }
                callback.onSuccess(location);
            } catch (IOException e) {
                callback.onFail(CustomMessages.getString("Error.failedOfflineTeleport", "{player}", name));
                e.printStackTrace();
            }
        });
    }

    public static void setLocation(String name, Location newLoc, NBTCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            try {
                setLocation(player, newLoc);
                callback.onSuccess(true);
            } catch (IOException e) {
                e.printStackTrace();
                callback.onFail(CustomMessages.getString("Error.failedOfflineTeleportHere", "{player}", name));
            }
        });
    }

    private static Location getLocation(OfflinePlayer player) throws IOException {
        UUID uuid = player.getUniqueId();
        File dataFile = getPlayerFile(uuid);

        if (dataFile == null) return null;
        CompoundBinaryTag tag = BinaryTagIO.reader().read(dataFile.toPath(), BinaryTagIO.Compression.GZIP);
        ListBinaryTag posTag = tag.getList("Pos");
        ListBinaryTag rotTag = tag.getList("Rotation");
        long worldUUIDMost = tag.getLong("WorldUUIDMost");
        long worldUUIDLeast = tag.getLong("WorldUUIDLeast");

        World world = Bukkit.getWorld(new UUID(worldUUIDMost, worldUUIDLeast));

        return new Location(world, posTag.getDouble(0), posTag.getDouble(1), posTag.getDouble(2),
                rotTag.getFloat(0), rotTag.getFloat(1));
    }

    private static File getPlayerFile(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            File worldFolder = world.getWorldFolder();
            if (!worldFolder.isDirectory()) continue;
            File[] children = worldFolder.listFiles();
            if (children == null) continue;
            for (File file : children) {
                if (!file.isDirectory() || !file.getName().equals("playerdata")) continue;
                return getPlayerFile(file, uuid);
            }
        }
        return null;
    }

    private static File getPlayerFile(File playerDataFolder, UUID uuid) {
        File[] files = playerDataFolder.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.getName().equals(uuid.toString() + ".dat")) return file;
        }
        return null;
    }

    private static void setLocation(OfflinePlayer player, Location location) throws IOException {
        UUID uuid = player.getUniqueId();
        File dataFile = getPlayerFile(uuid);

        if (dataFile == null) return;
        CompoundBinaryTag tag = BinaryTagIO.reader().read(dataFile.toPath(), BinaryTagIO.Compression.GZIP);
        ListBinaryTag posTag = ListBinaryTag.empty();
        posTag.add(DoubleBinaryTag.of(location.getX()));
        posTag.add(DoubleBinaryTag.of(location.getY()));
        posTag.add(DoubleBinaryTag.of(location.getZ()));

        ListBinaryTag rotTag = ListBinaryTag.empty();
        rotTag.add(FloatBinaryTag.of(location.getYaw()));
        rotTag.add(FloatBinaryTag.of(location.getPitch()));

        tag.put("Pos", posTag);
        tag.put("Rotation", rotTag);

        BinaryTagIO.writer().write(tag, dataFile.toPath());
    }

    public interface NBTCallback<D> {

        void onSuccess(D data);

        default void onFail(String message) {}
    }
}
