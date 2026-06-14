package nl.frostnetwork.betterachievements.manager;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the rewards given to players for completing achievements.
 */
public class RewardManager {

    private final BetterAchievements plugin;
    private final Map<String, Reward> rewards = new HashMap<>();

    /**
     * Constructs the RewardManager.
     *
     * @param plugin The plugin instance.
     */
    public RewardManager(BetterAchievements plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    /**
     * Loads all rewards from rewards.yml.
     */
    public void loadRewards() {
        rewards.clear();
        File file = new File(plugin.getDataFolder(), "rewards.yml");
        if (!file.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("rewards");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            double money = section.getDouble(key + ".money");
            List<String> items = section.getStringList(key + ".items");
            List<String> commands = section.getStringList(key + ".commands");
            rewards.put(key, new Reward(money, items, commands));
        }
    }

    public Reward getReward(String id) {
        return rewards.get(id);
    }

    /**
     * Gives a reward to a player.
     *
     * @param player   The player to receive the reward.
     * @param rewardId The ID of the reward to give.
     */
    public void giveReward(Player player, String rewardId) {
        Reward reward = rewards.get(rewardId);
        if (reward == null) return;

        // Geld beloning
        if (reward.getMoney() > 0) {
            if (plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, reward.getMoney());
            } else {
                plugin.getLogger().warning("Kon geen geld beloning geven aan " + player.getName() + " omdat Vault/Economy ontbreekt!");
            }
        }

        // Item beloningen
        for (String itemStr : reward.getItems()) {
            String[] parts = itemStr.split(":");
            Material material = Material.matchMaterial(parts[0]);
            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {}
            }

            if (material != null) {
                ItemStack item = new ItemStack(material, amount);
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }

        // Command beloningen
        for (String command : reward.getCommands()) {
            String cmd = command.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}
