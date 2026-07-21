package me.kodysimpson.backpack.managers;

import me.kodysimpson.backpack.Backpack;
import me.kodysimpson.backpack.BackpackTier;
import me.kodysimpson.backpack.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class BackpackManager {

    private final Backpack plugin;
    private final NamespacedKey backpackContentsKey;
    private final NamespacedKey backpackAutoCollectionKey;
    private final NamespacedKey backpackTierKey;
    private final Map<Inventory, BackpackSession> openBackpacks;

    public static final class BackpackSession {
        public final Player player;
        public final BackpackTier tier;
        public BackpackSession(Player player, BackpackTier tier) {
            this.player = player;
            this.tier = tier;
        }
    }

    // الأسماء الخاصة في الكونفيج للحقائب كمكوّن في الوصفة
    private static final Map<String, BackpackTier> CUSTOM_INGREDIENT_NAMES = new HashMap<>();
    static {
        CUSTOM_INGREDIENT_NAMES.put("SMALL_BACKPACK",  BackpackTier.SMALL);
        CUSTOM_INGREDIENT_NAMES.put("MEDIUM_BACKPACK", BackpackTier.MEDIUM);
        CUSTOM_INGREDIENT_NAMES.put("LARGE_BACKPACK",  BackpackTier.LARGE);
        CUSTOM_INGREDIENT_NAMES.put("ENDER_BACKPACK",  BackpackTier.ENDER);
    }

    public BackpackManager(Backpack plugin) {
        this.plugin = plugin;
        this.backpackContentsKey       = new NamespacedKey(plugin, "backpack_contents");
        this.backpackAutoCollectionKey = new NamespacedKey(plugin, "backpack_autocollect");
        this.backpackTierKey           = new NamespacedKey(plugin, "backpack_tier");
        this.openBackpacks             = new HashMap<>();
    }

    public NamespacedKey getBackpackTierKey() {
        return backpackTierKey;
    }

    // ═══════════════════════════════════════════
    // إنشاء عنصر الحقيبة
    // ═══════════════════════════════════════════
    public ItemStack createBackpack(BackpackTier tier) {
        String cfgPath = "backpacks." + tier.getConfigKey() + ".";
        String texture = plugin.getConfig().getString(cfgPath + "head-texture", "");
        String name    = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(cfgPath + "name", "&6Backpack"));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(cfgPath + "lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (!texture.isEmpty()) applyTexture(meta, texture);
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setMaxStackSize(1);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(backpackAutoCollectionKey, PersistentDataType.BOOLEAN, false);
            pdc.set(backpackTierKey,           PersistentDataType.STRING,  tier.getConfigKey());
            head.setItemMeta(meta);
        }
        return head;
    }

    // نسخة للوصفة — نفس PDC مثل createBackpack عشان ExactChoice يتطابق
    public ItemStack createBackpackForRecipe(BackpackTier tier) {
        String cfgPath = "backpacks." + tier.getConfigKey() + ".";
        String texture = plugin.getConfig().getString(cfgPath + "head-texture", "");
        String name    = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString(cfgPath + "name", "&6Backpack"));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList(cfgPath + "lore")) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (!texture.isEmpty()) applyTexture(meta, texture);
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setMaxStackSize(1);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(backpackTierKey, PersistentDataType.STRING,  tier.getConfigKey());
            pdc.set(backpackAutoCollectionKey, PersistentDataType.BOOLEAN, false);
            head.setItemMeta(meta);
        }
        return head;
    }

    private void applyTexture(SkullMeta meta, String base64Texture) {
        try {
            // بعض الـ textures تأتي كـ URL مشفّر بـ base64 وبعضها JSON مشفّر
            String decoded;
            try {
                decoded = new String(Base64.getDecoder().decode(base64Texture));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[Backpack] الـ texture غير صالح للفكّ: " + e.getMessage());
                return;
            }

            String skinUrl = null;

            // حالة 1: JSON كامل {"textures":{"SKIN":{"url":"..."}}}
            if (decoded.contains("\"url\":\"")) {
                int urlStart = decoded.indexOf("\"url\":\"") + 7;
                int urlEnd   = decoded.indexOf("\"", urlStart);
                if (urlStart > 6 && urlEnd > urlStart) {
                    skinUrl = decoded.substring(urlStart, urlEnd);
                }
            }
            // حالة 2: URL مباشر بعد الفكّ
            else if (decoded.startsWith("http")) {
                skinUrl = decoded.trim();
            }

            if (skinUrl == null) {
                plugin.getLogger().warning("[Backpack] لم يُعثر على URL داخل الـ texture.");
                return;
            }

            // UUID ثابت لكل skin URL عشان ExactChoice يتطابق في الـ recipe book
            PlayerProfile  profile  = Bukkit.createPlayerProfile(
                    UUID.nameUUIDFromBytes(skinUrl.getBytes()), "backpack");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(skinUrl));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);

        } catch (MalformedURLException e) {
            plugin.getLogger().warning("[Backpack] رابط الـ texture غير صالح: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    // تسجيل الوصفات
    // ═══════════════════════════════════════════
    public void registerRecipes() {
        for (BackpackTier tier : BackpackTier.values()) {
            String recipePath = "backpacks." + tier.getConfigKey() + ".recipe.";

            if (!plugin.getConfig().getBoolean(recipePath + "enabled", false)) {
                plugin.getLogger().info("[Backpack] وصفة " + tier.getConfigKey() + " معطّلة.");
                continue;
            }

            try {
                NamespacedKey recipeKey = new NamespacedKey(plugin, tier.getConfigKey() + "_backpack_recipe");
                ShapedRecipe  recipe    = new ShapedRecipe(recipeKey, createBackpack(tier));

                // ── الشكل ──
                List<String> shape = plugin.getConfig().getStringList(recipePath + "shape");
                if (shape.size() != 3) {
                    plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] shape يجب أن يكون 3 صفوف!");
                    continue;
                }
                boolean shapeOk = true;
                for (int i = 0; i < 3; i++) {
                    if (shape.get(i).length() != 3) {
                        plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] الصف " + (i+1) + " لازم يكون 3 أحرف. الحالي: \"" + shape.get(i) + "\"");
                        shapeOk = false;
                    }
                }
                if (!shapeOk) continue;
                recipe.shape(shape.get(0), shape.get(1), shape.get(2));

                // ── جمع الأحرف المستخدمة في الشكل فعلاً ──
                Set<Character> usedChars = new HashSet<>();
                for (String row : shape) {
                    for (char c : row.toCharArray()) {
                        if (c != ' ') usedChars.add(c);
                    }
                }

                // ── المواد ──
                var ingredientsSection = plugin.getConfig().getConfigurationSection(recipePath + "ingredients");
                if (ingredientsSection == null || ingredientsSection.getKeys(false).isEmpty()) {
                    plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] لا توجد مواد (ingredients)!");
                    continue;
                }

                boolean ingredientsOk = true;
                for (String key : ingredientsSection.getKeys(false)) {
                    if (key.length() != 1) {
                        plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] المفتاح يجب أن يكون حرف واحد: \"" + key + "\"");
                        ingredientsOk = false;
                        continue;
                    }

                    char slot = key.charAt(0);

                    // تجاهل الأحرف غير المستخدمة في الشيب (يمنع الخطأ الصامت)
                    if (!usedChars.contains(slot)) {
                        plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] الحرف '" + slot + "' معرّف في ingredients لكن غير موجود في shape — تم تجاهله.");
                        continue;
                    }

                    String matName = ingredientsSection.getString(key);
                    if (matName == null) continue;

                    // ── حقيبة مخصصة؟ ──
                    BackpackTier customTier = CUSTOM_INGREDIENT_NAMES.get(matName.toUpperCase());
                    if (customTier != null) {
                        recipe.setIngredient(slot, new RecipeChoice.ExactChoice(createBackpackForRecipe(customTier)));
                        plugin.getLogger().info("[Backpack] [" + tier.getConfigKey() + "] '" + key + "' = " + matName + " ✔");
                        continue;
                    }

                    // ── مادة عادية ──
                    Material mat = Material.getMaterial(matName.toUpperCase());
                    if (mat == null) {
                        plugin.getLogger().warning("[Backpack] [" + tier.getConfigKey() + "] المادة \"" + matName + "\" غير موجودة! راجع: https://minecraft.wiki/w/Item");
                        ingredientsOk = false;
                        continue;
                    }
                    recipe.setIngredient(slot, mat);
                }

                if (!ingredientsOk) continue;

                boolean added = Bukkit.addRecipe(recipe);
                if (added) {
                    plugin.getLogger().info("[Backpack] تم تسجيل وصفة " + tier.getConfigKey() + " بنجاح ✔");
                } else {
                    plugin.getLogger().warning("[Backpack] فشل addRecipe لـ " + tier.getConfigKey() + " — ربما تعارض مع وصفة أخرى.");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("[Backpack] خطأ في تسجيل وصفة " + tier.getConfigKey() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ═══════════════════════════════════════════
    // حجم الحقيبة
    // ═══════════════════════════════════════════
    public int getBackpackSize(BackpackTier tier) {
        int size = plugin.getConfig().getInt("backpacks." + tier.getConfigKey() + ".size", 9);
        if (size < 9)  size = 9;
        if (size > 54) size = 54;
        return (int) Math.round(size / 9.0) * 9;
    }

    public BackpackTier getBackpackTier(ItemStack item) {
        if (!isBackpack(item)) return null;
        String tierStr = item.getItemMeta().getPersistentDataContainer()
                .get(backpackTierKey, PersistentDataType.STRING);
        return BackpackTier.fromString(tierStr != null ? tierStr : "small");
    }

    public Inventory getBackpackInventory(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return null;
        var pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(backpackTierKey)) return null;

        BackpackTier tier  = getBackpackTier(itemStack);
        if (tier == null) tier = BackpackTier.SMALL;
        int    size  = getBackpackSize(tier);
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("backpacks." + tier.getConfigKey() + ".name", "&6Backpack"));

        Inventory inventory = Bukkit.createInventory(null, size, title);

        String itemContents = pdc.get(backpackContentsKey, PersistentDataType.STRING);
        if (itemContents != null) {
            inventory.setContents(ItemSerializer.itemStackArrayFromBase64(itemContents));
        }

        return inventory;
    }

    public void trackOpenInventory(Inventory inventory, Player player, BackpackTier tier) {
        openBackpacks.put(inventory, new BackpackSession(player, tier));
    }

    public BackpackSession getOpenSession(Inventory inventory) {
        return openBackpacks.remove(inventory);
    }

    public boolean isTrackedInventory(Inventory inventory) {
        return openBackpacks.containsKey(inventory);
    }

    public ItemStack findBackpackInInventory(Player player, BackpackTier tier) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() == Material.PLAYER_HEAD && isBackpack(cursor)) {
            String t = cursor.getItemMeta().getPersistentDataContainer()
                    .get(backpackTierKey, PersistentDataType.STRING);
            if (tier.getConfigKey().equals(t)) return cursor;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() == Material.PLAYER_HEAD && isBackpack(mainHand)) {
            String t = mainHand.getItemMeta().getPersistentDataContainer()
                    .get(backpackTierKey, PersistentDataType.STRING);
            if (tier.getConfigKey().equals(t)) return mainHand;
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() == Material.PLAYER_HEAD && isBackpack(offHand)) {
            String t = offHand.getItemMeta().getPersistentDataContainer()
                    .get(backpackTierKey, PersistentDataType.STRING);
            if (tier.getConfigKey().equals(t)) return offHand;
        }

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PLAYER_HEAD && isBackpack(item)) {
                String t = item.getItemMeta().getPersistentDataContainer()
                        .get(backpackTierKey, PersistentDataType.STRING);
                if (tier.getConfigKey().equals(t)) return item;
            }
        }

        return null;
    }

    public void saveBackpackContents(Inventory inventory, ItemStack backpack) {
        String itemsString = ItemSerializer.itemStackArrayToBase64(inventory.getContents());
        ItemMeta itemMeta  = backpack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(backpackContentsKey, PersistentDataType.STRING, itemsString);
        backpack.setItemMeta(itemMeta);
    }

    public boolean toggleAutoCollect(ItemStack itemStack) {
        ItemMeta itemMeta  = itemStack.getItemMeta();
        var      container = itemMeta.getPersistentDataContainer();
        boolean  current   = container.get(backpackAutoCollectionKey, PersistentDataType.BOOLEAN);
        container.set(backpackAutoCollectionKey, PersistentDataType.BOOLEAN, !current);
        itemStack.setItemMeta(itemMeta);
        return !current;
    }

    public boolean isBackpack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getItemMeta() == null) return false;
        return itemStack.getItemMeta().getPersistentDataContainer().has(backpackTierKey);
    }

    public ItemStack[] loadContents(ItemStack backpack) {
        if (!isBackpack(backpack) || backpack.getItemMeta() == null) return new ItemStack[0];
        String serialized = backpack.getItemMeta().getPersistentDataContainer()
                .get(backpackContentsKey, PersistentDataType.STRING);
        return serialized == null ? new ItemStack[0] : ItemSerializer.itemStackArrayFromBase64(serialized);
    }

    public boolean isAutoCollectEnabled(ItemStack backpack) {
        if (!isBackpack(backpack) || backpack.getItemMeta() == null) return false;
        return backpack.getItemMeta().getPersistentDataContainer()
                .getOrDefault(backpackAutoCollectionKey, PersistentDataType.BOOLEAN, false);
    }

    // ── تشخيص: طباعة كل الوصفات المسجلة في الكونسول ──
    public void debugRecipes(java.util.logging.Logger logger) {
        logger.info("[Backpack-DEBUG] الوصفات المسجلة:");
        for (BackpackTier tier : BackpackTier.values()) {
            NamespacedKey key = new NamespacedKey(plugin, tier.getConfigKey() + "_backpack_recipe");
            var recipe = Bukkit.getRecipe(key);
            if (recipe != null) {
                logger.info("[Backpack-DEBUG]  ✔ " + tier.getConfigKey() + " — مسجلة");
            } else {
                logger.warning("[Backpack-DEBUG]  ✘ " + tier.getConfigKey() + " — غير موجودة!");
            }
        }
    }
}
