package nl.frostnetwork.betterachievements.task;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Task that runs periodically to check for achievement progress (e.g., playtime).
 */
public class AchievementTask implements Runnable {

    private final BetterAchievements plugin;

    public AchievementTask(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Achievement activeAch = plugin.getAchievementManager().getActiveAchievement(uuid);
            if (activeAch == null) continue;

            String type = activeAch.getType().toUpperCase();
            switch (type) {
                case "PLAY_TIME":
                    updatePlayTime(player, activeAch);
                    break;
                case "MONEY":
                    updateMoney(player, activeAch);
                    break;
                case "ISLAND_VALUE":
                    updateIslandValue(player, activeAch);
                    break;
                case "ACHIEVEMENTS_COMPLETED":
                    updateAchievementsCompleted(player, activeAch);
                    break;
            }
        }
    }

    private void updatePlayTime(Player player, Achievement a) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        // We voegen 1 minuut toe (taak draait elke 1200 ticks = 60s)
        data.addProgress(a.getId(), 1);
        checkCompletion(player, a, data.getProgress(a.getId()));
    }

    private void updateMoney(Player player, Achievement a) {
        if (plugin.getEconomy() == null) return;
        double balance = plugin.getEconomy().getBalance(player);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        data.setProgress(a.getId(), (int) balance);
        checkCompletion(player, a, (int) balance);
    }

    private void updateIslandValue(Player player, Achievement a) {
        // Placeholder check.
    }

    private void updateAchievementsCompleted(Player player, Achievement a) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int completed = data.getClaimedAchievements().size();
        data.setProgress(a.getId(), completed);
        checkCompletion(player, a, completed);
    }

    private void checkCompletion(Player player, Achievement a, int current) {
        if (current >= a.getRequired()) {
            if (plugin.getConfig().getBoolean("settings.actionbar", true)) {
                plugin.sendActionBar(player, plugin.getMessage("actionbar.completed"));
            }
        } else {
            sendProgressActionBar(player, a, current);
        }
    }

    private void sendProgressActionBar(Player player, Achievement a, int current) {
        if (!plugin.getConfig().getBoolean("settings.actionbar", true)) return;

        double percentage = (double) current / a.getRequired() * 100;
        String message = plugin.getMessage("actionbar.progress")
                .replace("%achievement%", a.getName())
                .replace("%current%", String.valueOf(current))
                .replace("%required%", String.valueOf(a.getRequired()))
                .replace("%percentage%", String.format("%.1f", percentage));

        plugin.sendActionBar(player, message);
    }
}
