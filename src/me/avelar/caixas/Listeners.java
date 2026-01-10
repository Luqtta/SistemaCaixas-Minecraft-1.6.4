package me.avelar.caixas;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Listeners implements Listener {

    private final Main plugin;
    private final Random rnd = new Random();

    private final Set<UUID> opening = new HashSet<UUID>();

    private final Map<UUID, PendingChance> pendingAddChance = new HashMap<UUID, PendingChance>();
    private final Map<UUID, PendingEditChance> pendingEditChance = new HashMap<UUID, PendingEditChance>();
    private final Map<UUID, PendingRemove> pendingRemove = new HashMap<UUID, PendingRemove>();

    public Listeners(Main plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(Player p) {
        return p.isOp() || p.hasPermission("caixas.admin");
    }

    private boolean isAir(ItemStack it) {
        return it == null || it.getTypeId() == 0 || it.getAmount() <= 0;
    }

    private boolean isReal(ItemStack it) {
        return it != null && it.getTypeId() != 0 && it.getAmount() > 0;
    }

    private String menuTitle() {
        return Util.color(plugin.getConfig().getString("settings.menu-title", "&8Caixas"));
    }

    private String rouletteTitle() {
        return Util.color(plugin.getConfig().getString("settings.roulette-title", "&8Roleta"));
    }

    private String rewardsTitleStart() {
        String tpl = Util.color(plugin.getConfig().getString("settings.rewards-title", "&8Premios: {caixa}"));
        String[] sp = tpl.split("\\{caixa\\}");
        return sp.length > 0 ? sp[0] : tpl;
    }

    private boolean isEditItemsGui(String title) {
        if (title == null) return false;
        return org.bukkit.ChatColor.stripColor(title).toLowerCase().startsWith("editar itens:");
    }

    private Crate getCrateFromEditTitle(String title) {
        String raw = org.bukkit.ChatColor.stripColor(title);
        String[] sp = raw.split(":");
        if (sp.length < 2) return null;
        return plugin.getCrateManager().getCrate(sp[1].trim().toLowerCase());
    }

    private Integer getRewardIndexFromGuiItem(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        ItemMeta meta = it.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;

        for (String line : meta.getLore()) {
            String raw = org.bukkit.ChatColor.stripColor(line).toLowerCase().trim();
            if (raw.startsWith("rid:")) {
                try {
                    return Integer.parseInt(raw.substring(4).trim());
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        String title = e.getInventory().getTitle();
        if (!isEditItemsGui(title)) return;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot >= 0 && rawSlot <= 53) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private void refreshMenuLive(Player p, Inventory inv) {
        Inventory fresh = Gui.buildMenu(plugin, p);
        inv.setContents(fresh.getContents());
        p.updateInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getInventory().getTitle();

        if (title.equals(menuTitle())) {
            e.setCancelled(true);

            if (e.getClick() == ClickType.NUMBER_KEY || e.getHotbarButton() >= 0) return;

            ItemStack clicked = e.getCurrentItem();
            if (isAir(clicked)) return;

            String crateId = Util.getCrateId(clicked);
            if (crateId == null) return;

            Crate crate = plugin.getCrateManager().getCrate(crateId);
            if (crate == null || !crate.enabled) return;

            if (e.isLeftClick()) {
                buyCrate(p, crate);
                refreshMenuLive(p, e.getInventory());
                return;
            }

            if (e.isRightClick()) {
                p.openInventory(Gui.buildRewards(plugin, crate));
                return;
            }
            return;
        }

        String start = rewardsTitleStart();
        if (start != null && start.length() > 0 && title.startsWith(start)) {
            e.setCancelled(true);
            return;
        }

        if (title.equals(rouletteTitle())) {
            e.setCancelled(true);
            return;
        }

        if (isEditItemsGui(title)) {
            int raw = e.getRawSlot();
            int topSize = e.getInventory().getSize();
            boolean clickedTop = raw < topSize;

            if (!clickedTop) return;

            if (e.isShiftClick()) {
                e.setCancelled(true);
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cShift-click desativado aqui."));
                return;
            }

            if (raw == 49) {
                e.setCancelled(true);
                p.closeInventory();
                return;
            }

            if (raw < 0 || raw > 44) {
                e.setCancelled(true);
                return;
            }

            e.setCancelled(true);

            Crate crate = getCrateFromEditTitle(title);
            if (crate == null) return;

            ItemStack clicked = e.getCurrentItem();
            ItemStack cursor = p.getItemOnCursor();


            if (isAir(clicked) && isReal(cursor)) {
                ItemStack stack = cursor.clone();

                p.setItemOnCursor(null);

                p.closeInventory();
                pendingAddChance.put(p.getUniqueId(), new PendingChance(crate.id, stack));

                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Digite no chat (somente numero) a &fchance&7 do item."));
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Para cancelar digite &fcancelar&7."));
                return;
            }

            if (!isAir(clicked)) {
                Integer idx = getRewardIndexFromGuiItem(clicked);
                if (idx == null) idx = raw;
                if (idx < 0 || idx >= crate.rewards.size()) return;


                if (e.isRightClick()) {
                    p.closeInventory();
                    pendingEditChance.put(p.getUniqueId(), new PendingEditChance(crate.id, idx));

                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Digite no chat (somente numero) a &fNOVA chance&7 do item."));
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Para cancelar digite &fcancelar&7."));
                    return;
                }

                if (e.isLeftClick()) {
                    p.closeInventory();
                    pendingRemove.put(p.getUniqueId(), new PendingRemove(crate.id, idx));

                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cTem certeza que deseja remover este item?"));
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Digite &aSIM &7para confirmar ou &cCANCELAR &7para cancelar."));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        final String msg = e.getMessage().trim();

        final PendingRemove rem = pendingRemove.get(p.getUniqueId());
        if (rem != null) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("sim") || msg.equalsIgnoreCase("s")) {
                pendingRemove.remove(p.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        plugin.getCrateManager().delReward(rem.crateId, rem.index);
                        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aItem removido com sucesso."));

                        Crate crate = plugin.getCrateManager().getCrate(rem.crateId);
                        if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                    }
                });
                return;
            }

            if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("nao") || msg.equalsIgnoreCase("não") || msg.equalsIgnoreCase("n")) {
                pendingRemove.remove(p.getUniqueId());
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cRemocao cancelada."));

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        Crate crate = plugin.getCrateManager().getCrate(rem.crateId);
                        if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                    }
                });
                return;
            }

            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cResposta invalida. Digite &aSIM &cou &cCANCELAR&c."));
            return;
        }

        final PendingEditChance edit = pendingEditChance.get(p.getUniqueId());
        if (edit != null) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("cancelar")) {
                pendingEditChance.remove(p.getUniqueId());
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cAcao cancelada."));

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        Crate crate = plugin.getCrateManager().getCrate(edit.crateId);
                        if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                    }
                });
                return;
            }

            double chance;
            try {
                chance = Double.parseDouble(msg.replace(",", "."));
            } catch (Exception ex) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cValor invalido. Digite novamente ou &fcancelar&c."));
                return;
            }

            if (chance <= 0 || chance > 100000) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cValor invalido. Digite novamente ou &fcancelar&c."));
                return;
            }

            pendingEditChance.remove(p.getUniqueId());
            final double finalChance = chance;

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    updateRewardChance(edit.crateId, edit.index, finalChance);
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aChance atualizada para &f" + finalChance + "&a."));

                    Crate crate = plugin.getCrateManager().getCrate(edit.crateId);
                    if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                }
            });
            return;
        }

        final PendingChance pending = pendingAddChance.get(p.getUniqueId());
        if (pending != null) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("cancelar")) {
                pendingAddChance.remove(p.getUniqueId());
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cAcao cancelada."));

                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        ItemStack toReturn = pending.item.clone();


                        Map<Integer, ItemStack> leftover = p.getInventory().addItem(toReturn);
                        if (leftover != null && !leftover.isEmpty()) {
                            for (ItemStack rem : leftover.values()) {
                                p.getWorld().dropItemNaturally(p.getLocation(), rem);
                            }
                        }

                        p.updateInventory();

                        final Crate crate = plugin.getCrateManager().getCrate(pending.crateId);

                        
                        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                            @Override public void run() {
                                if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                            }
                        }, 1L);
                    }
                });
                return;
            }

            double chance;
            try {
                chance = Double.parseDouble(msg.replace(",", "."));
            } catch (Exception ex) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cValor invalido. Digite novamente ou &fcancelar&c."));
                return;
            }

            if (chance <= 0 || chance > 100000) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cValor invalido. Digite novamente ou &fcancelar&c."));
                return;
            }

            pendingAddChance.remove(p.getUniqueId());
            final double finalChance = chance;

            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    plugin.getCrateManager().addReward(pending.crateId, pending.item, finalChance, false, Reward.Rarity.COMMON);
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aItem adicionado com chance &f" + finalChance + "&a."));

                    Crate crate = plugin.getCrateManager().getCrate(pending.crateId);
                    if (crate != null) p.openInventory(Gui.buildEditItems(plugin, crate));
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void updateRewardChance(String crateId, int index, double chance) {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("crates." + crateId + ".rewards");
        if (index < 0 || index >= list.size()) return;

        Map<Object, Object> map = (Map<Object, Object>) list.get(index);
        map.put("chance", chance);

        plugin.getConfig().set("crates." + crateId + ".rewards", list);
        plugin.saveConfig();
        plugin.getCrateManager().reload();
    }

    private void buyCrate(Player p, Crate crate) {
        boolean admin = isAdmin(p);

        if (!admin) {
            if (plugin.getEconomy() == null) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cEconomia nao configurada."));
                return;
            }

            double bal = plugin.getEconomy().getBalance(p.getName());
            if (bal < crate.price) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cVoce nao tem dinheiro pra comprar essa caixa."));
                Util.playFirstAvailable(p, 1f, 0.7f, "NOTE_BASS", "NOTE_BASS_GUITAR", "VILLAGER_NO");
                return;
            }

            plugin.getEconomy().withdrawPlayer(p.getName(), crate.price);
        }

        ItemStack box = makeCrateItemStatic(plugin, crate);
        p.getInventory().addItem(box);

        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aVoce comprou a caixa &f" + crate.displayName));
        Util.playFirstAvailable(p, 1f, 1.2f, "LEVEL_UP", "NOTE_PLING", "ORB_PICKUP");
    }

    public static ItemStack makeCrateItemStatic(Main plugin, Crate crate) {
        ItemStack it = plugin.getCrateManager().createIconItem(crate);

        List<String> lore = new ArrayList<String>();
        lore.add("&7Clique direito para abrir");
        lore.add("&8crate:" + crate.id);

        Util.setNameLore(it, "&7Caixa &a" + crate.displayName, lore);
        return it;
    }

    @EventHandler
    public void onUseCrate(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack hand = p.getItemInHand();
        if (isAir(hand)) return;

        if (!Util.hasCrateId(hand)) return;

        e.setCancelled(true);

        String id = Util.getCrateId(hand);
        Crate crate = plugin.getCrateManager().getCrate(id);

        if (crate == null || !crate.enabled) {
            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cEssa caixa nao existe/esta desativada."));
            return;
        }
        if (crate.rewards.isEmpty()) {
            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cEssa caixa ainda nao tem premios configurados."));
            return;
        }
        if (opening.contains(p.getUniqueId())) {
            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cAguarde terminar a roleta."));
            Util.playFirstAvailable(p, 1f, 0.8f, "NOTE_BASS", "NOTE_BASS_GUITAR", "VILLAGER_NO");
            return;
        }

        int amt = hand.getAmount();
        if (amt <= 1) p.setItemInHand(null);
        else {
            hand.setAmount(amt - 1);
            p.setItemInHand(hand);
        }

        openRoulette(p, crate);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        if (e.getInventory() == null) return;

        if (rouletteTitle().equals(e.getInventory().getTitle())) {
            if (opening.contains(p.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override public void run() {
                        p.openInventory(Gui.buildRoulette(plugin));
                        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cAguarde terminar a roleta."));
                    }
                });
            }
            return;
        }
    }

    private void openRoulette(final Player p, final Crate crate) {
        opening.add(p.getUniqueId());

        final Inventory inv = Gui.buildRoulette(plugin);

        final ItemStack glass = new ItemStack(Material.THIN_GLASS, 1);
        Util.setNameLore(glass, "&7", Arrays.asList("&7"));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
        final ItemStack star = new ItemStack(Material.NETHER_STAR, 1);
        Util.setNameLore(star, "&e&lITEM GANHO", Arrays.asList("&7O premio aparece no &fmeio&7."));
        inv.setItem(4, star);
        inv.setItem(22, star.clone());

        final int[] row = {9,10,11,12,13,14,15,16,17};

        final Deque<Reward> strip = new ArrayDeque<Reward>();
        for (int i = 0; i < 9; i++) strip.addLast(plugin.getCrateManager().roll(crate, rnd));

        drawRow(inv, row, strip);
        p.openInventory(inv);

        new BukkitRunnable() {
            int spins = 0;
            int stepEvery = 1;
            int tickCounter = 0;

            int slow1 = 25, slow2 = 45, slow3 = 60;
            int stopAt = 85;

            @Override public void run() {
                tickCounter++;
                if (tickCounter % stepEvery != 0) return;

                strip.removeFirst();
                strip.addLast(plugin.getCrateManager().roll(crate, rnd));
                spins++;

                drawRow(inv, row, strip);
                inv.setItem(4, star);
                inv.setItem(22, star);

                Util.playFirstAvailable(p, 1f, 1.2f, "CLICK", "WOOD_CLICK", "UI_BUTTON_CLICK");

                if (spins == slow1) stepEvery = 2;
                if (spins == slow2) stepEvery = 3;
                if (spins == slow3) stepEvery = 4;

                if (spins >= stopAt) {
                    cancel();

                    Reward win = getAt(strip, 4);
                    ItemStack prize = win.item.clone();

                    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
                    inv.setItem(4, star);
                    inv.setItem(22, star.clone());

                    ItemStack preview = prize.clone();
                    preview.setAmount(1);
                    try {
                        ItemMeta pm = preview.getItemMeta();
                        if (pm != null) {
                            List<String> lore = pm.hasLore() ? new ArrayList<String>(pm.getLore()) : new ArrayList<String>();
                            lore.add(Util.color("&8(Preview - nao pegue)"));
                            pm.setLore(lore);
                            preview.setItemMeta(pm);
                        }
                    } catch (Throwable ignored) {}
                    inv.setItem(13, preview);

                    p.getInventory().addItem(prize.clone());

                    try {
                        String itemName;
                        ItemMeta prizeMeta = prize.getItemMeta();
                        if (prizeMeta != null && prizeMeta.hasDisplayName()) itemName = prizeMeta.getDisplayName();
                        else itemName = prize.getType().toString();
                        int amountWon = prize.getAmount();
                        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aVoce ganhou: &f" + itemName + " &ax" + amountWon + " &7(Chance: &f" + win.chance + "%&7)"));
                    } catch (Throwable ignored) {}

                    inv.setItem(13, null);
                    p.updateInventory();

                    if (win.chance < 25.0) {
                        spawnFireworks(p, 3);
                        broadcastRare(p, crate);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override public void run() {
                            opening.remove(p.getUniqueId());
                            if (p.getOpenInventory() != null) p.closeInventory();
                        }
                    }, 40L);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void drawRow(Inventory inv, int[] row, Deque<Reward> strip) {
        int i = 0;
        for (int slot : row) {
            Reward r = getAt(strip, i++);
            inv.setItem(slot, decoratePreview(r.item.clone(), r));
        }
    }

    private Reward getAt(Deque<Reward> dq, int index) {
        int i = 0;
        for (Reward r : dq) {
            if (i == index) return r;
            i++;
        }
        return dq.peekLast();
    }

    private ItemStack decoratePreview(ItemStack it, Reward r) {
        try {
            ItemMeta meta = it.getItemMeta();
            List<String> lore = (meta != null && meta.hasLore()) ? new ArrayList<String>(meta.getLore()) : new ArrayList<String>();
            lore.add(Util.color("&8"));
            lore.add(Util.color("&7Chance: &f" + r.chance));
            if (meta != null) {
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
        } catch (Throwable ignored) {}
        return it;
    }

    private void spawnFireworks(Player p, int amount) {
        try {
            for (int i = 0; i < amount; i++) {
                Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();

                meta.addEffect(org.bukkit.FireworkEffect.builder()
                        .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.ORANGE)
                        .withFade(Color.YELLOW)
                        .trail(true)
                        .flicker(true)
                        .build());

                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }
        } catch (Throwable ignored) {}
    }

    private void broadcastRare(Player p, Crate crate) {
        Bukkit.broadcastMessage(Util.CAIXAS_PREFIX + Util.color("&e" + p.getName()
                + " &7ganhou &fum item &7na caixa misteriosa &a" + crate.displayName));
        Bukkit.broadcastMessage(Util.CAIXAS_PREFIX + Util.color("&7Garanta ja a sua &e/caixas"));
    }
}
