package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listens for player-related events like joining and quitting.
 */
public class PlayerListener implements Listener {

    private final BetterAchievements plugin;

    public PlayerListener(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Zorg dat data geladen is
        plugin.getPlayerDataManager().getPlayerData(uuid);

        // JOIN achievement
        handleProgress(player, "JOIN");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unloadPlayer(event.getPlayer().getUniqueId());
    }

    private void handleProgress(Player player, String type) {
        UUID uuid = player.getUniqueId();
        Achievement activeAch = plugin.getAchievementManager().getActiveAchievement(uuid);
        
        if (activeAch == null) return;
        if (!activeAch.getType().equalsIgnoreCase(type)) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        int current = data.getProgress(activeAch.getId());
        if (current < activeAch.getRequired()) {
            data.addProgress(activeAch.getId(), 1);
            current++;
            
            if (current == activeAch.getRequired()) {
                String msg = plugin.getMessage("achievement.completed").replace("%achievement%", activeAch.getName());
                player.sendMessage(plugin.getMessage("prefix") + msg);

                if (plugin.getConfig().getBoolean("settings.actionbar", true)) {
                    plugin.sendActionBar(player, plugin.getMessage("actionbar.completed"));
                }
            } else {
                sendProgressActionBar(player, activeAch, current);
            }
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
