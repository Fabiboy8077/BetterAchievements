package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listens for mob kills and updates achievement progress.
 */
public class MobKillListener implements Listener {

    private final BetterAchievements plugin;

    public MobKillListener(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        plugin.getProgressManager().recordMobKill(
                killer,
                event.getEntityType(),
                event.getEntity() instanceof Monster
        );
    }
}
