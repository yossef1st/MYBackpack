package me.kodysimpson.backpack.listener;

import me.kodysimpson.backpack.Backpack;
import me.kodysimpson.backpack.BackpackTier;
import me.kodysimpson.backpack.managers.BackpackManager;
import me.kodysimpson.backpack.utils.Lang;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
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

        if (tier != null && tier.isEnder()) {
            event.getPlayer().openInventory(event.getPlayer().getEnderChest());
            return;
        }

        if (event.getPlayer().isSneaking()) {
            boolean newState = backpackManager.toggleAutoCollect(item);
            event.getPlayer().sendMessage(
                newState ? Lang.AUTO_COLLECT_ENABLED.format(plugin.isArabic())
                         : Lang.AUTO_COLLECT_DISABLED.format(plugin.isArabic())
            );
            return;
        }

        var inventory = backpackManager.getBackpackInventory(item);
        if (inventory != null) {
            backpackManager.trackOpenInventory(inventory, event.getPlayer(), tier);
            event.getPlayer().openInventory(inventory);
        }
    }

    @EventHandler
    public void onBackpackClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        var session = backpackManager.getOpenSession(event.getInventory());
        if (session == null) return;

        ItemStack backpack = backpackManager.findBackpackInInventory(session.player, session.tier);
        if (backpack != null) {
            backpackManager.saveBackpackContents(event.getInventory(), backpack);
        } else {
            plugin.getLogger().warning("[Backpack] لم يتم العثور على حقيبة " + session.tier.getConfigKey()
                    + " للاعب " + session.player.getName());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        var topInv = event.getView().getTopInventory();
        if (!backpackManager.isTrackedInventory(topInv)) return;
        if (event.getRawSlot() >= topInv.getSize()) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (isClickOnBackpack(current) || isClickOnBackpack(cursor)) {
            event.setCancelled(true);
            player.sendMessage(Lang.CANNOT_PLACE_BACKPACK.format(plugin.isArabic()));
        }
    }

    private boolean isClickOnBackpack(ItemStack item) {
        return item != null && item.getType() == Material.PLAYER_HEAD && backpackManager.isBackpack(item);
    }
}
