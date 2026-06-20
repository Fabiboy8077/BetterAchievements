package nl.frostnetwork.betterachievements.task;

import nl.frostnetwork.betterachievements.BetterAchievements;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
            plugin.getProgressManager().addProgress(player, "PLAY_TIME", 1, "NONE");

            if (plugin.getEconomy() != null) {
                plugin.getProgressManager().setProgress(player, "MONEY", (int) plugin.getEconomy().getBalance(player), "NONE");
            }

            int completed = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getClaimedCount();
            plugin.getProgressManager().setProgress(player, "ACHIEVEMENTS_COMPLETED", completed, "NONE");
        }
    }
}
