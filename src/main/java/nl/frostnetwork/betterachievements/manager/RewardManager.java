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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages the rewards given to players for completing achievements.
 */
public class RewardManager {

    private final BetterAchievements plugin;
    private final Map<String, Reward> rewards = new HashMap<>();

    public RewardManager(BetterAchievements plugin) {
        this.plugin = plugin;
        loadRewards();
    }

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
            double money = Math.max(0D, section.getDouble(key + ".money"));
            List<String> items = section.getStringList(key + ".items");
            List<String> commands = section.getStringList(key + ".commands");
            rewards.put(key, new Reward(money, items, commands));
        }
    }

    public Reward getReward(String id) {
        return rewards.get(id);
    }

    public List<String> getRewardDescription(String rewardId) {
        List<String> description = new ArrayList<>();
        Reward reward = rewards.get(rewardId);
        if (reward == null) return description;

        if (reward.getMoney() > 0) {
            description.add("&8- &6$" + String.format(Locale.US, "%.0f", reward.getMoney()));
        }

        for (String itemStr : reward.getItems()) {
            RewardItem rewardItem = parseRewardItem(itemStr);
            if (rewardItem == null) continue;
            description.add("&8- &f" + rewardItem.amount + "x " + formatMaterialName(rewardItem.material.name()));
        }

        return description;
    }

    public void giveReward(Player player, String rewardId) {
        Reward reward = rewards.get(rewardId);
        if (reward == null) return;

        if (reward.getMoney() > 0) {
            if (plugin.getEconomy() != null) {
                plugin.getEconomy().depositPlayer(player, reward.getMoney());
            } else {
                plugin.getLogger().warning("Could not give money reward to " + player.getName() + " because Vault/Economy is missing.");
            }
        }

        for (String itemStr : reward.getItems()) {
            RewardItem rewardItem = parseRewardItem(itemStr);
            if (rewardItem == null) {
                plugin.getLogger().warning("Invalid reward item '" + itemStr + "' in reward '" + rewardId + "'.");
                continue;
            }

            ItemStack item = new ItemStack(rewardItem.material, rewardItem.amount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        for (String command : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }
    }

    private RewardItem parseRewardItem(String itemStr) {
        if (itemStr == null || itemStr.isBlank()) {
            return null;
        }

        String[] parts = itemStr.split(":", 2);
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            return null;
        }

        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }

        return new RewardItem(material, Math.max(1, amount));
    }

    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static final class RewardItem {
        private final Material material;
        private final int amount;

        private RewardItem(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
    }
}
