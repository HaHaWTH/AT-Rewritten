package io.github.niestrat99.advancedteleport.config;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.thatsmusic99.configurationmaster.api.ConfigSection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.bukkit.util.NumberConversions.square;

public class Spawn extends ATConfig {

    private static Spawn instance;
    private Location mainSpawn;

    public Spawn() throws IOException {
        super("spawn.yml");
        instance = this;
    }

    @Override
    public void loadDefaults() {
        addDefault("main-spawn", "");
        makeSectionLenient("spawns");
    }

    @Override
    public void moveToNew() {
        String defaultName = getString("spawnpoint.world");
        if (defaultName == null || defaultName.isEmpty()) return;
        moveTo("spawnpoint.x", "spawns." + defaultName + ".x");
        moveTo("spawnpoint.y", "spawns." + defaultName + ".y");
        moveTo("spawnpoint.z", "spawns." + defaultName + ".z");
        moveTo("spawnpoint.world", "spawns." + defaultName + ".world");
        moveTo("spawnpoint.yaw", "spawns." + defaultName + ".yaw");
        moveTo("spawnpoint.pitch", "spawns." + defaultName + ".pitch");
        set("main-spawn", defaultName);
    }

    @Override
    public void postSave() {
        String mainSpawn = getString("main-spawn", "");
        ConfigSection spawns = getConfigSection("spawns");
        if (spawns == null) return;
        if (!spawns.contains(mainSpawn) || mainSpawn.isEmpty()) return;
        this.mainSpawn = new Location(Bukkit.getWorld(getString("spawns." + mainSpawn + ".world")),
                getDouble("spawns." + mainSpawn + ".x"),
                getDouble("spawns." + mainSpawn + ".y"),
                getDouble("spawns." + mainSpawn + ".z"),
                getFloat("spawns." + mainSpawn + ".yaw"),
                getFloat("spawns." + mainSpawn + ".pitch"));
    }

    public void setSpawn(Location location, String name) throws IOException {
        set("spawns." + name + ".x", location.getX());
        set("spawns." + name + ".y", location.getY());
        set("spawns." + name + ".z", location.getZ());
        set("spawns." + name + ".world", location.getWorld().getName());
        set("spawns." + name + ".yaw", location.getYaw());
        set("spawns." + name + ".pitch", location.getPitch());
        set("spawns." + name + ".mirror", null);
        save();
        if (mainSpawn == null) {
            setMainSpawn(name, location);
        }
    }

    public String mirrorSpawn(String from, String to) {
        ConfigSection section = getConfigSection("spawns");
        String mirror = to;
        if (section == null || !section.contains(to)) return "Error.noSuchSpawn";
        ConfigSection toSection = section.getConfigSection(to);
        while (true) {
            if (toSection == null) return "Error.noSuchSpawn";
            String alternateMirror = toSection.getString("mirror");
            if (alternateMirror != null && !alternateMirror.isEmpty() && !alternateMirror.equals(mirror)) {
                mirror = alternateMirror;
                toSection = section.getConfigSection(alternateMirror);
            } else if (toSection.contains("x")
                    && toSection.contains("y")
                    && toSection.contains("z")
                    && toSection.contains("yaw")
                    && toSection.contains("pitch")) {
                set("spawns." + from + ".mirror", mirror);
                set("spawns." + from + ".requires-permission", false);
                try {
                    save();
                } catch (IOException e) {
                    CoreClass.getInstance().getLogger().severe("Failed to mirror spawn from " + from + " to " + to + ": " + e.getMessage());
                    return "Error.mirrorSpawnFail";
                }
                return "Info.mirroredSpawn";
            }
        }
    }

    public Location getSpawn(String name) {
        return getSpawn(name, null, false, true);
    }

