package nl.frostnetwork.betterachievements;

import net.milkbowl.vault.economy.Economy;
import nl.frostnetwork.betterachievements.command.AchievementCommand;
import nl.frostnetwork.betterachievements.listener.BlockListener;
import nl.frostnetwork.betterachievements.listener.MobKillListener;
import nl.frostnetwork.betterachievements.listener.InventoryListener;
import nl.frostnetwork.betterachievements.listener.PlayerListener;
import nl.frostnetwork.betterachievements.manager.AchievementManager;
import nl.frostnetwork.betterachievements.manager.PlayerDataManager;
import nl.frostnetwork.betterachievements.manager.RewardManager;
import nl.frostnetwork.betterachievements.placeholder.AchievementExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

/**
 * Main class for the BetterAchievements plugin.
 * Handles initialization, configuration loading, and manager registration.
 */
public final class BetterAchievements extends JavaPlugin {

    private static BetterAchievements instance;
    private Economy econ = null;
    private AchievementManager achievementManager;
    private PlayerDataManager playerDataManager;
    private RewardManager rewardManager;
    private FileConfiguration languageConfig;
    private FileConfiguration fallbackConfig;
    private FileConfiguration guiConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Save default configs
        saveDefaultConfigs();
        loadCustomConfigs();

        // Setup Vault
        if (getConfig().getBoolean("vault.enabled", true)) {
            if (!setupEconomy()) {
                getLogger().warning("Vault not found or no economy plugin present! Money rewards will not work.");
            }
        }

        // Initialize Managers
        this.rewardManager = new RewardManager(this);
        this.achievementManager = new AchievementManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        // Register Commands
        getCommand("achievements").setExecutor(new AchievementCommand(this));

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobKillListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);

        // PlaceholderAPI support
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AchievementExpansion(this).register();
        }

        getLogger().info("BetterAchievements has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info("BetterAchievements has been disabled.");
    }

    /**
     * Saves default configuration files if they don't exist.
     */
    private void saveDefaultConfigs() {
        saveDefaultConfig();
        String[] otherConfigs = {"achievements.yml", "rewards.yml", "gui.yml", "example-achievements.yml"};
        for (String config : otherConfigs) {
            File file = new File(getDataFolder(), config);
            if (!file.exists()) {
                saveResource(config, false);
            }
        }

        // Save language files
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        String[] langFiles = {"en_US.yml", "nl_NL.yml"};
        for (String langFile : langFiles) {
            File file = new File(langDir, langFile);
            if (!file.exists()) {
                saveResource("lang/" + langFile, false);
            }
        }
    }

    /**
     * Loads custom configuration files and the selected language file.
     */
    private void loadCustomConfigs() {
        guiConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "gui.yml"));

        // Load language
        String lang = getConfig().getString("Language.Default", "en_US");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            langFile = new File(getDataFolder(), "lang/en_US.yml");
        }
        languageConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load fallback (en_US)
        File fallbackFile = new File(getDataFolder(), "lang/en_US.yml");
        if (fallbackFile.exists()) {
            fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile);
        } else {
            fallbackConfig = languageConfig;
        }
    }

    /**
     * Sets up the Vault economy integration.
     *
     * @return true if successful, false otherwise.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static BetterAchievements getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return econ;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }

    public FileConfiguration getGuiConfig() {
        return guiConfig;
    }

    /**
     * Gets a translated message from the language file.
     *
     * @param path The path to the message.
     * @return The translated and color-coded message.
     */
    public String getMessage(String path) {
        String msg = languageConfig.getString(path);
        if (msg == null && fallbackConfig != null) {
            msg = fallbackConfig.getString(path);
        }
        if (msg == null) {
            return ChatColor.translateAlternateColorCodes('&', "&cMissing translation: " + path);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Gets a translated list of messages from the language file.
     *
     * @param path The path to the list.
     * @return A list of translated and color-coded messages.
     */
    public java.util.List<String> getMessageList(String path) {
        java.util.List<String> list = languageConfig.getStringList(path);
        if ((list == null || list.isEmpty()) && fallbackConfig != null) {
            list = fallbackConfig.getStringList(path);
        }
        java.util.List<String> coloredList = new java.util.ArrayList<>();
        for (String s : list) {
            coloredList.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        return coloredList;
    }

    /**
     * Sends an action bar message to a player.
     *
     * @param player  The player.
     * @param message The message.
     */
    public void sendActionBar(org.bukkit.entity.Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    /**
     * Reloads all plugin configurations.
     */
    public void reloadPlugin() {
        reloadConfig();
        loadCustomConfigs();
        rewardManager.loadRewards();
        achievementManager.loadAchievements();
    }
}
