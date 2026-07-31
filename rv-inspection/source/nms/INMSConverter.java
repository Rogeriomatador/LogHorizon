/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  org.bukkit.GameRule
 *  org.bukkit.Location
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Raid
 *  org.bukkit.World
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.matsubara.realisticvillagers.nms;

import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Optional;
import java.util.UUID;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.data.serialization.OfflineDataWrapper;
import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Raid;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface INMSConverter {
    public Optional<IVillagerNPC> getNPC(LivingEntity var1);

    public void registerEntities();

    public String getNPCTag(LivingEntity var1, boolean var2);

    public boolean isSeekGoatHorn(ItemStack var1);

    public void createBaby(Location var1, String var2, String var3, UUID var4, Player var5);

    public void loadDataFromTag(LivingEntity var1, String var2);

    public UUID getPartnerUUIDFromPlayerNBT(File var1);

    public void removePartnerFromPlayerNBT(File var1);

    public void loadData();

    public Raid getRaidAt(Location var1);

    public GameProfile getPlayerProfile(Player var1);

    public void refreshSchedules();

    public IVillagerNPC getNPCFromTag(String var1);

    public void spawnFromTag(Location var1, String var2);

    public void addGameRuleListener(World var1);

    @Nullable
    public OfflineDataWrapper getNPCFromPDC(PersistentDataContainer var1, NamespacedKey var2);

    public static void printRuleWarning(@NotNull RealisticVillagers plugin, @NotNull World world, @NotNull GameRule<?> rule) {
        String warning = "The rule {" + rule.getName() + "} has been disabled in the world {" + world.getName() + "}, this will not allow villagers to pick up items.";
        plugin.getLogger().warning(warning);
    }
}