    public Location getSpawn(
            @NotNull String name,
            @Nullable Player player,
            boolean bypassPermission,
            boolean forced
    ) {

        // If we're going to the nearest spawnpoint instead, look around
        if (player != null && NewConfig.get().TELEPORT_TO_NEAREST_SPAWN.get() && !forced) {
            return getNearestLocation(player);
        }

        ConfigSection spawns = getConfigSection("spawns");
        ConfigSection toSection = spawns.getConfigSection(name);
        while (true) {
            if (toSection != null) {
                String priorName = toSection.getString("mirror");
                boolean requiresPermission = toSection.getBoolean("requires-permission", true);
                // Just to note, "requires permission" indicates that the player can teleport to the spawn itself. Not the mirrored one.
                boolean hasCoords = toSection.contains("x")
                        && toSection.contains("y")
                        && toSection.contains("z")
                        && toSection.contains("yaw")
                        && toSection.contains("pitch")
                        && toSection.contains("world");
                boolean hasMirror = priorName != null && !priorName.isEmpty() && !priorName.equals(name);
                if (hasCoords) {
                    if (hasMirror && (requiresPermission
                            && !player.hasPermission("at.member.spawn." + name)
                            && !bypassPermission)) {
                        name = priorName;
                        toSection = spawns.getConfigSection(name);
                    } else {
                        return new Location(Bukkit.getWorld(toSection.getString("world")),
                                toSection.getDouble("x"),
                                toSection.getDouble("y"),
                                toSection.getDouble("z"),
                                (float) toSection.getDouble("yaw"),
                                (float) toSection.getDouble("pitch"));
                    }
                } else {
                    if (hasMirror) {
                        name = priorName;
                        toSection = spawns.getConfigSection(name);
                    } else {
                        break;
                    }
                }
            } else {
                String mainSpawn = getString("main-spawn");
                if (mainSpawn == null || mainSpawn.equals(name)) break;
                toSection = spawns.getConfigSection(mainSpawn);
                name = mainSpawn;
            }
        }
        return mainSpawn;
    }

    private Location getNearestLocation(@NotNull Player player) {

        final Location loc = player.getLocation();

        // Set the initial values
        @Nullable String chosenSpawn = null;
        double max = 0;

        ConfigSection spawns = getConfigSection("spawns");
        for (String spawnName : getSpawns()) {
            ConfigSection spawn = spawns.getConfigSection(spawnName);

            // Same world?
            if (!player.getWorld().getName().equals(spawn.getString("world"))) continue;

            // Not a mirror?
            boolean hasCoords = spawn.contains("x")
                    && spawn.contains("y")
                    && spawn.contains("z")
                    && spawn.contains("yaw")
                    && spawn.contains("pitch")
                    && spawn.contains("world");
            if (!hasCoords) continue;

            // Has permission?
            boolean requiresPermission = spawn.getBoolean("requires-permission", true);
            if (requiresPermission && !player.hasPermission("at.member.spawn." + spawnName.toLowerCase())) continue;

            // Check the distance
            double x = spawn.getDouble("x");
            double y = spawn.getDouble("y");
            double z = spawn.getDouble("z");
            double distance = square(x - loc.getX()) + square(y - loc.getY()) + square(z - loc.getZ());
            if (chosenSpawn != null && distance >= max) continue;

            // If it works out, set it!
            chosenSpawn = spawnName;
            max = distance;
        }

        // If no spawn was chosen, use the main spawn
        if (chosenSpawn == null) return mainSpawn;
        return getSpawn(chosenSpawn);
    }

    public String setMainSpawn(String id, Location location) {
        mainSpawn = location;
        set("main-spawn", id);
        try {
            save();
        } catch (IOException e) {
            CoreClass.getInstance().getLogger().severe("Failed to set main spawnpoint " + id + ": " + e.getMessage());
            return "Error.setMainSpawnFail";
        }
        return "Info.setMainSpawn";
    }

    public String getMainSpawn() {
        return getString("main-spawn");
    }

    public String removeSpawn(String id) {
        set("spawns." + id, null);
        try {
            save();
        } catch (IOException e) {
            CoreClass.getInstance().getLogger().severe("Failed to remove spawnpoint " + id + ": " + e.getMessage());
            return "Error.removeSpawnFail";
        }
        return "Info.removedSpawn";
    }

    @Nullable
    public Location getProperMainSpawn() {
        if (getMainSpawn() == null || getMainSpawn().isEmpty()) return null;
        if (mainSpawn == null || mainSpawn.getWorld() == null) {
            mainSpawn = new Location(Bukkit.getWorld(getString("spawns." + getMainSpawn() + ".world")),
                    getDouble("spawns." + getMainSpawn() + ".x"),
                    getDouble("spawns." + getMainSpawn() + ".y"),
                    getDouble("spawns." + getMainSpawn() + ".z"),
                    getFloat("spawns." + getMainSpawn() + ".yaw"),
                    getFloat("spawns." + getMainSpawn() + ".pitch"));
        }
        return mainSpawn;
    }

    public boolean doesSpawnExist(String id) {
        return get("spawns." + id) != null;
    }

    public List<String> getSpawns() {
        ConfigSection section = getConfigSection("spawns");
        if (section == null) return new ArrayList<>();
        return new ArrayList<>(section.getKeys(false));
    }

    public static Spawn get() {
        return instance;
    }
}
