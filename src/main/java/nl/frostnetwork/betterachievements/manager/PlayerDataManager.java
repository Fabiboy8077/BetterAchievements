package nl.frostnetwork.betterachievements.manager;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data, including loading, saving, and caching.
 */
public class PlayerDataManager {

    private final BetterAchievements plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

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
        
        // Auto-save task every 60 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllData, 1200L, 1200L);
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
        File file = new File(dataFolder, uuid.toString() + ".yml");
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
        PlayerData data = cache.get(uuid);
        if (data == null || !data.isDirty()) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        ConfigurationSection progressSection = config.createSection("progress");
        data.getProgressMap().forEach(progressSection::set);
        
        config.set("claimed", new ArrayList<>(data.getClaimedAchievements()));

        try {
            config.save(file);
            data.setDirty(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves all cached player data to disk if it has changed.
     */
    public void saveAllData() {
        for (UUID uuid : cache.keySet()) {
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

    public void unloadPlayer(UUID uuid) {
        savePlayerData(uuid);
        cache.remove(uuid);
    }
}
