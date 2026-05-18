package me.kodysimpson.backpack;

import me.kodysimpson.backpack.commands.BackpackCommand;
import me.kodysimpson.backpack.listener.AutoCollectListener;
import me.kodysimpson.backpack.listener.BackpackListener;
import me.kodysimpson.backpack.listener.CraftingListener;
import me.kodysimpson.backpack.managers.BackpackManager;
import me.kodysimpson.backpack.utils.Lang;
import org.bukkit.plugin.java.JavaPlugin;

public final class Backpack extends JavaPlugin {

    private BackpackManager backpackManager;
    private boolean arabic;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        arabic = getConfig().getString("lang", "ar").equalsIgnoreCase("ar");

        backpackManager = new BackpackManager(this);
        backpackManager.registerRecipes();

        getServer().getPluginManager().registerEvents(new BackpackListener(backpackManager, this), this);
        getServer().getPluginManager().registerEvents(new AutoCollectListener(backpackManager, this), this);
        getServer().getPluginManager().registerEvents(new CraftingListener(backpackManager, this), this);

        var cmd = getCommand("backpack");
        if (cmd != null) {
            var executor = new BackpackCommand(backpackManager, this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        backpackManager.debugRecipes(getLogger());
        getLogger().info(Lang.PLUGIN_ENABLED.format(arabic, "ver", getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        getLogger().info(Lang.PLUGIN_DISABLED.format(arabic));
    }

    public boolean isArabic() {
        return arabic;
    }
}
