/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.matsubara.realisticvillagers.manager.gift;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.manager.gift.Gift;
import me.matsubara.realisticvillagers.manager.gift.GiftCategory;
import me.matsubara.realisticvillagers.util.PluginUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GiftManager {
    private final RealisticVillagers plugin;
    private final Map<Material, Gift> gifts = new EnumMap<Material, Gift>(Material.class);
    private final Map<UUID, Map<UUID, DailyGiftData>> dailyCaps = new HashMap<UUID, Map<UUID, DailyGiftData>>();
    private int defaultCooldownSeconds = 300;
    private int maxGain = 15;
    private int maxLoss = 10;

    public GiftManager(@NotNull RealisticVillagers plugin) {
        this.plugin = plugin;
        this.loadGiftCategories();
    }

    public void loadGiftCategories() {
        this.gifts.clear();
        this.defaultCooldownSeconds = this.plugin.getGiftsConfig().getInt("default-cooldown-seconds", 300);
        this.maxGain = this.plugin.getGiftsConfig().getInt("max-gain", 9999);
        this.maxLoss = this.plugin.getGiftsConfig().getInt("max-loss", 9999);
        ConfigurationSection items = this.plugin.getGiftsConfig().getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (String key : items.getKeys(false)) {
            Material material = PluginUtils.getOrNull(Material.class, key.toUpperCase(Locale.ROOT));
            if (material == null) {
                this.plugin.getLogger().warning("Unknown material in gifts.items: " + key);
                continue;
            }
            String categoryName = items.getString(key + ".category", "NEUTRAL").toUpperCase(Locale.ROOT);
            GiftCategory category = PluginUtils.getOrDefault(GiftCategory.class, categoryName, GiftCategory.NEUTRAL);
            int reputation = items.getInt(key + ".reputation", 0);
            boolean inventoryLootOnly = items.getBoolean(key + ".inventory-loot-only", false);
            this.gifts.put(material, new Gift(material, category, reputation, inventoryLootOnly));
        }
    }

    @Nullable
    public Gift getGift(@NotNull Material material) {
        return this.gifts.get(material);
    }

    @NotNull
    public Set<Gift> getAllGifts() {
        return Collections.unmodifiableSet(new HashSet<Gift>(this.gifts.values()));
    }

    @NotNull
    public Set<Gift> getGiftsFromCategory(String ignoredPath) {
        return this.getAllGifts();
    }

    public int applyDailyCap(@NotNull UUID villagerUUID, @NotNull UUID playerUUID, int rawDelta) {
        if (rawDelta == 0) {
            return 0;
        }
        DailyGiftData data = this.dailyCaps.computeIfAbsent(villagerUUID, k -> new HashMap()).computeIfAbsent(playerUUID, k -> new DailyGiftData());
        data.rolloverIfNeeded();
        if (rawDelta > 0) {
            int remaining = this.maxGain - data.gained;
            if (remaining <= 0) {
                return 0;
            }
            int applied = Math.min(rawDelta, remaining);
            data.gained += applied;
            return applied;
        }
        int remaining = this.maxLoss - data.lost;
        if (remaining <= 0) {
            return 0;
        }
        int applied = Math.min(-rawDelta, remaining);
        data.lost += applied;
        return -applied;
    }

    public int getDefaultCooldownSeconds() {
        return this.defaultCooldownSeconds;
    }

    public int getMaxGain() {
        return this.maxGain;
    }

    public int getMaxLoss() {
        return this.maxLoss;
    }

    private static final class DailyGiftData {
        int gained = 0;
        int lost = 0;
        LocalDate date = LocalDate.now();

        private DailyGiftData() {
        }

        void rolloverIfNeeded() {
            LocalDate today = LocalDate.now();
            if (!today.equals(this.date)) {
                this.gained = 0;
                this.lost = 0;
                this.date = today;
            }
        }
    }
}

