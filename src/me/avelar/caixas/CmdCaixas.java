package me.avelar.caixas;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CmdCaixas implements CommandExecutor {

    private final Main plugin;

    public CmdCaixas(Main plugin) {
        this.plugin = plugin;
    }

    private boolean isAdmin(Player p) {
        return p.isOp() || p.hasPermission("caixas.admin");
    }

    private void sendLine(Player p) {
        p.sendMessage(Util.color("&8&m---------------------------------------"));
    }

    private void sendUserHelp(Player p) {
        sendLine(p);
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Use &e/caixas &7para ver as caixas disponiveis."));
        sendLine(p);
    }

    private void sendAdminHelp(Player p) {
        sendLine(p);
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&eComandos administrativos:"));
        p.sendMessage("");

        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas &f- Abrir menu de caixas"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas criar <id>"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar <id> &f(items, icon, nome, descricao, preco)"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas give <id> <qtd>"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas deletar <id> &c(minimo 3 caixas)"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas reload &f- Recarregar config.yml"));

        p.sendMessage("");
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Exemplos:"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&f/caixas editar normal items"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&f/caixas editar normal nome &eCaixa Normal"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&f/caixas editar normal preco &a1000"));

        sendLine(p);
    }

    private void sendEditHelp(Player p, String id) {
        sendLine(p);
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&eEditando a caixa &f" + id + "&e:"));
        p.sendMessage("");

        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar " + id + " items &f- Editar itens"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar " + id + " icon &f- Definir icone (item na mao)"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar " + id + " nome <texto>"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar " + id + " descricao <texto> &8(use \\n)"));
        p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7/caixas editar " + id + " preco <valor>"));

        sendLine(p);
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private boolean handleCaixas(Player p, String[] args, boolean calledFromCaixaAlias) {

        if (args.length == 0) {
            if (calledFromCaixaAlias) {
                if (!isAdmin(p)) sendUserHelp(p);
                else sendAdminHelp(p);
                return true;
            }
            Gui.openMenuLive(plugin, p);
            return true;
        }

        if (!isAdmin(p)) {
            sendUserHelp(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("criar")) {
            if (args.length < 2) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas criar <id>"));
                return true;
            }
            String id = args[1].toLowerCase();
            if (plugin.getCrateManager().createCrate(id)) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aCaixa criada: &f" + id));
            } else {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cEssa caixa ja existe."));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas give <id> <qtd>"));
                return true;
            }
            String id = args[1].toLowerCase();
            int qtd;
            try {
                qtd = Integer.parseInt(args[2]);
            } catch (Exception ex) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cQuantidade invalida."));
                return true;
            }

            Crate crate = plugin.getCrateManager().getCrate(id);
            if (crate == null) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cCaixa nao existe."));
                return true;
            }

            ItemStack it = Listeners.makeCrateItemStatic(plugin, crate);
            it.setAmount(Math.max(1, qtd));
            p.getInventory().addItem(it);

            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aVoce recebeu &f" + qtd + "x &a" + crate.displayName));
            return true;
        }

        if (args[0].equalsIgnoreCase("deletar") || args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas deletar <id>"));
                return true;
            }

            String id = args[1].toLowerCase();
            Crate crate = plugin.getCrateManager().getCrate(id);
            if (crate == null) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cCaixa nao existe."));
                return true;
            }

            int enabled = plugin.getCrateManager().countEnabledCrates();
            if (enabled <= 3 && crate.enabled) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cNao pode deletar. Deve sobrar no minimo &f3 &ccaixas."));
                return true;
            }

            boolean ok = plugin.getCrateManager().deleteCrate(id);
            if (ok) p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aCaixa deletada: &f" + id));
            else p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cFalha ao deletar a caixa."));
            return true;
        }

        if (args[0].equalsIgnoreCase("editar")) {
            if (args.length < 2) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas editar <id> (items|icon|nome|descricao|preco)"));
                return true;
            }

            String id = args[1].toLowerCase();
            Crate crate = plugin.getCrateManager().getCrate(id);
            if (crate == null) {
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cCaixa nao existe."));
                return true;
            }

            if (args.length == 2) {
                sendEditHelp(p, id);
                return true;
            }

            String sub = args[2].toLowerCase();

            if (sub.equals("items") || sub.equals("itens")) {
                p.openInventory(Gui.buildEditItems(plugin, crate));
                return true;
            }

            if (sub.equals("icon") || sub.equals("icone")) {
                ItemStack hand = p.getItemInHand();
                if (hand == null || hand.getTypeId() == 0) {
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cSegure um item na mao para definir como icone."));
                    return true;
                }

                if (hand.getType() != null && hand.getType() != Material.AIR) {
                    plugin.getCrateManager().saveCrateIcon(crate.id, hand.getType().name());
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aIcone definido: &f" + hand.getType().name()));
                } else {
                    plugin.getCrateManager().saveCrateIcon(crate.id, "ID:" + hand.getTypeId());
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aIcone (mod) definido: &fID:" + hand.getTypeId()));
                }
                return true;
            }

            if (sub.equals("nome") || sub.equals("name")) {
                if (args.length < 4) {
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas editar " + id + " nome <texto>"));
                    return true;
                }
                String name = joinFrom(args, 3);
                plugin.getCrateManager().setName(crate.id, name);
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aNome atualizado."));
                return true;
            }

            if (sub.equals("descricao") || sub.equals("desc") || sub.equals("description")) {
                if (args.length < 4) {
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas editar " + id + " descricao <texto>"));
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&7Dica: use \\n para quebrar linha."));
                    return true;
                }

                String text = joinFrom(args, 3);
                String[] lines = text.split("\\\\n");

                List<String> desc = new ArrayList<String>();
                for (String l : lines) desc.add(l);

                plugin.getCrateManager().setDesc(crate.id, desc);
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aDescricao atualizada."));
                return true;
            }

            if (sub.equals("preco") || sub.equals("price")) {
                if (args.length < 4) {
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cUso: /caixas editar " + id + " preco <valor>"));
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[3].replace(",", "."));
                } catch (Exception ex) {
                    p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cValor invalido."));
                    return true;
                }
                if (price < 0) price = 0;
                plugin.getCrateManager().setPrice(crate.id, price);
                p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aPreco atualizado: &f" + price));
                return true;
            }

            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cSubcomando invalido."));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
            plugin.reloadConfig();
            plugin.getCrateManager().reload();
            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&aConfiguracoes recarregadas com sucesso."));
            return true;
        }

        sendAdminHelp(p);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Apenas jogadores.");
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("caixas.use")) {
            p.sendMessage(Util.CAIXAS_PREFIX + Util.color("&cSem permissao."));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("caixa")) {
            if (!isAdmin(p) && args.length > 0) {
                sendUserHelp(p);
                return true;
            }
            return handleCaixas(p, args, true);
        }

        if (cmd.getName().equalsIgnoreCase("caixas")) {
            return handleCaixas(p, args, false);
        }

        return false;
    }
}
