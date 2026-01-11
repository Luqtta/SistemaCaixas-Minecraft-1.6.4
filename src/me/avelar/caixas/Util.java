package me.avelar.caixas;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static final String CAIXAS_PREFIX = color("&6&l[CAIXAS&6&l] ");

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> colorList(List<String> list) {
        List<String> out = new ArrayList<String>();
        if (list == null) return out;
        for (String s : list) out.add(color(s));
        return out;
    }

    /**
     * Returns an ampersand color code (like "&a") appropriate for the given chance.
     * Does not translate the code; caller should use Util.color(...) or rely on later coloring.
     */
    public static String getChanceColor(double chance) {
        if (Double.isNaN(chance)) return "&7";
        if (chance >= 50.0) return "&a";        // green - common
        if (chance >= 10.0) return "&e";        // yellow - uncommon
        if (chance >= 2.0)  return "&6";        // orange - rare
        return "&d&l";                         // bold purple - very rare
    }

    /**
     * Optional human label for low chances. Returns null for chances >= 10%.
     * The returned string contains ampersand color codes and will be colored by Util.color(...).
     */
    public static String getChanceLabel(double chance) {
        if (chance < 2.0) return "&d&lITEM MUITO RARO"; // very rare
        if (chance < 10.0) return "&6ITEM RARO";         // rare
        return null;
    }

    public static void setNameLore(ItemStack it, String name, List<String> lore) {
        if (it == null) return;
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> ll = new ArrayList<String>();
                for (String l : lore) ll.add(color(l));
                meta.setLore(ll);
            }
            it.setItemMeta(meta);
        }
    }

    public static boolean hasCrateId(ItemStack it) {
        return getCrateId(it) != null;
    }

    public static String getCrateId(ItemStack it) {
        if (it == null || it.getTypeId() == 0) return null;
        if (!it.hasItemMeta()) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        for (String line : meta.getLore()) {
            String raw = ChatColor.stripColor(line).toLowerCase().trim();

            if (raw.startsWith("crate:")) {
                return raw.substring("crate:".length()).trim();
            }

            if (raw.startsWith("caixa:")) {
                return raw.substring("caixa:".length()).trim();
            }
        }
        return null;
    }


    public static void playFirstAvailable(Player p, float vol, float pitch, String... names) {
        try {
            for (String n : names) {
                try {
                    p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(n), vol, pitch);
                    return;
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
