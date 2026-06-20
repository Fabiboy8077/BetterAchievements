package nl.frostnetwork.betterachievements.manager;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player data, including loading, saving, and caching.
 */
public class PlayerDataManager {

    private final BetterAchievements plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private BukkitTask autoSaveTask;

    /**
     * Constructs the PlayerDataManager.
     *
     * @param plugin The plugin instance.
     */
    public PlayerDataManager(BetterAchievements plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        startAutoSaveTask();
    }

    /**
     * Gets the player data for a player, loading it if not cached.
     *
     * @param uuid The player's UUID.
     * @return The PlayerData object.
     */
    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    private PlayerData loadPlayerData(UUID uuid) {
        File file = getDataFile(uuid);
        PlayerData data = new PlayerData(uuid);
        if (!file.exists()) {
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        ConfigurationSection progressSection = config.getConfigurationSection("progress");
        if (progressSection != null) {
            for (String key : progressSection.getKeys(false)) {
                data.setProgress(key, progressSection.getInt(key));
            }
        }

        List<String> claimed = config.getStringList("claimed");
        for (String achId : claimed) {
            data.setClaimed(achId);
        }
        
        data.setDirty(false);
        return data;
    }

    public void savePlayerData(UUID uuid) {
        synchronized (saveLock) {
            PlayerData data = cache.get(uuid);
            if (data == null) return;

            PlayerData.SaveSnapshot snapshot = data.createSaveSnapshot();
            if (snapshot == null) return;

            File file = getDataFile(uuid);
            FileConfiguration config = new YamlConfiguration();

            ConfigurationSection progressSection = config.createSection("progress");
            new TreeMap<>(snapshot.getProgress()).forEach(progressSection::set);

            List<String> claimed = new ArrayList<>(snapshot.getClaimedAchievements());
            Collections.sort(claimed);
            config.set("claimed", claimed);

            try {
                config.save(file);
                data.markSaved(snapshot.getVersion());
            } catch (IOException e) {
                data.setDirty(true);
                plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + uuid, e);
            }
        }
    }

    /**
     * Saves all cached player data to disk if it has changed.
     */
    public void saveAllData() {
        for (UUID uuid : new ArrayList<>(cache.keySet())) {
            savePlayerData(uuid);
        }
    }

    public int getProgress(UUID uuid, String achievementId) {
        return getPlayerData(uuid).getProgress(achievementId);
    }

    public void addProgress(UUID uuid, String achievementId, int amount) {
        getPlayerData(uuid).addProgress(achievementId, amount);
    }

    public boolean isCompleted(UUID uuid, String achievementId) {
        var achievement = plugin.getAchievementManager().getAchievement(achievementId);
        if (achievement == null) return false;
        int required = achievement.getRequired();
        return getProgress(uuid, achievementId) >= required;
    }

    public boolean isClaimed(UUID uuid, String achievementId) {
        return getPlayerData(uuid).isClaimed(achievementId);
    }

    public void setClaimed(UUID uuid, String achievementId) {
        getPlayerData(uuid).setClaimed(achievementId);
        savePlayerData(uuid);
    }

    public boolean hasStoredData(UUID uuid) {
        return cache.containsKey(uuid) || getDataFile(uuid).exists();
    }

    public boolean resetPlayerData(UUID uuid) {
        synchronized (saveLock) {
            File file = getDataFile(uuid);
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("Could not delete player achievement data for " + uuid);
                return false;
            }

            if (Bukkit.getPlayer(uuid) == null) {
                cache.remove(uuid);
            } else {
                cache.put(uuid, new PlayerData(uuid));
            }
            return true;
        }
    }

    public ResetResult resetAllPlayerData() {
        synchronized (saveLock) {
            cache.clear();

            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files == null || files.length == 0) {
                return new ResetResult(0, 0);
            }

            int deleted = 0;
            int failed = 0;
            for (File file : files) {
                if (file.delete()) {
                    deleted++;
                    continue;
                }

                failed++;
                plugin.getLogger().warning("Could not delete player achievement data file: " + file.getAbsolutePath());
            }

            return new ResetResult(deleted, failed);
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayerData(uuid);
        cache.remove(uuid);
    }

    public void reloadSettings() {
        startAutoSaveTask();
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        saveAllData();
    }

    private void startAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        long intervalSeconds = Math.max(5L, plugin.getConfig().getLong("settings.autosave-interval", 60L));
        long intervalTicks = intervalSeconds * 20L;
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllData, intervalTicks, intervalTicks);
    }

    private File getDataFile(UUID uuid) {
        return new File(dataFolder, uuid + ".yml");
    }

    public static final class ResetResult {
        private final int deletedCount;
        private final int failedCount;

        private ResetResult(int deletedCount, int failedCount) {
            this.deletedCount = deletedCount;
            this.failedCount = failedCount;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public boolean isSuccess() {
            return failedCount == 0;
        }
    }
}
