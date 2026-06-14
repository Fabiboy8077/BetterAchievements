package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

/**
 * Listens for block breaks and updates achievement progress.
 */
public class BlockListener implements Listener {

    private final BetterAchievements plugin;

    public BlockListener(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("player-placed")) {
            return;
        }
        
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        handleProgress(player, material, "BLOCK_BREAK");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata("player-placed", new FixedMetadataValue(plugin, true));
        
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();

        handleProgress(player, material, "BLOCK_PLACE");
    }

    private void handleProgress(Player player, Material material, String type) {
        UUID uuid = player.getUniqueId();
        Achievement activeAch = plugin.getAchievementManager().getActiveAchievement(uuid);
        
        if (activeAch == null) return;
        if (!activeAch.getType().equalsIgnoreCase(type)) return;

        boolean match = false;
        String target = activeAch.getTarget();
        if (target.equalsIgnoreCase("ANY") || target.equalsIgnoreCase("NONE") || target.equalsIgnoreCase(material.name())) {
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
                    player.sendMessage(plugin.getMessage("prefix") + msg);
                    
                    if (plugin.getConfig().getBoolean("settings.actionbar", true)) {
                        plugin.sendActionBar(player, plugin.getMessage("actionbar.completed"));
                    }
                } else {
                    sendProgressActionBar(player, activeAch, current);
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
