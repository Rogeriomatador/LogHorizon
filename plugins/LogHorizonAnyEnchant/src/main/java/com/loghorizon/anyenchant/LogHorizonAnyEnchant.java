package com.loghorizon.anyenchant;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class LogHorizonAnyEnchant extends JavaPlugin {
    private EnchantBridge bridge;
    private boolean debug;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadLocalConfig();

        NamespacedKey stateKey = new NamespacedKey(this, "synthetic_enchants");
        bridge = new EnchantBridge(this, stateKey);

        getServer().getPluginManager().registerEvents(new AnyEnchantListener(this, bridge), this);
        AnyEnchantCommand executor = new AnyEnchantCommand(this, bridge);
        PluginCommand command = getCommand("lhae");
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        bridge.start();
        getLogger().info("LogHorizonAnyEnchant 1.0.0-LH1 ativado. "
                + "Encantamentos vanilla equipados serão espelhados para o slot compatível usando o maior nível.");
    }

    @Override
    public void onDisable() {
        if (bridge != null) {
            bridge.stop();
            getServer().getOnlinePlayers().forEach(bridge::restore);
        }
    }

    void reloadLocalConfig() {
        reloadConfig();
        debug = getConfig().getBoolean("debug", false);
    }

    boolean debug() {
        return debug;
    }
}
