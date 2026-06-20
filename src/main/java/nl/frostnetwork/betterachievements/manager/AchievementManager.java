package nl.frostnetwork.betterachievements.manager;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * Manages all achievements, including loading and retrieval.
 */
public class AchievementManager {

    private final BetterAchievements plugin;
    private final Map<String, Achievement> achievements = new LinkedHashMap<>();
    private final List<Achievement> sortedAchievements = new ArrayList<>();
    private final Map<String, List<Achievement>> achievementsByType = new HashMap<>();

    /**
     * Constructs the AchievementManager.
     *
     * @param plugin The plugin instance.
     */
    public AchievementManager(BetterAchievements plugin) {
        this.plugin = plugin;
        loadAchievements();
    }

    /**
     * Loads all achievements from achievements.yml and translates their display strings.
     */
    public void loadAchievements() {
        achievements.clear();
        sortedAchievements.clear();
        achievementsByType.clear();

        File file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            plugin.saveResource("achievements.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("achievements");

        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int order = section.getInt(key + ".order", 0);
            
            // Fetch translated strings from language files
            String name = plugin.getMessage("achievements." + key + ".name");
            String task = plugin.getMessage("achievements." + key + ".task");
            String sneakpeekHint = plugin.getMessage("achievements." + key + ".hint");
            
            String type = normalize(section.getString(key + ".type", "UNKNOWN"));
            String target = normalize(section.getString(key + ".target", "NONE"));
            int required = Math.max(1, section.getInt(key + ".required", 1));
            
            String guiItemName = section.getString(key + ".gui-item", "PAPER");
            Material guiItem = Material.matchMaterial(guiItemName);
            if (guiItem == null || guiItem.isAir()) {
                plugin.getLogger().warning("Invalid gui-item '" + guiItemName + "' for achievement '" + key + "'. Using PAPER.");
                guiItem = Material.PAPER;
            }
            
            String rewardId = section.getString(key + ".reward-id", "");
            if (!rewardId.isBlank() && plugin.getRewardManager().getReward(rewardId) == null) {
                plugin.getLogger().warning("Achievement '" + key + "' references missing reward-id '" + rewardId + "'.");
            }

            Achievement achievement = new Achievement(key, order, name, task, type, target, required, guiItem, sneakpeekHint, rewardId);
            
            achievements.put(key, achievement);
            sortedAchievements.add(achievement);
            achievementsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(achievement);
        }
        
        Comparator<Achievement> orderComparator = Comparator
                .comparingInt(Achievement::getOrder)
                .thenComparing(Achievement::getId);
        sortedAchievements.sort(orderComparator);
        achievementsByType.values().forEach(list -> list.sort(orderComparator));
        plugin.getLogger().info("Loaded " + achievements.size() + " achievements.");
    }

    /**
     * Gets all loaded achievements.
     *
     * @return A map of achievement IDs to Achievement objects.
     */
    public Map<String, Achievement> getAchievements() {
        return Collections.unmodifiableMap(achievements);
    }
    
    /**
     * Gets all achievements sorted by their order.
     *
     * @return A list of sorted achievements.
     */
    public List<Achievement> getSortedAchievements() {
        return Collections.unmodifiableList(sortedAchievements);
    }

    /**
     * Gets a specific achievement by its ID.
     *
     * @param id The achievement ID.
     * @return The Achievement object, or null if not found.
     */
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }

    /**
     * Gets all achievements of a specific type.
     *
     * @param type The type (e.g., BLOCK_BREAK).
     * @return A list of achievements with that type.
     */
    public List<Achievement> getAchievementsByType(String type) {
        List<Achievement> achievements = achievementsByType.get(normalize(type));
        if (achievements == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(achievements);
    }

    /**
     * Gets the currently active achievement for a player (the first one they haven't claimed).
     *
     * @param uuid The player's UUID.
     * @return The active Achievement, or null if all are completed.
     */
    public Achievement getActiveAchievement(UUID uuid) {
        return getActiveAchievement(plugin.getPlayerDataManager().getPlayerData(uuid));
    }

    public Achievement getActiveAchievement(PlayerData data) {
        for (Achievement a : sortedAchievements) {
            if (!data.isClaimed(a.getId())) {
                return a;
            }
        }
        return null;
    }

    public int getFirstUnclaimedOrder(PlayerData data) {
        for (Achievement achievement : sortedAchievements) {
            if (!data.isClaimed(achievement.getId())) {
                return achievement.getOrder();
            }
        }
        return Integer.MAX_VALUE;
    }

    public boolean hasUnclaimedBefore(PlayerData data, Achievement achievement) {
        return getFirstUnclaimedOrder(data) < achievement.getOrder();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "NONE";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
