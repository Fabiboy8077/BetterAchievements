package nl.frostnetwork.betterachievements.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import nl.frostnetwork.betterachievements.BetterAchievements;
import nl.frostnetwork.betterachievements.model.Achievement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * PlaceholderAPI expansion for BetterAchievements.
 */
public class AchievementExpansion extends PlaceholderExpansion {

    private final BetterAchievements plugin;

    public AchievementExpansion(BetterAchievements plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "betterachievements";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FrostNetwork";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("completed")) {
            return String.valueOf(plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getClaimedAchievements().size());
        }

        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(plugin.getAchievementManager().getAchievements().size());
        }

        if (params.startsWith("progress_")) {
            String id = params.substring(9);
            return String.valueOf(plugin.getPlayerDataManager().getProgress(player.getUniqueId(), id));
        }

        return null;
    }
}
