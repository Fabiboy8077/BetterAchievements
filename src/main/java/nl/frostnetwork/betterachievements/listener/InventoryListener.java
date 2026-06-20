package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.gui.AchievementGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listens for inventory clicks to handle GUI interactions.
 */
public class InventoryListener implements Listener {

    private final BetterAchievements plugin;
    private final AchievementGUI gui;

    public InventoryListener(BetterAchievements plugin) {
        this.plugin = plugin;
        this.gui = new AchievementGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof AchievementGUI) {
            event.setCancelled(true);
            
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof AchievementGUI) {
                if (event.getCurrentItem() == null) return;
                gui.handleClick(player, event.getSlot(), event.getCurrentItem());
            }
        }
    }
}
