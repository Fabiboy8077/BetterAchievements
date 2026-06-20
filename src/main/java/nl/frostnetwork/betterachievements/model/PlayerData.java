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
    private long version = 0L;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public synchronized UUID getUuid() {
        return uuid;
    }

    public synchronized Map<String, Integer> getProgressMap() {
        return new HashMap<>(progress);
    }

    public synchronized int getProgress(String achievementId) {
        return progress.getOrDefault(achievementId, 0);
    }

    public synchronized void setProgress(String achievementId, int amount) {
        int sanitizedAmount = Math.max(0, amount);
        if (getProgress(achievementId) == sanitizedAmount) {
            return;
        }

        if (sanitizedAmount == 0) {
            progress.remove(achievementId);
        } else {
            progress.put(achievementId, sanitizedAmount);
        }
        markDirty();
    }

    public synchronized void addProgress(String achievementId, int amount) {
        if (amount <= 0) return;
        setProgress(achievementId, getProgress(achievementId) + amount);
    }

    public synchronized Set<String> getClaimedAchievements() {
        return new HashSet<>(claimedAchievements);
    }

    public synchronized int getClaimedCount() {
        return claimedAchievements.size();
    }

    public synchronized boolean isClaimed(String achievementId) {
        return claimedAchievements.contains(achievementId);
    }

    public synchronized void setClaimed(String achievementId) {
        if (claimedAchievements.add(achievementId)) {
            markDirty();
        }
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void setDirty(boolean dirty) {
        if (dirty && !this.dirty) {
            version++;
        }
        this.dirty = dirty;
    }

    public synchronized SaveSnapshot createSaveSnapshot() {
        if (!dirty) {
            return null;
        }
        return new SaveSnapshot(uuid, new HashMap<>(progress), new HashSet<>(claimedAchievements), version);
    }

    public synchronized void markSaved(long savedVersion) {
        if (version == savedVersion) {
            dirty = false;
        }
    }

    private void markDirty() {
        dirty = true;
        version++;
    }

    public static final class SaveSnapshot {
        private final UUID uuid;
        private final Map<String, Integer> progress;
        private final Set<String> claimedAchievements;
        private final long version;

        private SaveSnapshot(UUID uuid, Map<String, Integer> progress, Set<String> claimedAchievements, long version) {
            this.uuid = uuid;
            this.progress = progress;
            this.claimedAchievements = claimedAchievements;
            this.version = version;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Map<String, Integer> getProgress() {
            return progress;
        }

        public Set<String> getClaimedAchievements() {
            return claimedAchievements;
        }

        public long getVersion() {
            return version;
        }
    }
}
