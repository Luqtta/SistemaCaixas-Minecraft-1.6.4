package me.avelar.caixas;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CrateManager {

    private final Main plugin;
    private final Map<String, Crate> crates = new LinkedHashMap<String, Crate>();

    public CrateManager(Main plugin) {
        this.plugin = plugin;
    }

    public Collection<Crate> getCrates() {
        return crates.values();
    }

    public Crate getCrate(String id) {
        if (id == null) return null;
        return crates.get(id.toLowerCase());
    }

    public int countEnabledCrates() {
        int c = 0;
        for (Crate cr : crates.values()) if (cr.enabled) c++;
        return c;
    }

    public boolean deleteCrate(String id) {
        id = id.toLowerCase();
        if (plugin.getConfig().getConfigurationSection("crates") == null) return false;
        if (plugin.getConfig().getConfigurationSection("crates." + id) == null) return false;

        plugin.getConfig().set("crates." + id, null);
        plugin.saveConfig();
        reload();
        return true;
    }

    public void reload() {
        crates.clear();
        plugin.reloadConfig();

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("crates");
        if (sec == null) return;

        for (String id : sec.getKeys(false)) {
            ConfigurationSection c = sec.getConfigurationSection(id);
            if (c == null) continue;

            Crate crate = new Crate(id.toLowerCase());
            crate.enabled = c.getBoolean("enabled", true);

            crate.displayName = Util.color(c.getString("display.name", "&a" + id));
            crate.desc = Util.colorList(c.getStringList("display.desc"));
            crate.iconMaterial = c.getString("display.icon", "CHEST");
            crate.price = c.getDouble("price", 0.0);

            crate.rewards.clear();

            List<Map<?, ?>> list = c.getMapList("rewards");
            for (Map<?, ?> map : list) {
                try {
                    double chance = Double.parseDouble(String.valueOf(map.get("chance")));

                    boolean bc = map.containsKey("broadcast") &&
                            Boolean.parseBoolean(String.valueOf(map.get("broadcast")));

                    Reward.Rarity rarity = Reward.Rarity.COMMON;
                    if (map.containsKey("rarity")) {
                        try { rarity = Reward.Rarity.valueOf(String.valueOf(map.get("rarity")).toUpperCase()); }
                        catch (Exception ignored) {}
                    }

                    ItemStack it = mapToItem(map);
                    if (it != null) {
                        crate.rewards.add(new Reward(it, chance, bc, rarity));
                    }
                } catch (Exception ignored) {}
            }

            crates.put(crate.id, crate);
        }
    }

    public boolean createCrate(String id) {
        id = id.toLowerCase();
        if (crates.containsKey(id)) return false;

        plugin.getConfig().set("crates." + id + ".enabled", true);

        plugin.getConfig().set("crates." + id + ".display.name", "&a" + id);

        plugin.getConfig().set("crates." + id + ".display.desc", Arrays.asList("&7Sem descricao"));
        plugin.getConfig().set("crates." + id + ".display.icon", "CHEST");
        plugin.getConfig().set("crates." + id + ".price", 0.0);
        plugin.getConfig().set("crates." + id + ".rewards", new ArrayList<Object>());
        plugin.saveConfig();
        reload();
        return true;
    }

    public void saveCrateIcon(String id, String materialNameOrId) {
        plugin.getConfig().set("crates." + id + ".display.icon", materialNameOrId);
        plugin.saveConfig();
        reload();
    }

    public void setName(String id, String name) {
        plugin.getConfig().set("crates." + id + ".display.name", name);
        plugin.saveConfig();
        reload();
    }

    public void setDesc(String id, List<String> desc) {
        plugin.getConfig().set("crates." + id + ".display.desc", desc);
        plugin.saveConfig();
        reload();
    }

    public void setPrice(String id, double price) {
        plugin.getConfig().set("crates." + id + ".price", price);
        plugin.saveConfig();
        reload();
    }

    public void addReward(String id, ItemStack item, double chance, boolean broadcast, Reward.Rarity rarity) {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("crates." + id + ".rewards");

        Map<String, Object> map = itemToMap(item);
        map.put("chance", chance);
        map.put("broadcast", broadcast);
        map.put("rarity", rarity == null ? "COMMON" : rarity.name());

        list.add(map);

        plugin.getConfig().set("crates." + id + ".rewards", list);
        plugin.saveConfig();
        reload();
    }

    public boolean delReward(String id, int index) {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("crates." + id + ".rewards");
        if (index < 0 || index >= list.size()) return false;

        list.remove(index);
        plugin.getConfig().set("crates." + id + ".rewards", list);
        plugin.saveConfig();
        reload();
        return true;
    }

    public Reward roll(Crate crate, Random rnd) {
        double total = 0.0;
        for (Reward r : crate.rewards) total += Math.max(0.0, r.chance);
        if (total <= 0.0) return crate.rewards.get(0);

        double roll = rnd.nextDouble() * total;
        double acc = 0.0;

        for (Reward r : crate.rewards) {
            acc += Math.max(0.0, r.chance);
            if (roll <= acc) return r;
        }
        return crate.rewards.get(crate.rewards.size() - 1);
    }

    public ItemStack createIconItem(Crate crate) {
        String icon = crate.iconMaterial == null ? "CHEST" : crate.iconMaterial;

        if (icon.toUpperCase().startsWith("ID:")) {
            try {
                int id = Integer.parseInt(icon.substring(3).trim());
                return new ItemStack(id, 1, (short) 0);
            } catch (Exception ignored) {}
        }

        Material m = Material.matchMaterial(icon);
        if (m == null) m = Material.CHEST;
        return new ItemStack(m, 1);
    }

    private Map<String, Object> itemToMap(ItemStack it) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("typeId", it.getTypeId());
        map.put("durability", (int) it.getDurability());
        map.put("amount", it.getAmount());


        if (it.hasItemMeta()) {
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) map.put("name", meta.getDisplayName());
                if (meta.hasLore()) map.put("lore", meta.getLore());
            }
        }

        if (it.getEnchantments() != null && !it.getEnchantments().isEmpty()) {
            Map<String, Object> ench = new LinkedHashMap<String, Object>();
            for (Map.Entry<Enchantment, Integer> e : it.getEnchantments().entrySet()) {
                if (e.getKey() != null) ench.put(String.valueOf(e.getKey().getId()), e.getValue());
            }
            map.put("ench", ench);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private ItemStack mapToItem(Map<?, ?> map) {
        if (!map.containsKey("typeId")) return null;

        int typeId = Integer.parseInt(String.valueOf(map.get("typeId")));
        int amount = map.containsKey("amount") ? Integer.parseInt(String.valueOf(map.get("amount"))) : 1;
        int durability = map.containsKey("durability") ? Integer.parseInt(String.valueOf(map.get("durability"))) : 0;

        ItemStack it = new ItemStack(typeId, Math.max(1, amount), (short) durability);

        try {
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                if (map.containsKey("name")) meta.setDisplayName(String.valueOf(map.get("name")));
                if (map.containsKey("lore")) meta.setLore((List<String>) map.get("lore"));
                it.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}

        if (map.containsKey("ench")) {
            try {
                Map<?, ?> ench = (Map<?, ?>) map.get("ench");
                for (Map.Entry<?, ?> e : ench.entrySet()) {
                    int enchId = Integer.parseInt(String.valueOf(e.getKey()));
                    int lvl = Integer.parseInt(String.valueOf(e.getValue()));
                    Enchantment en = Enchantment.getById(enchId);
                    if (en != null) it.addUnsafeEnchantment(en, lvl);
                }
            } catch (Throwable ignored) {}
        }

        return it;
    }
}
