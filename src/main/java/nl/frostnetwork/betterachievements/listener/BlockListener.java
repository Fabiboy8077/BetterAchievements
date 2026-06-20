package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listens for block breaks and updates achievement progress.
 */
public class BlockListener implements Listener {

    private static final Set<Material> MATURE_CROPS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH
    );

    private final BetterAchievements plugin;

    public BlockListener(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.hasMetadata("player-placed") && !MATURE_CROPS.contains(block.getType())) {
            return;
        }

        Player player = event.getPlayer();
        if (!isHarvestable(block)) {
            return;
        }

        plugin.getProgressManager().recordBlockBreak(player, block.getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata("player-placed", new FixedMetadataValue(plugin, true));

        plugin.getProgressManager().recordBlockPlace(
                event.getPlayer(),
                event.getBlock().getType(),
                event.getItemInHand().getType()
        );
    }

    private boolean isHarvestable(Block block) {
        if (!MATURE_CROPS.contains(block.getType())) {
            return true;
        }

        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return true;
        }

        return ageable.getAge() >= ageable.getMaximumAge();
    }
}
