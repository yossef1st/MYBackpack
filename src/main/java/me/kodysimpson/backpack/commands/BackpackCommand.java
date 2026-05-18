package me.kodysimpson.backpack.commands;

import me.kodysimpson.backpack.Backpack;
import me.kodysimpson.backpack.BackpackTier;
import me.kodysimpson.backpack.managers.BackpackManager;
import me.kodysimpson.backpack.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BackpackCommand implements CommandExecutor, TabCompleter {

    private final BackpackManager backpackManager;
    private final Backpack plugin;

    public BackpackCommand(BackpackManager backpackManager, Backpack plugin) {
        this.backpackManager = backpackManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Lang.USAGE.format(plugin.isArabic()));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("backpack.reload")) {
                sender.sendMessage(Lang.NO_PERMISSION.format(plugin.isArabic()));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(Lang.RELOADED.format(plugin.isArabic()));
            return true;
        }

        if (!sender.hasPermission("backpack.admin")) {
            sender.sendMessage(Lang.NO_PERMISSION.format(plugin.isArabic()));
            return true;
        }

        // /backpack give <player> <small|medium|large|ender>
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(Lang.USAGE.format(plugin.isArabic()));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Lang.PLAYER_NOT_FOUND.format(plugin.isArabic(), "player", args[1]));
                return true;
            }

            BackpackTier tier = BackpackTier.fromString(args[2]);
            if (tier == null) {
                sender.sendMessage(Lang.INVALID_TIER.format(plugin.isArabic()));
                return true;
            }

            var backpack = backpackManager.createBackpack(tier);
            target.getInventory().addItem(backpack);
            target.sendMessage(Lang.RECEIVED.format(plugin.isArabic(), "tier", tier.getConfigKey()));
            sender.sendMessage(Lang.GIVEN.format(plugin.isArabic(), "player", target.getName(), "tier", tier.getConfigKey()));
            return true;
        }

        sender.sendMessage(Lang.USAGE.format(plugin.isArabic()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("backpack.admin")) subs.add("give");
            if (sender.hasPermission("backpack.reload")) subs.add("reload");
            if (subs.isEmpty()) subs.add("give");
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("backpack.admin")) {
            return null; // null = player names
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("backpack.admin")) {
            return List.of("small", "medium", "large", "ender");
        }
        return List.of();
    }
}
