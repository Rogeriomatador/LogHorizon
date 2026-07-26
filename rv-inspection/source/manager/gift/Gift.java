/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 */
package me.matsubara.realisticvillagers.manager.gift;

import me.matsubara.realisticvillagers.manager.gift.GiftCategory;
import org.bukkit.Material;

public class Gift {
    private final Material type;
    private final GiftCategory category;
    private final int reputation;
    private final boolean inventoryLootOnly;

    public Gift(Material type, GiftCategory category, int reputation, boolean inventoryLootOnly) {
        this.type = type;
        this.category = category;
        this.reputation = reputation;
        this.inventoryLootOnly = inventoryLootOnly;
    }

    public boolean is(Material type) {
        return this.type == type;
    }

    public Material getType() {
        return this.type;
    }

    public GiftCategory getCategory() {
        return this.category;
    }

    public int getReputation() {
        return this.reputation;
    }

    public boolean isInventoryLootOnly() {
        return this.inventoryLootOnly;
    }
}

