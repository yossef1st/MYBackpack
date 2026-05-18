package me.kodysimpson.backpack.listener;

import me.kodysimpson.backpack.Backpack;
import me.kodysimpson.backpack.managers.BackpackManager;
import me.kodysimpson.backpack.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AutoCollectListener implements Listener {

    private final BackpackManager backpackManager;
    private final Backpack plugin;

    public AutoCollectListener(BackpackManager backpackManager, Backpack plugin) {
        this.backpackManager = backpackManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check main hand first, then offhand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        ItemStack backpack = null;
        if (backpackManager.isBackpack(mainHand) && backpackManager.isAutoCollectEnabled(mainHand)) {
            backpack = mainHand;
        } else if (backpackManager.isBackpack(offHand) && backpackManager.isAutoCollectEnabled(offHand)) {
            backpack = offHand;
        }

        if (backpack == null) return;

        var itemToAdd = event.getItem().getItemStack();
        // Don't collect backpacks into backpacks
        if (backpackManager.isBackpack(itemToAdd)) return;

        var backpackContents = backpackManager.loadContents(backpack);
        int size = backpackContents.length > 0 ? backpackContents.length : 9;
        var tempInventory = Bukkit.createInventory(null, size);
        tempInventory.setContents(backpackContents);

        if (hasRoom(tempInventory, itemToAdd)) {
            addItemToInventory(tempInventory, itemToAdd);
            backpackManager.saveBackpackContents(tempInventory, backpack);
            event.setCancelled(true);
            event.getItem().remove();
            player.sendMessage(Lang.COLLECTED.format(plugin.isArabic()));
        }
    }

    private boolean hasRoom(Inventory inventory, ItemStack item) {
        for (ItemStack content : inventory.getContents()) {
            if (content != null && content.isSimilar(item) && content.getAmount() < content.getMaxStackSize()) {
                return true;
            }
        }
        return inventory.firstEmpty() != -1;
    }

    private void addItemToInventory(Inventory inventory, ItemStack item) {
        for (ItemStack content : inventory.getContents()) {
            if (content != null && content.isSimilar(item) && content.getAmount() < content.getMaxStackSize()) {
                int canAdd = content.getMaxStackSize() - content.getAmount();
                if (canAdd >= item.getAmount()) {
                    content.setAmount(content.getAmount() + item.getAmount());
                    return;
                }
            }
        }
        inventory.addItem(item);
    }
}
