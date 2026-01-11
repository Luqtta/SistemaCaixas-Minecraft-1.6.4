package me.avelar.caixas;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Gui {

    public static void openMenuLive(Main plugin, Player p) {
        p.openInventory(buildMenu(plugin, p));
    }

    public static Inventory buildMenu(Main plugin, Player viewer) {
        String title = Util.color(plugin.getConfig().getString("settings.menu-title", "&8Caixas"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        boolean hasEco = (plugin.getEconomy() != null);
        double bal = hasEco ? plugin.getEconomy().getBalance(viewer.getName()) : 0.0;

        int[] slots = {11, 13, 15};
        int idx = 0;

        for (Crate crate : plugin.getCrateManager().getCrates()) {
            if (!crate.enabled) continue;
            if (idx >= slots.length) break;

            ItemStack icon = plugin.getCrateManager().createIconItem(crate);

            List<String> lore = new ArrayList<String>();
            if (crate.desc != null) lore.addAll(crate.desc);

            lore.add("&8");
            if (hasEco) {
                boolean can = bal >= crate.price;
                String moneyColor = can ? "&a" : "&c";
                lore.add("&7Preco: &f$ " + crate.price + " &8/ " + moneyColor + "$ " + bal);
                lore.add(can ? "&aVoce possui dinheiro" : "&cVoce nao possui dinheiro");
            } else {
                lore.add("&cEconomia nao configurada.");
            }

            lore.add("&8");
            lore.add("&eBotao esquerdo: &7Comprar");
            lore.add("&eBotao direito: &7Ver premios");
            lore.add("&8crate:" + crate.id);

            Util.setNameLore(icon, "&7Caixa &a" + crate.displayName, lore);
            inv.setItem(slots[idx], icon);
            idx++;
        }

        return inv;
    }

    public static Inventory buildEditItems(Main plugin, Crate crate) {
        Inventory inv = Bukkit.createInventory(null, 54, Util.color("&fEditar Itens: " + crate.id));

        int slot = 0;
        int rewardIndex = 0;

        for (Reward r : crate.rewards) {
            if (slot > 44) break;

            ItemStack it = r.item.clone();
            List<String> lore = new ArrayList<String>();

            lore.add("&7Chance: " + Util.getChanceColor(r.chance) + r.chance);
            if (r.chance < 10.0) {
                lore.add("&8");
                String lbl = Util.getChanceLabel(r.chance);
                if (lbl != null) lore.add(lbl);
            } else {
                lore.add("&8");
            }
            lore.add("&eDIR: &aEditar chance");
            lore.add("&eESQ: &cRemover");
            lore.add("&8rid:" + rewardIndex);

            Util.setNameLore(it,
                    (it.hasItemMeta() && it.getItemMeta().hasDisplayName())
                            ? it.getItemMeta().getDisplayName()
                            : "&fItem",
                    lore);

            inv.setItem(slot, it);
            slot++;
            rewardIndex++;
        }

        ItemStack close = new ItemStack(Material.REDSTONE, 1);
        List<String> lore = new ArrayList<String>();
        lore.add("&7Clique para fechar");
        Util.setNameLore(close, "&cFechar", lore);
        inv.setItem(49, close);

        return inv;
    }

    public static Inventory buildRoulette(Main plugin) {
        String title = Util.color(plugin.getConfig().getString("settings.roulette-title", "&8Roleta"));
        return Bukkit.createInventory(null, 27, title);
    }

    public static Inventory buildRewards(Main plugin, Crate crate) {
        String tpl = plugin.getConfig().getString("settings.rewards-title", "&8Premios: {caixa}");
        String title = Util.color(tpl.replace("{caixa}", crate.displayName));

        Inventory inv = Bukkit.createInventory(null, 54, title);

        int slot = 0;
        for (Reward r : crate.rewards) {
            if (slot >= inv.getSize()) break;

            ItemStack it = r.item.clone();
            List<String> lore = new ArrayList<String>();
            lore.add("&7Chance: " + Util.getChanceColor(r.chance) + r.chance);
            if (r.chance < 10.0) {
                lore.add("&8");
                String lbl = Util.getChanceLabel(r.chance);
                if (lbl != null) lore.add(lbl);
            }

            Util.setNameLore(it,
                    (it.hasItemMeta() && it.getItemMeta().hasDisplayName())
                            ? it.getItemMeta().getDisplayName()
                            : "&fItem",
                    lore);

            inv.setItem(slot++, it);
        }

        return inv;
    }
}
