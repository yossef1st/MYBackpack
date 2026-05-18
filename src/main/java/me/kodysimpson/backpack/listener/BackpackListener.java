package me.kodysimpson.backpack.listener;

import me.kodysimpson.backpack.Backpack;
import me.kodysimpson.backpack.BackpackTier;
import me.kodysimpson.backpack.managers.BackpackManager;
import me.kodysimpson.backpack.utils.Lang;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BackpackListener implements Listener {

    private final BackpackManager backpackManager;
    private final Backpack plugin;

    public BackpackListener(BackpackManager backpackManager, Backpack plugin) {
        this.backpackManager = backpackManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !backpackManager.isBackpack(item)) return;

        event.setCancelled(true);

        BackpackTier tier = backpackManager.getBackpackTier(item);

        // ── حقيبة الإندر: تفتح مخزون Ender Chest الحقيقي للاعب ──
        if (tier != null && tier.isEnder()) {
            event.getPlayer().openInventory(event.getPlayer().getEnderChest());
            return;
        }

        // ── Shift + كليك: تفعيل/تعطيل الجمع التلقائي (للحقائب العادية فقط) ──
        if (event.getPlayer().isSneaking()) {
            boolean newState = backpackManager.toggleAutoCollect(item);
            event.getPlayer().sendMessage(
                newState ? Lang.AUTO_COLLECT_ENABLED.format(plugin.isArabic())
                         : Lang.AUTO_COLLECT_DISABLED.format(plugin.isArabic())
            );
            return;
        }

        // ── حقيبة عادية: فتح المخزون ──
        var inventory = backpackManager.getBackpackInventory(item);
        if (inventory != null) {
            backpackManager.trackOpenInventory(inventory, item);
            event.getPlayer().openInventory(inventory);
        }
    }

    @EventHandler
    public void onBackpackClose(InventoryCloseEvent event) {
        var backpack = backpackManager.getTrackedItem(event.getInventory());
        if (backpack == null) return; // مش حقيبة مفتوحة أو إندر
        backpackManager.saveBackpackContents(event.getInventory(), backpack);
    }
}
