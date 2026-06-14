package nl.frostnetwork.betterachievements.gui;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import nl.frostnetwork.betterachievements.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Achievement GUI, including rendering and click handling.
 */
public class AchievementGUI implements InventoryHolder {

    private final BetterAchievements plugin;
    private static final Map<UUID, Integer> playerPage = new HashMap<>();

    /**
     * Constructs the AchievementGUI.
     *
     * @param plugin The plugin instance.
     */
    public AchievementGUI(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the achievement GUI for a player at the first page.
     *
     * @param player The player.
     */
    public void open(Player player) {
        open(player, playerPage.getOrDefault(player.getUniqueId(), 0));
    }

    /**
     * Opens the achievement GUI for a player at a specific page.
     *
     * @param player The player.
     * @param page   The page number (0-indexed).
     */
    public void open(Player player, int page) {
        FileConfiguration guiConfig = plugin.getGuiConfig();
        int size = guiConfig.getInt("size", 54);
        String title = plugin.getMessage("gui.title").replace("%page%", String.valueOf(page + 1));
        
        Inventory inv = Bukkit.createInventory(this, size, title);
        playerPage.put(player.getUniqueId(), page);

        // Fill background
        ItemStack filler = createItem(Material.valueOf(guiConfig.getString("filler.item", "BLACK_STAINED_GLASS_PANE")), guiConfig.getString("filler.name", " "));
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Add buttons
        if (page > 0) {
            inv.setItem(guiConfig.getInt("buttons.previous.slot"), createItem(Material.valueOf(guiConfig.getString("buttons.previous.item", "ARROW")), plugin.getMessage("gui.previous_page")));
        }
        
        List<Achievement> allAchievements = plugin.getAchievementManager().getSortedAchievements();
        List<Integer> slots = guiConfig.getIntegerList("zigzag-slots");
        int achievementsPerPage = slots.size();
        
        if (allAchievements.size() > (page + 1) * achievementsPerPage) {
            inv.setItem(guiConfig.getInt("buttons.next.slot"), createItem(Material.valueOf(guiConfig.getString("buttons.next.item", "ARROW")), plugin.getMessage("gui.next_page")));
        }

        // Add achievements
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int startIdx = page * achievementsPerPage;
        
        for (int i = 0; i < achievementsPerPage; i++) {
            int achIdx = startIdx + i;
            if (achIdx >= allAchievements.size()) break;
            
            Achievement ach = allAchievements.get(achIdx);
            int slot = slots.get(i);
            
            inv.setItem(slot, createAchievementItem(player, ach, data));
        }

        player.openInventory(inv);
    }

    private ItemStack createAchievementItem(Player player, Achievement ach, PlayerData data) {
        FileConfiguration guiConfig = plugin.getGuiConfig();
        boolean claimed = data.isClaimed(ach.getId());
        boolean completed = data.getProgress(ach.getId()) >= ach.getRequired();
        
        // Check if locked (previous not claimed)
        boolean locked = false;
        int currentOrder = ach.getOrder();
        for (Achievement a : plugin.getAchievementManager().getSortedAchievements()) {
            if (a.getOrder() < currentOrder && !data.isClaimed(a.getId())) {
                locked = true;
                break;
            }
        }

        String status = claimed ? "claimed" : (completed ? "claimable" : (locked ? "locked" : "active"));
        Material material = Material.valueOf(guiConfig.getString("styles." + status + ".material", "PAPER"));
        String name = ach.getName();
        List<String> lore = new ArrayList<>();

        if (status.equals("locked")) {
            material = Material.valueOf(guiConfig.getString("styles.locked.material", "GRAY_STAINED_GLASS_PANE"));
            name = plugin.getMessage("gui.locked_name");
            List<String> lockedLore = plugin.getMessageList("gui.locked_lore");
            for (String line : lockedLore) {
                lore.add(line.replace("%hint%", ach.getSneakpeekHint()));
            }
        } else {
            if (status.equals("active")) {
                material = ach.getGuiItem(); // Use custom item for active
            }
            List<String> styleLore = plugin.getMessageList("gui." + status + "_lore");
            for (String line : styleLore) {
                if (line.contains("%rewards%")) {
                    lore.add(plugin.getMessage("gui.rewards_label"));
                    lore.addAll(plugin.getRewardManager().getRewardDescription(ach.getRewardId()));
                } else {
                    lore.add(line.replace("%task%", ach.getTask())
                                 .replace("%progress%", String.valueOf(data.getProgress(ach.getId())))
                                 .replace("%required%", String.valueOf(ach.getRequired())));
                }
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(Player player, int slot, ItemStack item) {
        FileConfiguration guiConfig = plugin.getGuiConfig();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == guiConfig.getInt("buttons.previous.slot")) {
            open(player, page - 1);
            return;
        }
        if (slot == guiConfig.getInt("buttons.next.slot")) {
            open(player, page + 1);
            return;
        }

        List<Integer> slots = guiConfig.getIntegerList("zigzag-slots");
        if (slots.contains(slot)) {
            int achIdx = (page * slots.size()) + slots.indexOf(slot);
            List<Achievement> all = plugin.getAchievementManager().getSortedAchievements();
            if (achIdx < all.size()) {
                Achievement ach = all.get(achIdx);
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                
                if (data.isClaimed(ach.getId())) {
                    player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.already_claimed"));
                    return;
                }
                
                if (data.getProgress(ach.getId()) >= ach.getRequired()) {
                    // Check if previous claimed
                    boolean previousClaimed = true;
                    for (Achievement a : all) {
                        if (a.getOrder() < ach.getOrder() && !data.isClaimed(a.getId())) {
                            previousClaimed = false;
                            break;
                        }
                    }
                    
                    if (!previousClaimed) {
                        player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.locked"));
                        return;
                    }

                    // Claim
                    data.setClaimed(ach.getId());
                    plugin.getRewardManager().giveReward(player, ach.getRewardId());
                    player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.claimed").replace("%achievement%", ach.getName()));
                    open(player, page); // Refresh
                } else {
                    player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.locked"));
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
