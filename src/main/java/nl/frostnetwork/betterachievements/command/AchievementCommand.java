package nl.frostnetwork.betterachievements.command;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.gui.AchievementGUI;
import nl.frostnetwork.betterachievements.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles the /achievements command and its subcommands.
 */
public class AchievementCommand implements TabExecutor {

    private final BetterAchievements plugin;
    private final AchievementGUI gui;
    private static final List<String> ADMIN_SUBCOMMANDS = List.of("reload", "reset", "resetall");

    /**
     * Constructs the AchievementCommand.
     *
     * @param plugin The plugin instance.
     */
    public AchievementCommand(BetterAchievements plugin) {
        this.plugin = plugin;
        this.gui = new AchievementGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!hasAdminPermission(sender)) {
                return true;
            }

            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reload"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!hasAdminPermission(sender)) {
                return true;
            }

            handleReset(sender, args);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("resetall")) {
            if (!hasAdminPermission(sender)) {
                return true;
            }

            handleResetAll(sender);
            return true;
        }

        // Open GUI for players
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;
        gui.open(player);
        return true;
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reset_usage"));
            return;
        }

        String requestedName = args[1];
        OfflinePlayer target = findOfflinePlayer(requestedName);
        UUID uuid = target.getUniqueId();

        if (!target.isOnline() && !target.hasPlayedBefore() && !plugin.getPlayerDataManager().hasStoredData(uuid)) {
            sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reset_player_not_found")
                    .replace("%player%", requestedName));
            return;
        }

        if (!plugin.getPlayerDataManager().resetPlayerData(uuid)) {
            sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reset_failed"));
            return;
        }

        AchievementGUI.clearPage(uuid);
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            closeAchievementInventory(onlineTarget);
        }

        sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reset_player_success")
                .replace("%player%", getPlayerName(target, requestedName)));
    }

    private void handleResetAll(CommandSender sender) {
        PlayerDataManager.ResetResult result = plugin.getPlayerDataManager().resetAllPlayerData();

        for (Player player : Bukkit.getOnlinePlayers()) {
            AchievementGUI.clearPage(player.getUniqueId());
            closeAchievementInventory(player);
        }

        String messagePath = result.isSuccess() ? "achievement.reset_all_success" : "achievement.reset_all_partial";
        sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage(messagePath)
                .replace("%count%", String.valueOf(result.getDeletedCount()))
                .replace("%failed%", String.valueOf(result.getFailedCount())));
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.hasPermission("betterachievements.admin")) {
            return true;
        }

        sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.no_permission"));
        return false;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer findOfflinePlayer(String name) {
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        return Bukkit.getOfflinePlayer(name);
    }

    private String getPlayerName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null || name.isBlank() ? fallback : name;
    }

    private void closeAchievementInventory(Player player) {
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AchievementGUI) {
            player.closeInventory();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("betterachievements.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterCompletions(args[0], ADMIN_SUBCOMMANDS);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filterCompletions(args[1], names);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(String input, List<String> options) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerInput)) {
                matches.add(option);
            }
        }

        return matches;
    }
}
