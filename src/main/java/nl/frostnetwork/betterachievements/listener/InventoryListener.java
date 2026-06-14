package nl.frostnetwork.betterachievements.listener;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.gui.AchievementGUI;
import org.bukkit.ChatColor;
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

        String title = event.getView().getTitle();
        // Check of de titel begint met de config titel (om pagina's te ondersteunen)
        String guiTitleBase = ChatColor.translateAlternateColorCodes('&', plugin.getGuiConfig().getString("title", "Achievements").split("%page%")[0]);

        if (title.startsWith(guiTitleBase)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            
            gui.handleClick(player, event.getSlot(), event.getCurrentItem());
        }
    }
}
