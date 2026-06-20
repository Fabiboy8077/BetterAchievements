package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        plugin.getProgressManager().recordJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        nl.frostnetwork.betterachievements.gui.AchievementGUI.clearPage(event.getPlayer().getUniqueId());
        plugin.getPlayerDataManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
