/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.EntityEffect
 *  org.bukkit.Keyed
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Tag
 *  org.bukkit.block.Block
 *  org.bukkit.block.data.type.Bed
 *  org.bukkit.block.data.type.Bed$Part
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Villager
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.event.entity.EntityPickupItemEvent
 *  org.bukkit.event.inventory.InventoryPickupItemEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.matsubara.realisticvillagers.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.matsubara.realisticvillagers.RealisticVillagers;
import me.matsubara.realisticvillagers.data.HandleHomeResult;
import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import me.matsubara.realisticvillagers.event.VillagerFishEvent;
import me.matsubara.realisticvillagers.event.VillagerPickGiftEvent;
import me.matsubara.realisticvillagers.files.Config;
import me.matsubara.realisticvillagers.files.Messages;
import me.matsubara.realisticvillagers.gui.InteractGUI;
import me.matsubara.realisticvillagers.manager.gift.Gift;
import me.matsubara.realisticvillagers.manager.gift.GiftCategory;
import me.matsubara.realisticvillagers.util.ItemStackUtils;
import me.matsubara.realisticvillagers.util.PluginUtils;
import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExpectingManager
implements Listener {
    private final RealisticVillagers plugin;
    private final Map<UUID, IVillagerNPC> villagerExpectingCache;

    public ExpectingManager(RealisticVillagers plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        this.villagerExpectingCache = new HashMap<UUID, IVillagerNPC>();
    }

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Villager)) {
            return;
        }
        Villager villager = (Villager)livingEntity;
        this.villagerExpectingCache.entrySet().removeIf(next -> ((IVillagerNPC)next.getValue()).bukkit().equals((Object)villager));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onVillagerFish(@NotNull VillagerFishEvent event) {
        if (event.getState() != VillagerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Entity caught = event.getCaught();
        if (!(caught instanceof Item)) {
            return;
        }
        ItemStack item = ((Item)caught).getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(this.plugin.getFishedKey(), PersistentDataType.STRING, (Object)event.getNPC().bukkit().getUniqueId().toString());
        }
        item.setItemMeta(meta);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerDropItem(@NotNull PlayerDropItemEvent event) {
        if (!this.getGiftModeFromConfig().drop()) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        IVillagerNPC npc = this.villagerExpectingCache.get(uuid);
        if (npc == null || !npc.isExpectingGift()) {
            return;
        }
        npc.setGiftDropped(true);
        ItemStack item = event.getItemDrop().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(this.plugin.getGiftKey(), PersistentDataType.STRING, (Object)uuid.toString());
        }
        item.setItemMeta(meta);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityDamage(@NotNull EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Item)) {
            return;
        }
        Item item = (Item)entity;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
            this.handleItemDissapear(item);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInventoryPickupItem(@NotNull InventoryPickupItemEvent event) {
        this.handleItemDissapear(event.getItem());
    }

    private void handleItemDissapear(Item item) {
        if (this.notOurItem(item)) {
            return;
        }
        IVillagerNPC npc = this.get(item.getThrower());
        npc.setGiftDropped(false);
        this.removeMetadata(item, this.plugin.getGiftKey());
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (this.notOurItem(item)) {
            return;
        }
        UUID thrower = item.getThrower();
        IVillagerNPC npc = this.get(thrower);
        LivingEntity picker = event.getEntity();
        UUID pickerUUID = picker.getUniqueId();
        Player throwerPlayer = Bukkit.getPlayer((UUID)thrower);
        if (npc.bukkit().getUniqueId().equals(pickerUUID)) {
            this.removeMetadata(item, this.plugin.getGiftKey());
            this.handleVillagerPickUp(npc, item.getItemStack(), thrower, throwerPlayer, (Cancellable)event);
            return;
        }
        if (!pickerUUID.equals(thrower)) {
            event.setCancelled(true);
            return;
        }
        npc.setGiftDropped(false);
        this.removeMetadata(item, this.plugin.getGiftKey());
        if (throwerPlayer == null) {
            return;
        }
        Inventory open = throwerPlayer.getOpenInventory().getTopInventory();
        InventoryHolder inventoryHolder = open.getHolder();
        if (inventoryHolder instanceof InteractGUI) {
            InteractGUI interact = (InteractGUI)inventoryHolder;
            interact.setShouldStopInteracting(true);
            throwerPlayer.closeInventory();
        }
    }

    public void handleVillagerPickUp(@NotNull IVillagerNPC npc, ItemStack item, UUID thrower, Player throwerPlayer, @Nullable Cancellable cancellable) {
        this.remove(thrower);
        npc.stopExpecting();
        if (throwerPlayer == null) {
            if (cancellable != null) {
                cancellable.setCancelled(true);
            }
            return;
        }
        this.plugin.getServer().getPluginManager().callEvent((Event)new VillagerPickGiftEvent(npc, throwerPlayer, item));
        this.handleGift(npc, throwerPlayer, item);
        this.plugin.getCooldownManager().addCooldown(throwerPlayer, npc.bukkit(), "gift");
    }

    private boolean notOurItem(Item item) {
        this.removeMetadata(item, this.plugin.getFishedKey());
        UUID thrower = item.getThrower();
        if (thrower == null) {
            return true;
        }
        IVillagerNPC npc = this.get(thrower);
        if (npc == null || !npc.isExpectingGift()) {
            this.removeMetadata(item, this.plugin.getGiftKey());
            return true;
        }
        return false;
    }

    private void removeMetadata(@NotNull Item item, NamespacedKey key) {
        ItemStack stack = item.getItemStack();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(key);
        }
        stack.setItemMeta(meta);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        HandleHomeResult result;
        Player player = event.getPlayer();
        IVillagerNPC npc = this.get(player.getUniqueId());
        if (npc == null || !npc.isExpectingBed()) {
            return;
        }
        if (event.getAction() != Action.PHYSICAL) {
            event.setCancelled(true);
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        Messages messages = this.plugin.getMessages();
        if (block == null || !Tag.BEDS.isTagged((Keyed)block.getType())) {
            messages.send((CommandSender)player, Messages.Message.BED_INVALID);
            return;
        }
        Bed bed = (Bed)block.getBlockData();
        boolean occupied = bed.isOccupied();
        if (occupied || (result = npc.handleBedHome(bed.getPart() == Bed.Part.HEAD ? block : block.getRelative(bed.getFacing()))) == HandleHomeResult.OCCUPIED) {
            messages.send((CommandSender)player, Messages.Message.BED_OCCUPIED);
            return;
        }
        if (result == HandleHomeResult.INVALID) {
            messages.send((CommandSender)player, Messages.Message.BED_INVALID);
            return;
        }
        if (result == HandleHomeResult.SUCCESS) {
            messages.send((CommandSender)player, Messages.Message.BED_ESTABLISHED);
            messages.send(player, npc, Messages.Message.SET_HOME_SUCCESS);
        }
        this.villagerExpectingCache.remove(player.getUniqueId());
        npc.stopExpecting();
        this.plugin.getCooldownManager().addCooldown(player, npc.bukkit(), "bed");
    }

    public void handleGift(@NotNull IVillagerNPC npc, @NotNull Player player, @NotNull ItemStack gift) {
        int amount;
        boolean success;
        Villager villager;
        LivingEntity bukkit = npc.bukkit();
        int reputation = npc.getReputation(player);
        int repRequiredToMarry = Config.REPUTATION_REQUIRED_TO_MARRY.asInt();
        boolean isRing = PluginUtils.isItem(gift, this.plugin.getIsRingKey());
        boolean isCross = PluginUtils.isItem(gift, this.plugin.getIsCrossKey());
        boolean alreadyMarriedWithPlayer = isRing && npc.isPartner(player);
        boolean alreadyHasCross = isCross && PluginUtils.hasAnyOf((InventoryHolder)bukkit, this.plugin.getIsCrossKey());
        boolean isAdult = !(bukkit instanceof Villager) || (villager = (Villager)bukkit).isAdult();
        boolean successByRing = isRing && isAdult && reputation >= repRequiredToMarry && !npc.isFamily(player, false) && !npc.hasPartner() && !this.plugin.isMarried(player) && !alreadyMarriedWithPlayer;
        boolean successByCross = isCross && !alreadyHasCross;
        Gift giftEntry = this.plugin.getGiftManager().getGift(gift.getType());
        boolean recognised = giftEntry != null;
        boolean bl = success = successByRing || successByCross || !isRing && !isCross && recognised || alreadyMarriedWithPlayer || alreadyHasCross;
        int rawDelta = success ? (successByRing ? Config.WEDDING_RING_REPUTATION.asInt() : (successByCross ? Config.CROSS_REPUTATION.asInt() : (alreadyMarriedWithPlayer || alreadyHasCross ? 0 : giftEntry.getReputation()))) : (isRing ? 0 : -Math.abs(Config.BAD_GIFT_REPUTATION.asInt()));
        int n = amount = rawDelta == 0 || successByRing || successByCross || alreadyMarriedWithPlayer || alreadyHasCross ? rawDelta : this.plugin.getGiftManager().applyDailyCap(bukkit.getUniqueId(), player.getUniqueId(), rawDelta);
        if (amount > 0) {
            npc.addMinorPositive(player, amount);
        } else if (amount < 0) {
            npc.addMinorNegative(player, -amount);
        }
        Messages messages = this.plugin.getMessages();
        if (successByRing) {
            bukkit.playEffect(EntityEffect.VILLAGER_HEART);
            messages.send(player, npc, Messages.Message.MARRRY_SUCCESS);
            npc.setPartner(player);
            player.getPersistentDataContainer().set(this.plugin.getMarriedWith(), PersistentDataType.STRING, (Object)bukkit.getUniqueId().toString());
            return;
        }
        if (isRing && !success && !npc.isFamily(player, false) && isAdult) {
            bukkit.playEffect(EntityEffect.VILLAGER_ANGRY);
            Messages.Message message = npc.hasPartner() ? Messages.Message.MARRY_FAIL_MARRIED_TO_OTHER : (this.plugin.isMarried(player) ? Messages.Message.MARRY_FAIL_PLAYER_MARRIED : Messages.Message.MARRY_FAIL_LOW_REPUTATION);
            messages.send(player, npc, message);
            this.dropRing(npc, gift);
            return;
        }
        if (successByCross || success && isCross) {
            bukkit.playEffect(EntityEffect.VILLAGER_HAPPY);
            messages.sendRandomGiftMessage(player, npc, GiftCategory.LOVED);
            return;
        }
        if (success && alreadyMarriedWithPlayer) {
            messages.send(player, npc, Messages.Message.MARRY_FAIL_MARRIED_TO_GIVER);
            return;
        }
        if (!success && isRing && !isAdult) {
            this.dropRing(npc, gift);
        }
        if (recognised) {
            GiftCategory category = giftEntry.getCategory();
            switch (category) {
                case LOVED: {
                    bukkit.playEffect(EntityEffect.VILLAGER_HEART);
                    break;
                }
                case DISLIKED: {
                    bukkit.playEffect(EntityEffect.VILLAGER_ANGRY);
                    break;
                }
                default: {
                    bukkit.playEffect(EntityEffect.VILLAGER_HAPPY);
                }
            }
            if (category.isPositive() && Config.ANNOYING_METER_CLEAR_AFTER_SUCCESS_INTERACTION.asBool()) {
                this.plugin.getAnnoyingManager().stopBeingAnnoyed(player, npc);
            }
            messages.sendRandomGiftMessage(player, npc, category);
        } else {
            bukkit.playEffect(EntityEffect.VILLAGER_ANGRY);
            messages.sendRandomGiftMessage(player, npc, null);
        }
        ItemStackUtils.setBetterWeaponInMaindHand(bukkit, gift);
        ItemStackUtils.setArmorItem(bukkit, gift);
    }

    private void dropRing(@NotNull IVillagerNPC npc, ItemStack gift) {
        npc.drop(gift);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            LivingEntity patt0$temp = npc.bukkit();
            if (patt0$temp instanceof InventoryHolder) {
                InventoryHolder holder = (InventoryHolder)patt0$temp;
                holder.getInventory().removeItem(new ItemStack[]{this.plugin.getRing().getResult()});
            }
        }, 2L);
    }

    public IVillagerNPC get(UUID uuid) {
        return this.villagerExpectingCache.get(uuid);
    }

    public void expect(UUID uuid, IVillagerNPC npc) {
        this.villagerExpectingCache.put(uuid, npc);
    }

    public void remove(UUID uuid) {
        this.villagerExpectingCache.remove(uuid);
    }

    public GiftMode getGiftModeFromConfig() {
        return PluginUtils.getOrDefault(GiftMode.class, Config.GIFT_MODE.asString("DROP"), GiftMode.DROP);
    }

    public static enum GiftMode {
        DROP,
        RIGHT_CLICK;


        public boolean drop() {
            return this == DROP;
        }

        public boolean rightClick() {
            return this == RIGHT_CLICK;
        }
    }
}

