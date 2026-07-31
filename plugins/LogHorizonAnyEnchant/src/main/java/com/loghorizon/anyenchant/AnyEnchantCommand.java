package com.loghorizon.anyenchant;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class AnyEnchantCommand implements CommandExecutor, TabCompleter {
    private final LogHorizonAnyEnchant plugin;
    private final EnchantBridge bridge;

    AnyEnchantCommand(LogHorizonAnyEnchant plugin, EnchantBridge bridge) {
        this.plugin = plugin;
        this.bridge = bridge;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase();
        return switch (subcommand) {
            case "status" -> status(sender);
            case "refresh" -> refresh(sender);
            case "cleanup" -> cleanup(sender);
            default -> {
                sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <status|refresh|cleanup>");
                yield true;
            }
        };
    }

    private boolean status(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.YELLOW + "Execute como jogador para consultar seus itens equipados.");
            return true;
        }
        Map<Enchantment, Integer> levels = bridge.inspect(player);
        sender.sendMessage(ChatColor.GOLD + "Log Horizon AnyEnchant " + ChatColor.WHITE + plugin.getPluginMeta().getVersion());
        if (levels.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nenhum encantamento vanilla equipado foi detectado.");
            return true;
        }
        sender.sendMessage(ChatColor.GRAY + "Maior nível equipado por encantamento:");
        levels.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getKey().toString()))
                .forEach(entry -> sender.sendMessage(ChatColor.AQUA + "- "
                        + entry.getKey().getKey().getKey() + " " + entry.getValue()));
        return true;
    }

    private boolean refresh(CommandSender sender) {
        plugin.reloadLocalConfig();
        bridge.start();
        plugin.getServer().getOnlinePlayers().forEach(bridge::refresh);
        sender.sendMessage(ChatColor.GREEN + "Configuração recarregada e encantamentos atualizados.");
        return true;
    }

    private boolean cleanup(CommandSender sender) {
        plugin.getServer().getOnlinePlayers().forEach(bridge::restore);
        sender.sendMessage(ChatColor.GREEN + "Encantamentos sintéticos removidos dos jogadores online.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("status", "refresh", "cleanup").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
