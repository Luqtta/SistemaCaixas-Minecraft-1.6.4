package me.avelar.caixas;

import org.bukkit.inventory.ItemStack;

public class PendingChance {
    public final String crateId;
    public final ItemStack item;

    public PendingChance(String crateId, ItemStack item) {
        this.crateId = crateId;
        this.item = item;
    }
}
