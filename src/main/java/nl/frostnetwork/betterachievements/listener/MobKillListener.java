package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

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

        UUID uuid = killer.getUniqueId();
        Achievement activeAch = plugin.getAchievementManager().getActiveAchievement(uuid);
        
        if (activeAch == null) return;
        if (!activeAch.getType().equalsIgnoreCase("MOB_KILL")) return;

        boolean match = false;
        String target = activeAch.getTarget();
        if (target.equalsIgnoreCase("ANY")) {
            match = true;
        } else if (target.equalsIgnoreCase("MONSTER")) {
            if (event.getEntity() instanceof Monster) match = true;
        } else if (target.equalsIgnoreCase(event.getEntityType().name())) {
            match = true;
        }

        if (match) {
            PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
            int current = data.getProgress(activeAch.getId());
            if (current < activeAch.getRequired()) {
                data.addProgress(activeAch.getId(), 1);
                current++;
                
                if (current == activeAch.getRequired()) {
                    String msg = plugin.getMessage("achievement.completed").replace("%achievement%", activeAch.getName());
                    killer.sendMessage(plugin.getMessage("prefix") + msg);

                    if (plugin.getConfig().getBoolean("settings.actionbar", true)) {
                        plugin.sendActionBar(killer, plugin.getMessage("actionbar.completed"));
                    }
                } else {
                    sendProgressActionBar(killer, activeAch, current);
                }
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
