/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Block
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.matsubara.realisticvillagers.entity;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.data.ExpectingType;
import me.matsubara.realisticvillagers.data.HandleHomeResult;
import me.matsubara.realisticvillagers.data.InteractType;
import me.matsubara.realisticvillagers.data.LastKnownPosition;
import me.matsubara.realisticvillagers.event.VillagerExhaustionEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IVillagerNPC {
    public UUID getUniqueId();

    public String getVillagerName();

    public void setVillagerName(String var1);

    public int getReputation(UUID var1);

    default public int getReputation(@NotNull Player player) {
        return this.getReputation(player.getUniqueId());
    }

    public IVillagerNPC getPartner();

    public List<IVillagerNPC> getPartners();

    public boolean isPartnerVillager();

    public IVillagerNPC getFather();

    public boolean isFatherVillager();

    public IVillagerNPC getMother();

    public boolean isMotherVillager();

    public List<IVillagerNPC> getChildrens();

    public LivingEntity bukkit();

    public void addMinorPositive(UUID var1, int var2);

    default public void addMinorPositive(@NotNull Player player, int amount) {
        this.addMinorPositive(player.getUniqueId(), amount);
    }

    public void addMinorNegative(UUID var1, int var2);

    default public void addMinorNegative(@NotNull Player player, int amount) {
        this.addMinorNegative(player.getUniqueId(), amount);
    }

    public void jumpIfPossible();

    public void setProcreatingWith(UUID var1);

    public void setLastProcreation(long var1);

    public boolean canAttack();

    public String getSex();

    public void setSex(String var1);

    public boolean isGenderLocked();

    public void setGenderLocked(boolean var1);

    public void setParent(@Nullable IVillagerNPC var1);

    public void setFather(@Nullable UUID var1, boolean var2);

    public int getSkinTextureId();

    public void setSkinTextureId(int var1);

    public int getKidSkinTextureId();

    public void setKidSkinTextureId(int var1);

    public boolean isExpectingGift();

    public boolean isGiftDropped();

    public void setGiftDropped(boolean var1);

    public void stopExpecting();

    public boolean isExpectingBed();

    public HandleHomeResult handleBedHome(Block var1);

    public boolean isTarget(EntityType var1);

    public boolean isConversating();

    public boolean isFemale();

    public boolean isMale();

    public boolean is(Villager.Profession ... var1);

    default public boolean isFamily(@NotNull Player player) {
        return this.isFamily(player.getUniqueId());
    }

    default public boolean isFamily(UUID uuid) {
        return this.isFamily(uuid, false);
    }

    public boolean isFamily(UUID var1, boolean var2);

    default public boolean isFamily(@NotNull Player player, boolean checkPartner) {
        return this.isFamily(player.getUniqueId(), checkPartner);
    }

    public boolean isPartner(UUID var1);

    default public boolean isPartner(@NotNull Player player) {
        return this.isPartner(player.getUniqueId());
    }

    public boolean isFather(UUID var1);

    default public boolean isFather(@NotNull Player player) {
        return this.isFather(player.getUniqueId());
    }

    public String getActivityName(String var1);

    public void addTarget(EntityType var1);

    public void removeTarget(EntityType var1);

    public void setInteractType(InteractType var1);

    public void stayInPlace();

    public void stopStayingInPlace();

    public void startExpectingFrom(ExpectingType var1, UUID var2, int var3);

    public long getLastProcreation();

    public void divorceAndDropRing(@Nullable Player var1);

    public void drop(ItemStack var1);

    public void startTrading(Player var1);

    public void stopInteracting();

    public void reactToSeekHorn(Player var1);

    public boolean isDamageSourceBlocked();

    public boolean isInsideRaid();

    public boolean isFighting();

    public boolean isProcreating();

    public boolean isExpectingGiftFrom(UUID var1);

    public boolean isExpectingBedFrom(UUID var1);

    public boolean isExpecting();

    public ExpectingType getExpectingType();

    public UUID getExpectingFrom();

    public boolean isInteracting();

    public UUID getInteractingWith();

    public boolean isFollowing();

    public boolean isStayingInPlace();

    public void setInteractingWithAndType(UUID var1, InteractType var2);

    public boolean hasPartner();

    public void setPartner(@Nullable UUID var1, boolean var2);

    default public void setPartner(@NotNull Player player) {
        this.setPartner(player.getUniqueId(), false);
    }

    public int getFoodLevel();

    public boolean isFishing();

    public void toggleFishing();

    public void sendSpawnPacket();

    public void sendDestroyPacket();

    public boolean isShakingHead();

    public void shakeHead(Player var1);

    public IVillagerNPC getOffline();

    public LastKnownPosition getLastKnownPosition();

    public boolean isEquipped();

    public void setEquipped(boolean var1);

    public boolean validShoulderEntityLeft();

    public Object getShoulderEntityLeft();

    public boolean validShoulderEntityRight();

    public Object getShoulderEntityRight();

    public void causeFoodExhaustion(float var1, VillagerExhaustionEvent.ExhaustionReason var2);

    public boolean isWasInfected();

    public void stopExchangeables();

    public void refreshBrain();

    public boolean isReviving();

    public Set<UUID> getPlayers();

    public byte getHandData();

    public int getEffectColor();

    public boolean getEffectAmbience();

    public int getBeeStingers();

    public void attack(LivingEntity var1);

    public boolean isWanderingTrader();

    default public void resetNametagsFor(@NotNull RealisticVillagers plugin, Player player) {
        LivingEntity bukkit = this.bukkit();
        if (bukkit == null || player == null) {
            return;
        }
        plugin.getTracker().getPool().getNPC(bukkit.getEntityId()).ifPresent(temp -> temp.refreshNametags(player));
    }
}

