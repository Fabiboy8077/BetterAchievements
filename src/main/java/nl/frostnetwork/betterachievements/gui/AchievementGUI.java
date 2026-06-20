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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the Achievement GUI, including rendering and click handling.
 */
public class AchievementGUI implements InventoryHolder {

    private final BetterAchievements plugin;
    private static final Map<UUID, Integer> playerPage = new HashMap<>();
    private static final List<Integer> DEFAULT_ACHIEVEMENT_SLOTS = List.of(
            10, 11, 12, 13, 14,
            23, 22, 21, 20, 19,
            28, 29, 30, 31, 32,
            41, 40, 39, 38, 37
    );

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
        int size = getInventorySize(guiConfig);
        List<Achievement> allAchievements = plugin.getAchievementManager().getSortedAchievements();
        List<Integer> slots = getAchievementSlots(guiConfig, size);
        int achievementsPerPage = slots.size();
        int maxPage = allAchievements.isEmpty() ? 0 : (allAchievements.size() - 1) / achievementsPerPage;
        int safePage = Math.max(0, Math.min(page, maxPage));
        String title = plugin.getMessage("gui.title").replace("%page%", String.valueOf(safePage + 1));

        Inventory inv = Bukkit.createInventory(this, size, title);
        playerPage.put(player.getUniqueId(), safePage);

        // Fill background
        ItemStack filler = createItem(getMaterial(guiConfig.getString("filler.item"), Material.BLACK_STAINED_GLASS_PANE), guiConfig.getString("filler.name", " "));
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Add buttons
        int previousSlot = guiConfig.getInt("buttons.previous.slot", 45);
        if (safePage > 0 && isValidSlot(previousSlot, size)) {
            inv.setItem(previousSlot, createItem(getMaterial(guiConfig.getString("buttons.previous.item"), Material.ARROW), plugin.getMessage("gui.previous_page")));
        }

        int nextSlot = guiConfig.getInt("buttons.next.slot", 53);
        if (allAchievements.size() > (safePage + 1) * achievementsPerPage && isValidSlot(nextSlot, size)) {
            inv.setItem(nextSlot, createItem(getMaterial(guiConfig.getString("buttons.next.item"), Material.ARROW), plugin.getMessage("gui.next_page")));
        }

        // Add achievements
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int firstUnclaimedOrder = plugin.getAchievementManager().getFirstUnclaimedOrder(data);
        int startIdx = safePage * achievementsPerPage;
        
        for (int i = 0; i < achievementsPerPage; i++) {
            int achIdx = startIdx + i;
            if (achIdx >= allAchievements.size()) break;
            
            Achievement ach = allAchievements.get(achIdx);
            int slot = slots.get(i);
            
            inv.setItem(slot, createAchievementItem(ach, data, firstUnclaimedOrder));
        }

        player.openInventory(inv);
    }

    private ItemStack createAchievementItem(Achievement ach, PlayerData data, int firstUnclaimedOrder) {
        FileConfiguration guiConfig = plugin.getGuiConfig();
        boolean claimed = data.isClaimed(ach.getId());
        boolean completed = data.getProgress(ach.getId()) >= ach.getRequired();
        boolean locked = firstUnclaimedOrder < ach.getOrder();

        String status = claimed ? "claimed" : (completed ? "claimable" : (locked ? "locked" : "active"));
        Material material = getMaterial(guiConfig.getString("styles." + status + ".material"), Material.PAPER);
        String name = ach.getName();
        List<String> lore = new ArrayList<>();

        if (status.equals("locked")) {
            material = getMaterial(guiConfig.getString("styles.locked.material"), Material.GRAY_STAINED_GLASS_PANE);
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
        if (meta == null) {
            return item;
        }
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
        if (meta == null) {
            return item;
        }
        if (name != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(Player player, int slot, ItemStack item) {
        FileConfiguration guiConfig = plugin.getGuiConfig();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);
        int size = getInventorySize(guiConfig);

        if (slot == guiConfig.getInt("buttons.previous.slot", 45)) {
            if (page > 0) {
                open(player, page - 1);
            }
            return;
        }
        if (slot == guiConfig.getInt("buttons.next.slot", 53)) {
            List<Achievement> allAchievements = plugin.getAchievementManager().getSortedAchievements();
            List<Integer> slots = getAchievementSlots(guiConfig, size);
            int achievementsPerPage = slots.size();

            if (allAchievements.size() > (page + 1) * achievementsPerPage) {
                open(player, page + 1);
            }
            return;
        }

        List<Integer> slots = getAchievementSlots(guiConfig, size);
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
                    if (plugin.getAchievementManager().hasUnclaimedBefore(data, ach)) {
                        player.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.locked"));
                        return;
                    }

                    // Claim
                    plugin.getPlayerDataManager().setClaimed(player.getUniqueId(), ach.getId());
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

    public static void clearPage(UUID uuid) {
        playerPage.remove(uuid);
    }

    private int getInventorySize(FileConfiguration guiConfig) {
        int size = guiConfig.getInt("size", 54);
        size = Math.max(9, Math.min(54, size));
        return size - (size % 9);
    }

    private List<Integer> getAchievementSlots(FileConfiguration guiConfig, int inventorySize) {
        Set<Integer> safeSlots = new LinkedHashSet<>();
        for (int slot : guiConfig.getIntegerList("zigzag-slots")) {
            if (isValidSlot(slot, inventorySize)) {
                safeSlots.add(slot);
            }
        }

        if (safeSlots.isEmpty()) {
            for (int slot : DEFAULT_ACHIEVEMENT_SLOTS) {
                if (isValidSlot(slot, inventorySize)) {
                    safeSlots.add(slot);
                }
            }
        }

        if (safeSlots.isEmpty()) {
            for (int slot = 0; slot < inventorySize; slot++) {
                safeSlots.add(slot);
            }
        }

        return new ArrayList<>(safeSlots);
    }

    private boolean isValidSlot(int slot, int inventorySize) {
        return slot >= 0 && slot < inventorySize;
    }

    private Material getMaterial(String materialName, Material fallback) {
        Material material = materialName == null ? null : Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            return fallback;
        }
        return material;
    }
}
