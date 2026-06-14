package nl.frostnetwork.betterachievements.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the data of a player, including progress and claimed achievements.
 */
public class PlayerData {
    private final UUID uuid;
    private final Map<String, Integer> progress = new HashMap<>();
    private final Set<String> claimedAchievements = new HashSet<>();
    private boolean dirty = false;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Map<String, Integer> getProgressMap() {
        return progress;
    }

    public int getProgress(String achievementId) {
        return progress.getOrDefault(achievementId, 0);
    }

    public void setProgress(String achievementId, int amount) {
        progress.put(achievementId, amount);
        this.dirty = true;
    }

    public void addProgress(String achievementId, int amount) {
        progress.put(achievementId, getProgress(achievementId) + amount);
        this.dirty = true;
    }

    public Set<String> getClaimedAchievements() {
        return claimedAchievements;
    }

    public boolean isClaimed(String achievementId) {
        return claimedAchievements.contains(achievementId);
    }

    public void setClaimed(String achievementId) {
        claimedAchievements.add(achievementId);
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
