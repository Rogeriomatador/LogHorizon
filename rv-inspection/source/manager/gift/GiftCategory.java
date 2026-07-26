/*
 * Decompiled with CFR 0.152.
 */
package me.matsubara.realisticvillagers.manager.gift;

import java.util.Locale;

public enum GiftCategory {
    LOVED,
    NEUTRAL,
    DISLIKED;


    public String lowerName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isPositive() {
        return this != DISLIKED;
    }
}

