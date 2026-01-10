package me.avelar.caixas;

import org.bukkit.inventory.ItemStack;

public class Reward {

    public enum Rarity {
        COMMON, EPIC, LEGENDARY
    }

    public final ItemStack item;
    public final double chance;
    public final boolean broadcast;
    public final Rarity rarity;

    public Reward(ItemStack item, double chance, boolean broadcast, Rarity rarity) {
        this.item = item;
        this.chance = chance;
        this.broadcast = broadcast;
        this.rarity = rarity;
    }
}
