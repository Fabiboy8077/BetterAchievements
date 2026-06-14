package nl.frostnetwork.betterachievements.model;

import java.util.List;

/**
 * Represents a reward that can be given to a player.
 */
public class Reward {
    private final double money;
    private final List<String> items; // Material:Amount
    private final List<String> commands;

    public Reward(double money, List<String> items, List<String> commands) {
        this.money = money;
        this.items = items;
        this.commands = commands;
    }

    public double getMoney() { return money; }
    public List<String> getItems() { return items; }
    public List<String> getCommands() { return commands; }
}
