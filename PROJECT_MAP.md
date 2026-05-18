# Backpack Plugin — Project Map

## Overview
Spigot 1.21.4 plugin for multi-tier backpacks with custom player head textures, auto-collect, and crafting.

## Source Structure

### Main — `me.kodysimpson.backpack`
- `Backpack.java` — Plugin entry point, enables managers & listeners
- `BackpackTier.java` — Enum: `SMALL`, `MEDIUM`, `LARGE`, `ENDER`

### Managers — `me.kodysimpson.backpack.managers`
- `BackpackManager.java` — Core logic:
  - Creates backpack items with PDC (UUID, tier, auto-collect, contents)
  - Manages inventories (`HashMap<UUID, Inventory>`)
  - Registers crafting recipes (shape + ingredients)
  - Serialization via `ItemSerializer`

### Listeners — `me.kodysimpson.backpack.listener`
- `BackpackListener.java` — Right-click open, shift-toggle auto-collect, close-save
- `CraftingListener.java` — Validates backpack ingredients in PrepareItemCraft & CraftItem
- `AutoCollectListener.java` — Auto-pickup items into held backpack

### Commands — `me.kodysimpson.backpack.commands`
- `BackpackCommand.java` — `/backpack give <player> <tier>`

### Utils — `me.kodysimpson.backpack.utils`
- `ItemSerializer.java` — Base64 serialization of ItemStack arrays

## Data Flow
1. **Creation**: `createBackpack(tier)` → PLAYER_HEAD with skull texture + PDC (UUID, tier, auto-collect)
2. **Open**: `BackpackListener.onPlayerInteract` → `getBackpackInventory(item)` → lazy-create `Inventory` keyed by UUID → open
3. **Close**: `onBackpackClose` → `saveBackpackContents(inv, item)` → serialize inventory → store in item PDC
4. **Craft**: Recipe uses `createBackpackTemplate(tier)` as result. `CraftingListener` validates ingredients.
5. **Auto-Collect**: When enabled, picked-up items route into backpack inventory (serialized).

## Persistent Data Keys
- `backpack_id` (String) — UUID unique per backpack instance
- `backpack_tier` (String) — Config key: small/medium/large/ender
- `backpack_contents` (String) — Base64 serialized inventory
- `backpack_autocollect` (Boolean) — Auto-collect toggle

## Recipe System
- Config-driven shapes + ingredients in `config.yml`
- Custom ingredients: `SMALL_BACKPACK`, `MEDIUM_BACKPACK`, etc.
- Recipe book uses `RecipeChoice.ExactChoice(createBackpackForRecipe(tier))` for backpack ingredients — shows the actual backpack skull instead of a generic player head (Fix 1)
- `ExactChoice` matches via `isSimilar()` — PDC and skull profile must match:
  - `applyTexture()` now uses `UUID.nameUUIDFromBytes(skinUrl.getBytes())` for deterministic skull profiles across all backpacks of the same tier
  - `createBackpack()` and `createBackpackForRecipe()` share identical PDC structure (no `backpack_id` initially) → `isSimilar()` matches

## Recent Changes (completed this session)

### Fix 1 — Recipe book shows backpack instead of player head
- Changed `registerRecipes()` in `BackpackManager` to use `RecipeChoice.ExactChoice(createBackpackForRecipe(customTier))` instead of `MaterialChoice(PLAYER_HEAD)` for custom backpack ingredients
- `applyTexture()`: switched from `UUID.randomUUID()` to `UUID.nameUUIDFromBytes(skinUrl)` so all backpacks of the same tier share the same skull profile → `ExactChoice.isSimilar()` matches

### Fix 2 — Backpacks not stackable
- Added `meta.setMaxStackSize(1)` in `createBackpack()`, `createBackpackForRecipe()`, `createBackpackTemplate()`

### Fix 3 — Each backpack has separate storage (except ender)
- `backpack_id` PDC key removed **entirely** → no PDC difference between fresh/opened backpacks
- Inventory tracking now uses `Map<Inventory, ItemStack> openBackpacks` (runtime map, not PDC-based):
  - `getBackpackInventory()` creates a fresh `Inventory` each time from saved `backpack_contents` PDC
  - `trackOpenInventory(inv, item)` stores the link at open time
  - `getTrackedItem(inv)` retrieves and removes the link on close (called from `BackpackListener`)
- `CraftingListener.onPrepare()` rewritten — **detects recipe by scanning the matrix directly** (`detectRecipeFromMatrix()`) instead of relying on `ExactChoice`:
  - `matchesShape()` validates ALL 9 slots (center = correct backpack tier, outer = correct material)
  - Works for both fresh AND opened backpacks (ignores `backpack_contents` PDC)
  - Small/ender recipes are left to the ShapedRecipe system (no player heads involved)
  - Invalid player heads in grid → result nullified
- **Result**: opened backpacks (with items inside) can be used as crafting ingredients for upgrades

## Removed Code
- `BackpackRecipeChoice.java` — deleted (replaced by `ExactChoice`)
- `TEMPLATE_UUIDS` + `createBackpackTemplate()` — replaced by `createBackpack()` as recipe result
- `getBackpackId()`, `isBackpackItemById()`, `getBackpackIdKey()` — orphaned, removed
- `backpackIdKey` NamespacedKey — removed from BackpackManager and CraftingListener
- `HashMap<UUID, Inventory> backpackInventories` — replaced by `Map<Inventory, ItemStack> openBackpacks`

## Deprecated / Dead Code
- `createDisplayTemplate()` — unused, left unchanged per surgical rules
