package io.github.niestrat99.advancedteleport.sql;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.api.data.UnloadedWorldException;
import io.github.niestrat99.advancedteleport.api.spawn.Spawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SpawnSQLManager extends SQLManager {

    private static SpawnSQLManager instance;

    public SpawnSQLManager() {
        super();
        instance = this;

        transferOldData();
    }

    public static SpawnSQLManager get() {
        return instance;
    }

    @Override
    public void createTable() {
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            try (Connection connection = implementConnection()) {
                PreparedStatement createTable = prepareStatement(connection,
                        "CREATE TABLE IF NOT EXISTS " + tablePrefix + "_spawns " +
                                "(id INTEGER PRIMARY KEY " + getStupidAutoIncrementThing() + ", " +
                                "spawn VARCHAR(256) NOT NULL, " +
                                "uuid_creator VARCHAR(256), " +
                                "x DOUBLE NOT NULL, " +
                                "y DOUBLE NOT NULL, " +
                                "z DOUBLE NOT NULL, " +
                                "yaw FLOAT NOT NULL, " +
                                "pitch FLOAT NOT NULL, " +
                                "world VARCHAR(256) NOT NULL, " +
                                "timestamp_created BIGINT NOT NULL, " +
                                "timestamp_updated BIGINT NOT NULL)");
                executeUpdate(createTable);
            } catch (SQLException exception) {
                CoreClass.getInstance().getLogger().severe("Failed to create the spawns table.");
                exception.printStackTrace();
            }
            transferOldData();
        });
    }

    @Override
    public void transferOldData() {

        // Get the file itself.
        File file = new File(CoreClass.getInstance().getDataFolder(), "spawn.yml");
        if (!file.exists()) return;

        // Load the config file.
        YamlConfiguration spawnsFile = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection spawns = spawnsFile.getConfigurationSection("spawns");
        if (spawns == null) return;

        // Get the main spawn
        String mainSpawn = spawnsFile.getString("main-spawn");

        // For each player found...
        for (String spawnName : spawns.getKeys(false)) {

            // Get the config section representing their homes.
            ConfigurationSection spawnSection = spawns.getConfigurationSection(spawnName);
            if (spawnSection == null) continue;

            // Get the world the home is in - but if it doesn't exist, ignore it
            String world = spawnSection.getString("world");
            if (world == null) continue;

            // Add the home to the database
            addSpawn(spawnName,
                    world,
                    null,
                    spawnSection.getDouble("x"),
                    spawnSection.getDouble("y"),
                    spawnSection.getDouble("z"),
                    (float) spawnSection.getDouble("yaw"),
                    (float) spawnSection.getDouble("pitch"));

            // If the spawn name matches, add the metadata
            if (spawnName.equals(mainSpawn)) {
                MetadataSQLManager.get().addSpawnMetadata(spawnName, "main_spawn", "true");
            }
        }

        // Create a backup file
        file.renameTo(new File(CoreClass.getInstance().getDataFolder(), "spawn-backup.yml"));
    }

    public CompletableFuture<Void> addSpawn(@NotNull Spawn spawn) {
        return addSpawn(spawn.getName(),
                spawn.getLocation().getWorld().getName(),
                spawn.getCreatorUUID(),
                spawn.getLocation().x(),
                spawn.getLocation().y(),
                spawn.getLocation().z(),
                spawn.getLocation().getYaw(),
                spawn.getLocation().getPitch());
    }

    public CompletableFuture<Void> addSpawn(
            @NotNull String name,
            @NotNull String worldName,
            @Nullable UUID creator,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = implementConnection()) {

                PreparedStatement statement = prepareStatement(connection, "INSERT INTO " + tablePrefix + "_spawns " +
                        "(spawn, x, y, z, yaw, pitch, world, timestamp_created, timestamp_updated) VALUES (?,?,?,?,?,?,?,?)");
                statement.setString(1, name);
                statement.setString(2, (creator == null ? null : creator.toString()));
                statement.setDouble(3, x);
                statement.setDouble(4, y);
                statement.setDouble(5, z);
                statement.setFloat(6, yaw);
                statement.setFloat(7, pitch);
                statement.setString(8, worldName);
                statement.setDouble(9, System.currentTimeMillis());
                statement.setDouble(10, System.currentTimeMillis());

                executeUpdate(statement);

            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }, CoreClass.async);
    }

    public CompletableFuture<Void> removeSpawn(String name) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = implementConnection()) {

                PreparedStatement statement = prepareStatement(connection, "DELETE FROM " + tablePrefix + "_spawns " +
                        "WHERE spawn = ?");
                statement.setString(1, name);

                executeUpdate(statement);

            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public CompletableFuture<Void> moveSpawn(Spawn spawn) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = implementConnection()) {

                PreparedStatement statement = prepareStatement(connection, "UPDATE " + tablePrefix + "_spawns SET " +
                        "x = ?, y = ?, z = ?, yaw = ?, pitch = ?, world = ?, timestamp_update = ? WHERE spawn = ?");
                prepareLocation(spawn.getLocation(), 1, statement);
                statement.setLong(7, spawn.getUpdatedTime());
                statement.setString(8, spawn.getName());

                executeUpdate(statement);

            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public CompletableFuture<List<Spawn>> getSpawns() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = implementConnection()) {

                List<Spawn> spawns = new ArrayList<>();

                PreparedStatement statement = prepareStatement(connection, "SELECT * FROM " + tablePrefix + "_spawns");

                // Get the result
                ResultSet result = executeQuery(statement);

                // While there's results to be read...
                while (result.next()) {

                    // Get the location from it
                    try {
                        Location loc = getLocation(result);
                        String uuid = result.getString("uuid_creator");
                        String name = result.getString("spawn");
                        long timestampCreated = result.getLong("timestamp_created");
                        long timestampUpdated = result.getLong("timestamp_updated");

                        Spawn spawn = new Spawn(name, loc, null, UUID.fromString(uuid), timestampCreated, timestampUpdated);

                        spawns.add(spawn);
                    } catch (UnloadedWorldException e) {
                        CoreClass.getInstance().getLogger().warning("Failed to get the spawn for " + result.getString("spawn") + ": " + e.getMessage());
                    }
                }

                return spawns;

            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    public CompletableFuture<Integer> getSpawnId(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = implementConnection()) {
                PreparedStatement statement = prepareStatement(connection,
                        "SELECT id FROM " + tablePrefix + "_spawns WHERE spawn = ?;");
                statement.setString(1, name);
                ResultSet set = executeQuery(statement);
                if (set.next()) {
                    connection.close();
                    return set.getInt("id");
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return -1;
        }, CoreClass.async);
    }
}
