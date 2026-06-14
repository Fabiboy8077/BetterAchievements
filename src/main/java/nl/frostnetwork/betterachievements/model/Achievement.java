package nl.frostnetwork.betterachievements.model;

import org.bukkit.Material;

/**
 * Represents an achievement within the plugin.
 */
public class Achievement {
    private final String id;
    private final String name;
    private final String task;
    private final String type;
    private final String target;
    private final int required;
    private final int order;
    private final Material guiItem;
    private final String sneakpeekHint;
    private final String rewardId;

    /**
     * Constructs a new Achievement.
     *
     * @param id             The unique identifier.
     * @param order          The unlock order.
     * @param name           The display name (translated).
     * @param task           The task description (translated).
     * @param type           The type of achievement (JOIN, BLOCK_BREAK, etc.).
     * @param target         The target material or entity.
     * @param required       The amount required for completion.
     * @param guiItem        The material used in the GUI.
     * @param sneakpeekHint  The hint shown when locked (translated).
     * @param rewardId       The ID of the associated reward.
     */
    public Achievement(String id, int order, String name, String task, String type, String target, int required, Material guiItem, String sneakpeekHint, String rewardId) {
        this.id = id;
        this.order = order;
        this.name = name;
        this.task = task;
        this.type = type;
        this.target = target;
        this.required = required;
        this.guiItem = guiItem;
        this.sneakpeekHint = sneakpeekHint;
        this.rewardId = rewardId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getTask() { return task; }
    public String getType() { return type; }
    public String getTarget() { return target; }
    public int getRequired() { return required; }
    public int getOrder() { return order; }
    public Material getGuiItem() { return guiItem; }
    public String getSneakpeekHint() { return sneakpeekHint; }
    public String getRewardId() { return rewardId; }
}
