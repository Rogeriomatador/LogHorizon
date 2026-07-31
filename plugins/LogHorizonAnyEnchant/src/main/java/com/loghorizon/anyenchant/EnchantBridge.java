package com.loghorizon.anyenchant;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EnchantBridge {
    static final int BOOTS_SLOT = 36;
    static final int LEGGINGS_SLOT = 37;
    static final int CHESTPLATE_SLOT = 38;
    static final int HELMET_SLOT = 39;
    static final int OFF_HAND_SLOT = 40;

    private static final Set<String> HELMET_ENCHANTMENTS = Set.of(
            "aqua_affinity", "respiration"
    );
    private static final Set<String> BOOTS_ENCHANTMENTS = Set.of(
            "feather_falling", "depth_strider", "frost_walker", "soul_speed"
    );
    private static final Set<String> LEGGINGS_ENCHANTMENTS = Set.of(
            "swift_sneak"
    );
    private static final Set<String> GENERAL_ARMOR_ENCHANTMENTS = Set.of(
            "protection", "fire_protection", "blast_protection",
            "projectile_protection", "thorns"
    );
    private static final Set<String> DURABILITY_ENCHANTMENTS = Set.of(
            "mending", "unbreaking"
    );

    private final LogHorizonAnyEnchant plugin;
    private final NamespacedKey stateKey;
    private int refreshTaskId = -1;

    EnchantBridge(LogHorizonAnyEnchant plugin, NamespacedKey stateKey) {
        this.plugin = plugin;
        this.stateKey = stateKey;
    }

    void start() {
        stop();
        long period = Math.max(1L, plugin.getConfig().getLong("refresh-ticks", 5L));
        refreshTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
                plugin,
                () -> plugin.getServer().getOnlinePlayers().forEach(this::refresh),
                1L,
                period
        );
    }

    void stop() {
        if (refreshTaskId >= 0) {
            plugin.getServer().getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
    }

    void refresh(Player player) {
        if (!player.isOnline()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        Set<Integer> equippedSlots = equippedSlots(inventory);
        Map<Integer, ItemStack> originalsBySlot = new HashMap<>();
        for (int slot : equippedSlots) {
            ItemStack item = inventory.getItem(slot);
            if (isUsable(item)) {
                originalsBySlot.put(slot, item);
            }
        }

        Map<Enchantment, Integer> globalLevels = collectGlobalLevels(originalsBySlot.values());
        Map<Integer, Map<Enchantment, Integer>> desiredBySlot = buildDesired(inventory, originalsBySlot, globalLevels);

        boolean changed = false;
        for (int slot = 0; slot <= OFF_HAND_SLOT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isUsable(item)) {
                continue;
            }
            Map<Enchantment, Integer> desired = desiredBySlot.getOrDefault(slot, Collections.emptyMap());
            if (SyntheticEnchantState.reconcile(item, desired, stateKey)) {
                inventory.setItem(slot, item);
                changed = true;
            }
        }

        if (changed && plugin.debug()) {
            plugin.getLogger().info("Atualizei encantamentos compatíveis de " + player.getName()
                    + ": " + format(globalLevels));
        }
    }

    void restore(Player player) {
        restoreInventory(player.getInventory());
    }

    void restoreInventory(PlayerInventory inventory) {
        for (int slot = 0; slot <= OFF_HAND_SLOT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isUsable(item)) {
                continue;
            }
            if (SyntheticEnchantState.restore(item, stateKey)) {
                inventory.setItem(slot, item);
            }
        }
    }

    void restoreItem(ItemStack item) {
        if (isUsable(item)) {
            SyntheticEnchantState.restore(item, stateKey);
        }
    }

    Map<Enchantment, Integer> inspect(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int slot : equippedSlots(inventory)) {
            ItemStack item = inventory.getItem(slot);
            if (isUsable(item)) {
                items.add(item);
            }
        }
        return collectGlobalLevels(items);
    }

    private Map<Enchantment, Integer> collectGlobalLevels(Collection<ItemStack> items) {
        Map<Enchantment, Integer> result = new HashMap<>();
        Set<String> ignored = new HashSet<>(plugin.getConfig().getStringList("ignored-enchantments"));
        boolean clamp = plugin.getConfig().getBoolean("clamp-to-vanilla-max", true);

        for (ItemStack item : items) {
            Map<Enchantment, Integer> originals = SyntheticEnchantState.originalEnchantments(item, stateKey);
            for (Map.Entry<Enchantment, Integer> entry : originals.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if (!"minecraft".equals(enchantment.getKey().getNamespace())) {
                    continue;
                }
                String key = enchantment.getKey().getKey();
                if (ignored.contains(key)) {
                    continue;
                }
                int level = entry.getValue();
                if (clamp) {
                    level = Math.min(level, enchantment.getMaxLevel());
                }
                if (level > 0) {
                    result.merge(enchantment, level, Math::max);
                }
            }
        }
        return result;
    }

    private Map<Integer, Map<Enchantment, Integer>> buildDesired(PlayerInventory inventory,
                                                                  Map<Integer, ItemStack> originalsBySlot,
                                                                  Map<Enchantment, Integer> globalLevels) {
        Map<Integer, Map<Enchantment, Integer>> desired = new HashMap<>();
        int mainSlot = inventory.getHeldItemSlot();
        boolean includeMain = plugin.getConfig().getBoolean("include-main-hand", true);
        boolean includeOff = plugin.getConfig().getBoolean("include-off-hand", true);
        boolean includeArmor = plugin.getConfig().getBoolean("include-armor", true);
        boolean propagateDurability = plugin.getConfig().getBoolean("propagate-durability-enchantments", true);

        for (Map.Entry<Enchantment, Integer> entry : globalLevels.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            String key = enchantment.getKey().getKey();

            if (DURABILITY_ENCHANTMENTS.contains(key) && propagateDurability) {
                for (int slot : equippedSlots(inventory)) {
                    addIfCompatible(desired, slot, originalsBySlot.get(slot), enchantment, level);
                }
                continue;
            }

            if (includeMain) {
                addIfCompatible(desired, mainSlot, originalsBySlot.get(mainSlot), enchantment, level);
            }
            if (includeOff) {
                addIfCompatible(desired, OFF_HAND_SLOT, originalsBySlot.get(OFF_HAND_SLOT), enchantment, level);
            }
            if (includeArmor) {
                int armorSlot = armorTargetSlot(key, originalsBySlot, enchantment);
                if (armorSlot >= 0) {
                    addDesired(desired, armorSlot, enchantment, level);
                }
            }
        }
        return desired;
    }

    private int armorTargetSlot(String enchantmentKey,
                                Map<Integer, ItemStack> originalsBySlot,
                                Enchantment enchantment) {
        if (HELMET_ENCHANTMENTS.contains(enchantmentKey)) {
            return compatibleSlot(HELMET_SLOT, originalsBySlot, enchantment);
        }
        if (BOOTS_ENCHANTMENTS.contains(enchantmentKey)) {
            return compatibleSlot(BOOTS_SLOT, originalsBySlot, enchantment);
        }
        if (LEGGINGS_ENCHANTMENTS.contains(enchantmentKey)) {
            return compatibleSlot(LEGGINGS_SLOT, originalsBySlot, enchantment);
        }
        if (GENERAL_ARMOR_ENCHANTMENTS.contains(enchantmentKey)) {
            for (int slot : new int[]{CHESTPLATE_SLOT, LEGGINGS_SLOT, HELMET_SLOT, BOOTS_SLOT}) {
                int compatible = compatibleSlot(slot, originalsBySlot, enchantment);
                if (compatible >= 0) {
                    return compatible;
                }
            }
        }
        return -1;
    }

    private int compatibleSlot(int slot,
                               Map<Integer, ItemStack> originalsBySlot,
                               Enchantment enchantment) {
        ItemStack item = originalsBySlot.get(slot);
        return isUsable(item) && safeCanEnchant(enchantment, item) ? slot : -1;
    }

    private void addIfCompatible(Map<Integer, Map<Enchantment, Integer>> desired,
                                 int slot,
                                 ItemStack item,
                                 Enchantment enchantment,
                                 int level) {
        if (isUsable(item) && safeCanEnchant(enchantment, item)) {
            addDesired(desired, slot, enchantment, level);
        }
    }

    private void addDesired(Map<Integer, Map<Enchantment, Integer>> desired,
                            int slot,
                            Enchantment enchantment,
                            int level) {
        desired.computeIfAbsent(slot, ignored -> new HashMap<>())
                .merge(enchantment, level, Math::max);
    }

    private boolean safeCanEnchant(Enchantment enchantment, ItemStack item) {
        try {
            return enchantment.canEnchantItem(item);
        } catch (RuntimeException exception) {
            if (plugin.debug()) {
                plugin.getLogger().warning("Não foi possível testar " + enchantment.getKey()
                        + " em " + item.getType() + ": " + exception.getMessage());
            }
            return false;
        }
    }

    private Set<Integer> equippedSlots(PlayerInventory inventory) {
        Set<Integer> slots = new HashSet<>();
        if (plugin.getConfig().getBoolean("include-main-hand", true)) {
            slots.add(inventory.getHeldItemSlot());
        }
        if (plugin.getConfig().getBoolean("include-off-hand", true)) {
            slots.add(OFF_HAND_SLOT);
        }
        if (plugin.getConfig().getBoolean("include-armor", true)) {
            slots.add(BOOTS_SLOT);
            slots.add(LEGGINGS_SLOT);
            slots.add(CHESTPLATE_SLOT);
            slots.add(HELMET_SLOT);
        }
        return slots;
    }

    private static boolean isUsable(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private static String format(Map<Enchantment, Integer> levels) {
        if (levels.isEmpty()) {
            return "nenhum";
        }
        return levels.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getKey().toString()))
                .map(entry -> entry.getKey().getKey().getKey() + " " + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("nenhum");
    }
}
