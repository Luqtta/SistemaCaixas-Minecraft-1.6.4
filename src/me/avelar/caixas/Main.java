package me.avelar.caixas;


import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private Economy econ;
    private CrateManager crateManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupEconomy();

        crateManager = new CrateManager(this);
        crateManager.reload();
        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        CmdCaixas cmd = new CmdCaixas(this);
        if (getCommand("caixas") != null) getCommand("caixas").setExecutor(cmd);
        if (getCommand("caixa") != null) getCommand("caixa").setExecutor(cmd);

        getLogger().info("SistemaCaixas ligado. Vault: " + (econ != null));
        getLogger().info("Desenvolvido por avelar");
    }

    @Override
    public void onDisable() {
        getLogger().info("SistemaCaixas desligado.");
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            econ = null;
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            econ = null;
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }
}
