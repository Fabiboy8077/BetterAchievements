package nl.frostnetwork.betterachievements.command;

import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.gui.AchievementGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles the /achievements command and its subcommands.
 */
public class AchievementCommand implements CommandExecutor {

    private final BetterAchievements plugin;
    private final AchievementGUI gui;

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
        
        // Handle reload subcommand
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("betterachievements.admin")) {
                sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.no_permission"));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessage("prefix") + plugin.getMessage("achievement.reload"));
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
}
