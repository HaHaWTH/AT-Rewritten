package io.github.niestrat99.advancedteleport.utilities.nbt;

import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class NBTReader {

    /*
     * Welcome to absolute Reflection hell! Enjoy your stay.
     */

    private static File MAIN_FOLDER;
    private static Object DEDICATED_SERVER;
    private static Object WORLD_NBT_STORAGE;
    private static HashMap<String, Location> CACHE;
    private static long lastModified;
    private static String VERSION;

    public static void init() {
        VERSION = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

        DEDICATED_SERVER = getDedicatedServer();
        MAIN_FOLDER = getPlayerdataFolder();

        if (MAIN_FOLDER == null) {
            CoreClass.getInstance().getLogger().warning("Main world folder was not found.");
        } else {
            lastModified = MAIN_FOLDER.lastModified();
        }

        WORLD_NBT_STORAGE = getWorldNBTStorage();
        CACHE = new HashMap<>();
    }

    public static void addLeaveToCache(Player player) {
        CACHE.put(player.getName().toLowerCase(), player.getLocation());
    }

    public static void getLocation(String name, NBTCallback<Location> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            String lowerName = name.toLowerCase();
            if (CACHE.containsKey(lowerName) && MAIN_FOLDER.lastModified() == lastModified) {
                callback.onSuccess(CACHE.get(lowerName));
            } else {
                OfflinePlayer player = Bukkit.getOfflinePlayer(name);
                if (DEDICATED_SERVER != null) {
                    if (WORLD_NBT_STORAGE != null) {
                        try {

                            Location location = getLocation(player);
                            if (location == null) {
                                callback.onFail(CustomMessages.getString("Error.noOfflineLocation", "{player}", name));
                                return;
                            }
                            CACHE.put(lowerName, location);
                            callback.onSuccess(location);
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e) {
                            callback.onFail(CustomMessages.getString("Error.failedOfflineTeleport", "{player}", name));
                            e.printStackTrace();
                        }

                    }

                } else {
                    callback.onFail("Dedicated server does not exist.");
                }
            }
        });
    }

    public static void setLocation(String name, Location newLoc, NBTCallback<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
            String lowerName = name.toLowerCase();
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            if (DEDICATED_SERVER != null) {
                if (WORLD_NBT_STORAGE != null) {
                    try {
                        setLocation(player, newLoc);
                        CACHE.put(lowerName, newLoc);
                        callback.onSuccess(true);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException | NoSuchFieldException | InstantiationException | FileNotFoundException e) {
                        e.printStackTrace();
                        callback.onFail(CustomMessages.getString("Error.failedOfflineTeleportHere", "{player}", name));
                    }
                }
            }
        });
    }

    /**
     * @see org.bukkit.craftbukkit.v1_16_R3.CraftServer - server object
     * @see net.minecraft.server.v1_16_R3.DedicatedServer - console object
     * @see net.minecraft.server.v1_16_R3.MinecraftServer - subclass of console
     * @see net.minecraft.server.v1_16_R3.WorldNBTStorage - nbtStorage
     * @return
     */
    private static Object getWorldNBTStorage() {
        Object console = DEDICATED_SERVER;
        try {
            // Get the NBT storage
            Field nbtField = getAlternativeFields(console.getClass().getSuperclass(), "worldNBTStorage", "k");
            nbtField.setAccessible(true);
            return nbtField.get(console);
        } catch (NullPointerException e) {
            Object world = null;
            // Legacy
            try {
                Field worlds = console.getClass().getSuperclass().getDeclaredField("worlds");
                List<Object> worldList = (List<Object>) worlds.get(console);
                world = worldList.get(0);
            } catch (IllegalAccessException | NoSuchFieldException ignored) {
                // ignored
            }
            // Some point after 1.13 but before 1.16, a weird era
            if (world == null) {
                try {
                    // Get world server method itself
                    Class<?> dimensionMan = getClass("DimensionManager", "world.level.dimension.");
                    Method getDimension = console.getClass().getSuperclass().getDeclaredMethod("getWorldServer", dimensionMan);
                    // Get overworld dimension field
                    Field overworld = dimensionMan.getDeclaredField("OVERWORLD");

                    world = getDimension.invoke(console, overworld.get(null));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException | NoSuchMethodException classNotFoundException) {
                    classNotFoundException.printStackTrace();
                }
            }

            if (world == null) return null;

            try {
                Method dataManager = world.getClass().getDeclaredMethod("getDataManager");
                return dataManager.invoke(world);
            } catch (NoSuchMethodException ex) {
                try {
                    Method dataManager = world.getClass().getSuperclass().getDeclaredMethod("getDataManager");
                    return dataManager.invoke(world);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exc) {
                    exc.printStackTrace();
                }
            } catch (InvocationTargetException | IllegalAccessException invocationTargetException) {
                invocationTargetException.printStackTrace();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getDedicatedServer() {
        try {
            // First, get the server
            Server server = Bukkit.getServer();
            // Get the console
            Field consoleField = server.getClass().getDeclaredField("console");
            consoleField.setAccessible(true);
            return consoleField.get(server);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Location getLocation(OfflinePlayer player) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        Method getPlayerData = player.getClass().getDeclaredMethod("getData");
        getPlayerData.setAccessible(true);
        Object nbtCompound = getPlayerData.invoke(player);
        // Offline mode
        if (nbtCompound == null || (!Bukkit.getOnlineMode() && CoreClass.getInstance().getVersion() < 17)) {
            getPlayerData = WORLD_NBT_STORAGE.getClass().getDeclaredMethod("getPlayerData", String.class);
            nbtCompound = getPlayerData.invoke(WORLD_NBT_STORAGE, UUID.nameUUIDFromBytes(player.getName().getBytes()).toString());
        }
        if (nbtCompound == null) return null;
        // Double ID: 6
        // String ID: 5
        // Float ID:
        Method getList = getAlternativeMethods(new String[]{"getList", "c"}, nbtCompound.getClass(), String.class, int.class);

        Object pos = getList.invoke(nbtCompound, "Pos", 6);
        Object rotation = getList.invoke(nbtCompound, "Rotation", 5);

        Method getWorld = getAlternativeMethods(new String[]{"getLong", "i"}, nbtCompound.getClass(), String.class);

        long worldUUIDMost = (long) getWorld.invoke(nbtCompound, "WorldUUIDMost");
        long worldUUIDLeast = (long) getWorld.invoke(nbtCompound, "WorldUUIDLeast");

        World world = Bukkit.getWorld(new UUID(worldUUIDMost, worldUUIDLeast));

        return new Location(world,
                getPosition(pos)[0],
                getPosition(pos)[1],
                getPosition(pos)[2],
                getRotation(rotation)[0],
                getRotation(rotation)[1]);
    }

    private static void setLocation(OfflinePlayer player, Location location) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchFieldException, InstantiationException, FileNotFoundException {
        Method getPlayerData = player.getClass().getDeclaredMethod("getData");
        getPlayerData.setAccessible(true);
        Object nbtCompound = getPlayerData.invoke(player);
        // Offline mode
        if (nbtCompound == null || (!Bukkit.getOnlineMode() && CoreClass.getInstance().getVersion() < 17)) {
            getPlayerData = WORLD_NBT_STORAGE.getClass().getDeclaredMethod("getPlayerData", String.class);
            nbtCompound = getPlayerData.invoke(WORLD_NBT_STORAGE, UUID.nameUUIDFromBytes(player.getName().getBytes()).toString());
        }
        if (nbtCompound == null) return;

        Constructor<?> listConstructor = getClass("NBTTagList", "nbt.").getDeclaredConstructor();
        Constructor<?> nbtDouble = getClass("NBTTagDouble", "nbt.").getDeclaredConstructor(double.class);
        Constructor<?> nbtFloat = getClass("NBTTagFloat", "nbt.").getDeclaredConstructor(float.class);
        Constructor<?> nbtLong = getClass("NBTTagLong", "nbt.").getDeclaredConstructor(long.class);
        // You will NOT fool me! YOU HEAR ME??? YOU WILL NOT FOOL ME!!!!
        listConstructor.setAccessible(true);
        nbtDouble.setAccessible(true);
        nbtFloat.setAccessible(true);
        nbtLong.setAccessible(true);

        Object pos = listConstructor.newInstance();

        List<Object> posList = getListVariable(pos);

        // Set the position
        posList.add(nbtDouble.newInstance(location.getX()));
        posList.add(nbtDouble.newInstance(location.getY()));
        posList.add(nbtDouble.newInstance(location.getZ()));

        Object rot = listConstructor.newInstance();


        List<Object> rotList = getListVariable(rot);
        // Set the rotation
        rotList.add(nbtFloat.newInstance(location.getYaw()));
        rotList.add(nbtFloat.newInstance(location.getPitch()));

        UUID worldUUID = location.getWorld().getUID();

        Method set = getAlternativeMethods(new String[]{"set", "a"}, nbtCompound.getClass(), String.class, getClass("NBTBase", "nbt."));

        set.invoke(nbtCompound, "Pos", pos);
        set.invoke(nbtCompound, "Rotation", rot);
        set.invoke(nbtCompound, "WorldUUIDMost", nbtLong.newInstance(worldUUID.getMostSignificantBits()));
        set.invoke(nbtCompound, "WorldUUIDLeast", nbtLong.newInstance(worldUUID.getLeastSignificantBits()));

        Method getDataFile = player.getClass().getDeclaredMethod("getDataFile");
        getDataFile.setAccessible(true);
        File file = (File) getDataFile.invoke(player);
        FileOutputStream outputStream = new FileOutputStream(file);

        getClass("NBTCompressedStreamTools", "nbt.")
                .getDeclaredMethod("a", getClass("NBTTagCompound", "nbt."), OutputStream.class)
                .invoke(null, nbtCompound, outputStream);
    }

    private static double[] getPosition(Object pos) throws IllegalAccessException, NoSuchFieldException {
        List<Object> list = getListVariable(pos);
        double[] posArray = new double[3];
        for (int i = 0; i < 3; i++) {
            Object nbtBase = list.get(i);
            Field data = getAlternativeFields(nbtBase.getClass(), "data", "w");
            data.setAccessible(true);
            posArray[i] = (double) data.get(nbtBase);
        }

        return posArray;
    }

    private static float[] getRotation(Object rot) throws IllegalAccessException, NoSuchFieldException {
        List<Object> list = getListVariable(rot);
        float[] rotArray = new float[2];
        for (int i = 0; i < 2; i++) {
            Object nbtBase = list.get(i);
            Field data = getAlternativeFields(nbtBase.getClass(), "data", "w");
            data.setAccessible(true);
            rotArray[i] = (float) data.get(nbtBase);
        }
        return rotArray;
    }

    private static List<Object> getListVariable(Object obj) throws IllegalAccessException, NoSuchFieldException {
        Field list = getAlternativeFields(obj.getClass(), "list", "c");
        list.setAccessible(true);
        return (List<Object>) list.get(obj);
    }

    private static File getPlayerdataFolder() {
        File root = new File(System.getProperty("user.dir"));
        if (!root.exists()) return null;
        if (!root.isDirectory()) return null;
        for (File file : root.listFiles()) {
            try {
                if (file.isDirectory()) {
                    for (File subFile : file.listFiles()) {
                        if (!subFile.isDirectory()) continue;
                        if (subFile.getName().equals("playerdata")) return subFile;
                    }
                }
            } catch (NullPointerException ex) {
                CoreClass.getInstance().getLogger().warning("Failed to get files of directory " + file.getName() + " in " + root.getName());
            }
        }
        return null;
    }

    private static Class<?> getClass(String className, String specificLocation) {
        try {
            return Class.forName("net.minecraft." + specificLocation + className);
        } catch (ClassNotFoundException ex) {
            try {
                return Class.forName("net.minecraft.server." + VERSION + "." + className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Field getAlternativeFields(Class<?> obj, String... fields) {
        for (String fieldName : fields) {
            try {
                Field field = obj.getDeclaredField(fieldName);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Method getAlternativeMethods(String[] names, Class<?> target, Class<?>... fields) {
        for (String name : names) {
            try {
                Method method = target.getDeclaredMethod(name, fields);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }

    public interface NBTCallback<D> {

        void onSuccess(D data);

        default void onFail(String message) {}
    }
}
