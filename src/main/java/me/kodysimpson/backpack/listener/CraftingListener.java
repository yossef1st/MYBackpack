package me.kodysimpson.backpack.listener;

import me.kodysimpson.backpack.BackpackTier;
import me.kodysimpson.backpack.managers.BackpackManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftingListener implements Listener {

    private final BackpackManager backpackManager;
    private final org.bukkit.NamespacedKey backpackTierKey;

    public CraftingListener(BackpackManager backpackManager, JavaPlugin plugin) {
        this.backpackManager = backpackManager;
        this.backpackTierKey = new org.bukkit.NamespacedKey(plugin, "backpack_tier");
    }

    // ─────────────────────────────────────────────────────
    // PrepareItemCraft: يُشغَّل في كل مرة يتغير شيء
    // في طاولة الصنع أو عند فتح recipe book
    // ─────────────────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();

        // كشف الوصفة يدوياً من محتويات الشبكة (عشان نتعامل مع الحقائب المفتوحة)
        BackpackTier resultTier = detectRecipeFromMatrix(matrix);

        if (resultTier != null) {
            // وصفة صحيحة — اعطِ حقيبة جديدة (بدون backpack_id، ماكس 1)
            event.getInventory().setResult(backpackManager.createBackpack(resultTier));
        } else if (hasAnyPlayerHead(matrix)) {
            // في رأس لاعب بالشبكة لكن الوصفة غير صحيحة — أخفِ الناتج
            event.getInventory().setResult(null);
        }
        // الوصفات الصغيرة/الإندر تترك للـ ShapedRecipe (ما فيها رأس لاعب)
    }

    // ─────────────────────────────────────────────────────
    // CraftItemEvent: تحقق نهائي عند أخذ الناتج
    // ─────────────────────────────────────────────────────
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || !backpackManager.isBackpack(result)) return;

        if (!allIngredientsValid((CraftingInventory) event.getInventory())) {
            event.setCancelled(true);
        }
    }

    // ═══════════════════════════════════════════════
    // كشف الوصفة من الشبكة
    // ═══════════════════════════════════════════════

    private BackpackTier detectRecipeFromMatrix(ItemStack[] matrix) {
        if (matrix.length != 9) return null;

        // MEDIUM: DDD / D(B=S)D / DDD
        if (matchesShape(matrix,
            Material.DIAMOND, Material.DIAMOND, Material.DIAMOND,
            Material.DIAMOND, BackpackTier.SMALL, Material.DIAMOND,
            Material.DIAMOND, Material.DIAMOND, Material.DIAMOND
        )) return BackpackTier.MEDIUM;

        // LARGE: NNN / N(X=M)N / NNN
        if (matchesShape(matrix,
            Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT,
            Material.NETHERITE_INGOT, BackpackTier.MEDIUM, Material.NETHERITE_INGOT,
            Material.NETHERITE_INGOT, Material.NETHERITE_INGOT, Material.NETHERITE_INGOT
        )) return BackpackTier.LARGE;

        return null;
    }

    private boolean matchesShape(ItemStack[] matrix,
            Material m0, Material m1, Material m2,
            Material m3, BackpackTier centerTier, Material m5,
            Material m6, Material m7, Material m8) {

        Material[] expectedOuter = {m0, m1, m2, m3, null, m5, m6, m7, m8};

        for (int i = 0; i < 9; i++) {
            ItemStack item = matrix[i];

            if (i == 4) {
                // المنتصف — لازم حقيبة بالتير المطلوب
                if (item == null || !backpackManager.isBackpack(item)) return false;
                BackpackTier tier = backpackManager.getBackpackTier(item);
                if (tier != centerTier) return false;
            } else {
                // الأطراف — لازم المادة المطلوبة
                if (item == null || item.getType() != expectedOuter[i]) return false;
            }
        }
        return true;
    }

    private boolean hasAnyPlayerHead(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && item.getType() == Material.PLAYER_HEAD) return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════════
    // التحقق النهائي عند الأخذ
    // ═══════════════════════════════════════════════

    private boolean allIngredientsValid(CraftingInventory inventory) {
        ItemStack result = inventory.getResult();
        if (result == null) return false;

        BackpackTier resultTier = backpackManager.getBackpackTier(result);
        if (resultTier == null) return false;

        BackpackTier requiredTier = getRequiredIngredientTier(resultTier);
        if (requiredTier == null) return true;

        for (ItemStack ingredient : inventory.getMatrix()) {
            if (ingredient == null) continue;
            if (ingredient.getType() != Material.PLAYER_HEAD) continue;
            if (!ingredient.hasItemMeta()) return false;

            var pdc = ingredient.getItemMeta().getPersistentDataContainer();

            if (!pdc.has(backpackTierKey, PersistentDataType.STRING)) return false;

            String tierStr = pdc.get(backpackTierKey, PersistentDataType.STRING);
            if (tierStr == null) return false;

            BackpackTier ingredientTier = BackpackTier.fromString(tierStr);
            if (ingredientTier != requiredTier) return false;
        }
        return true;
    }

    private BackpackTier getRequiredIngredientTier(BackpackTier resultTier) {
        return switch (resultTier) {
            case MEDIUM -> BackpackTier.SMALL;
            case LARGE  -> BackpackTier.MEDIUM;
            default     -> null;
        };
    }
}
