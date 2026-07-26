/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.github.retrooper.packetevents.PacketEvents
 *  com.github.retrooper.packetevents.PacketEventsAPI
 *  com.github.retrooper.packetevents.protocol.player.TextureProperty
 *  com.google.common.base.Strings
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Sets
 *  io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.lang3.RandomUtils
 *  org.apache.commons.lang3.tuple.Pair
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.Color
 *  org.bukkit.FireworkEffect
 *  org.bukkit.FireworkEffect$Builder
 *  org.bukkit.FireworkEffect$Type
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.World
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.MemoryConfiguration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.configuration.serialization.ConfigurationSerialization
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.event.Listener
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.InventoryHolder
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.metadata.Metadatable
 *  org.bukkit.persistence.PersistentDataAdapterContext
 *  org.bukkit.persistence.PersistentDataContainer
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.PluginManager
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionType
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.matsubara.realisticvillagers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.jeff_media.morepersistentdatatypes.datatypes.serializable.ConfigurationSerializableDataType;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import me.matsubara.realisticvillagers.command.GenderCommand;
import me.matsubara.realisticvillagers.command.MainCommand;
import me.matsubara.realisticvillagers.compatibility.Compatibility;
import me.matsubara.realisticvillagers.compatibility.CompatibilityManager;
import me.matsubara.realisticvillagers.compatibility.EMCompatibility;
import me.matsubara.realisticvillagers.compatibility.VTLCompatibility;
import me.matsubara.realisticvillagers.compatibility.ViaCompatibility;
import me.matsubara.realisticvillagers.data.ItemLoot;
import me.matsubara.realisticvillagers.data.serialization.GossipEntryWrapper;
import me.matsubara.realisticvillagers.data.serialization.OfflineDataWrapper;
import me.matsubara.realisticvillagers.entity.IVillagerNPC;
import me.matsubara.realisticvillagers.files.Config;
import me.matsubara.realisticvillagers.files.Messages;
import me.matsubara.realisticvillagers.gui.types.WhistleGUI;
import me.matsubara.realisticvillagers.hologram.HologramListener;
import me.matsubara.realisticvillagers.hologram.HologramManager;
import me.matsubara.realisticvillagers.listener.BlockListeners;
import me.matsubara.realisticvillagers.listener.InventoryListeners;
import me.matsubara.realisticvillagers.listener.OtherListeners;
import me.matsubara.realisticvillagers.listener.PlayerListeners;
import me.matsubara.realisticvillagers.listener.VillagerListeners;
import me.matsubara.realisticvillagers.manager.AnnoyingMeterManager;
import me.matsubara.realisticvillagers.manager.ChestManager;
import me.matsubara.realisticvillagers.manager.ExpectingManager;
import me.matsubara.realisticvillagers.manager.InteractCooldownManager;
import me.matsubara.realisticvillagers.manager.gift.Gift;
import me.matsubara.realisticvillagers.manager.gift.GiftCategory;
import me.matsubara.realisticvillagers.manager.gift.GiftManager;
import me.matsubara.realisticvillagers.manager.revive.ReviveManager;
import me.matsubara.realisticvillagers.nms.INMSConverter;
import me.matsubara.realisticvillagers.tracker.VillagerTracker;
import me.matsubara.realisticvillagers.util.ItemBuilder;
import me.matsubara.realisticvillagers.util.ItemStackUtils;
import me.matsubara.realisticvillagers.util.PluginUtils;
import me.matsubara.realisticvillagers.util.Shape;
import me.matsubara.realisticvillagers.util.VersionMatcher;
import me.matsubara.realisticvillagers.util.anvilgui.AnvilGUI;
import me.matsubara.realisticvillagers.util.bstats.bukkit.Metrics;
import me.matsubara.realisticvillagers.util.configupdater.ConfigUpdater;
import me.matsubara.realisticvillagers.util.customblockdata.CustomBlockData;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RealisticVillagers
extends JavaPlugin {
    private final NamespacedKey giftKey = this.key("GiftUUID");
    private final NamespacedKey marriedWith = this.key("MarriedWith");
    private final NamespacedKey procreationKey = this.key("Procreation");
    private final NamespacedKey motherUUIDKey = this.key("MotherUUID");
    private final NamespacedKey isRingKey = this.key("IsRing");
    private final NamespacedKey isWhistleKey = this.key("IsWhistle");
    private final NamespacedKey isCrossKey = this.key("IsCross");
    private final NamespacedKey entityTypeKey = this.key("EntityType");
    private final NamespacedKey chatInteractionTypeKey = this.key("ChatInteractionType");
    private final NamespacedKey childNameKey = this.key("ChildName");
    private final NamespacedKey childSexKey = this.key("ChildSex");
    private final NamespacedKey zombieTransformKey = this.key("ZombieTransform");
    private final NamespacedKey fishedKey = this.key("Fished");
    private final NamespacedKey bedVillagerKey = this.key("BedVillager");
    private final NamespacedKey playerSexKey = this.key("PlayerSex");
    @ApiStatus.Internal
    private final NamespacedKey valuesKey = this.key("RValues");
    private final NamespacedKey inventoryKey = this.key("RInventory");
    @ApiStatus.Internal
    private final NamespacedKey npcValuesKey = this.key("VillagerNPCValues");
    @ApiStatus.Internal
    private final NamespacedKey tamedByPlayerKey = this.key("TamedByPlayer");
    private final NamespacedKey tamedByVillagerKey = this.key("TamedByVillager");
    private final NamespacedKey isBeingLootedKey = this.key("IsBeingLooted");
    @ApiStatus.Internal
    private final NamespacedKey ignoreVillagerKey = this.key("IgnoreVillager");
    private final NamespacedKey villagerUUIDKey = this.key("VillagerUUID");
    private final NamespacedKey divorcePapersKey = this.key("DivorcePapers");
    private final NamespacedKey raidStatsKey = this.key("RaidStats");
    private final NamespacedKey skinDataKey = this.key("SkinDataID");
    private final NamespacedKey ignoreItemKey = this.key("IgnoreItem");
    private final NamespacedKey playerUUIDKey = new NamespacedKey((Plugin)this, "PlayerUUID");
    private final NamespacedKey itemIdKey = new NamespacedKey((Plugin)this, "ItemID");
    private InventoryListeners inventoryListeners;
    private OtherListeners otherListeners;
    private PlayerListeners playerListeners;
    private VillagerListeners villagerListeners;
    private HologramManager hologramManager;
    private VillagerTracker tracker;
    private Shape ring;
    private Shape whistle;
    private Shape cross;
    private AnnoyingMeterManager annoyingManager;
    private ReviveManager reviveManager;
    private GiftManager giftManager;
    private ChestManager chestManager;
    private ExpectingManager expectingManager;
    private InteractCooldownManager cooldownManager;
    private CompatibilityManager compatibilityManager;
    private FileConfiguration guiConfig;
    private FileConfiguration lootConfig;
    private FileConfiguration variableTextConfig;
    private FileConfiguration hologramConfig;
    private FileConfiguration giftsConfig;
    private Messages messages;
    private INMSConverter converter;
    private final List<String> defaultTargets = new ArrayList<String>();
    private final Set<Gift> wantedItems = new HashSet<Gift>();
    private final Map<String, List<ItemLoot>> loots = new HashMap<String, List<ItemLoot>>();
    private final Consumer<File> loadConsumer = file -> this.tracker.getFiles().put(file.getName(), (Pair<File, FileConfiguration>)Pair.of((Object)file, (Object)YamlConfiguration.loadConfiguration((File)file)));
    private List<String> worlds;
    private static final String VILLAGER_HEAD_TEXTURE = "4ca8ef2458a2b10260b8756558f7679bcb7ef691d41f534efea2ba75107315cc";
    private static final String UNKNOWN_HEAD_TEXTURE = "badc048a7ce78f7dad72a07da27d85c0916881e5522eeed1e3daf217a38c1a";
    public static final BiConsumer<JavaPlugin, Metadatable> LISTEN_MODE_IGNORE = (plugin, living) -> living.setMetadata("RemoveGlow", (MetadataValue)new FixedMetadataValue((Plugin)plugin, (Object)true));
    public static final List<AnvilGUI.ResponseAction> CLOSE_RESPONSE = Collections.singletonList(AnvilGUI.ResponseAction.close());
    private static final List<String> FILTER_TYPES = List.of("WHITELIST", "BLACKLIST");
    private static final Set<String> SPECIAL_SECTIONS = Sets.newHashSet((Object[])new String[]{"baby", "wedding-ring", "whistle", "divorce-papers", "cross", "change-skin", "default-wanted-items", "revive.head-item"});
    private static final Set<String> GUI_SPECIAL_SECTIONS = Sets.newHashSet((Object[])new String[]{"gui.main.frame"});
    private static final List<String> GUI_TYPES = List.of("main", "equipment", "combat", "whistle", "skin", "new-skin");
    private static final int BSTATS_ID = 27463;
    private static final NamespacedKey MM_KEY = new NamespacedKey("mythicmobs", "type");
    public static final PersistentDataType<byte[], OfflineDataWrapper> VILLAGER_DATA;

    public NamespacedKey getNpcValuesKey() {
        VersionMatcher matcher = VersionMatcher.getByMinecraftVersion();
        return matcher != null && matcher.higherOrEqualThan(VersionMatcher.v1_21_8) ? this.valuesKey : this.getLegacyNpcValuesKey();
    }

    @ApiStatus.Internal
    public NamespacedKey getLegacyNpcValuesKey() {
        return this.npcValuesKey;
    }

    public void onLoad() {
        PacketEvents.setAPI((PacketEventsAPI)SpigotPacketEventsBuilder.build((Plugin)this));
        PacketEvents.getAPI().load();
        long now = System.nanoTime();
        Logger logger = this.getLogger();
        logger.info("****************************************");
        logger.info("Loading compatibilities...");
        this.compatibilityManager = new CompatibilityManager();
        this.compatibilityManager.addCompatibility(this.getName(), (Villager villager) -> villager.hasAI() && !villager.hasMetadata("shopkeeper") && !villager.hasMetadata("NPC"));
        this.addCompatibility("EliteMobs", EMCompatibility::new);
        this.addCompatibility("ViaVersion", ViaCompatibility::new);
        this.addCompatibility("VillagerTradeLimiter", VTLCompatibility::new);
        this.addCompatibility("MythicMobs", () -> villager -> !villager.getPersistentDataContainer().has(MM_KEY, PersistentDataType.STRING));
        logger.info("Compatibilities loaded!");
        logger.info("");
        logger.info("Registering custom entities...");
        String currentMC = Bukkit.getBukkitVersion().split("-")[0];
        VersionMatcher matcher = VersionMatcher.getByMinecraftVersion();
        if (matcher == null) {
            logger.severe("NMSConverter couldn't find a valid implementation for this server version (" + currentMC + ").");
        } else if (!VersionMatcher.isExactMatch()) {
            logger.warning("Server version " + currentMC + " is not officially supported. Using latest known NMS as fallback \u2014 some features may not work correctly.");
        }
        if (matcher != null) {
            try {
                Class<?> converterClass = Class.forName(INMSConverter.class.getPackageName() + "." + matcher.getPackageName() + ".NMSConverter");
                Constructor<?> converterConstructor = converterClass.getConstructor(((Object)((Object)this)).getClass());
                this.converter = (INMSConverter)converterConstructor.newInstance(new Object[]{this});
                this.converter.registerEntities();
                try (InputStream stream = this.getResource("configs/variable-text.yml");){
                    if (stream != null) {
                        this.variableTextConfig = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(stream, StandardCharsets.UTF_8));
                        this.variableTextConfig.setDefaults((Configuration)new MemoryConfiguration());
                    }
                }
                catch (IOException iOException) {
                    // empty catch block
                }
                this.converter.refreshSchedules();
            }
            catch (ReflectiveOperationException exception) {
                logger.severe("NMSConverter failed to load for server version " + currentMC + " (fallback NMS: " + matcher.getPackageName() + ").");
                exception.printStackTrace();
            }
        }
        logger.info("Custom entities registered!");
        logger.info("");
        this.logLoadingTime(true, now);
        logger.info("****************************************");
    }

    private void addCompatibility(String name, Supplier<Compatibility> supplier) {
        PluginManager manager = this.getServer().getPluginManager();
        if (manager.getPlugin(name) == null) {
            return;
        }
        this.compatibilityManager.addCompatibility(name, supplier.get());
    }

    public Messages getMessages() {
        return this.messages;
    }

    public INMSConverter getConverter() {
        return this.converter;
    }

    public VillagerTracker getTracker() {
        return this.tracker;
    }

    public AnnoyingMeterManager getAnnoyingManager() {
        return this.annoyingManager;
    }

    public ReviveManager getReviveManager() {
        return this.reviveManager;
    }

    public GiftManager getGiftManager() {
        return this.giftManager;
    }

    public ChestManager getChestManager() {
        return this.chestManager;
    }

    public ExpectingManager getExpectingManager() {
        return this.expectingManager;
    }

    public InteractCooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public FileConfiguration getGuiConfig() {
        return this.guiConfig;
    }

    public FileConfiguration getLootConfig() {
        return this.lootConfig;
    }

    public FileConfiguration getVariableTextConfig() {
        return this.variableTextConfig;
    }

    public FileConfiguration getHologramConfig() {
        return this.hologramConfig;
    }

    public FileConfiguration getGiftsConfig() {
        return this.giftsConfig;
    }

    public CompatibilityManager getCompatibilityManager() {
        return this.compatibilityManager;
    }

    public List<String> getDefaultTargets() {
        return this.defaultTargets;
    }

    public Set<Gift> getWantedItems() {
        return this.wantedItems;
    }

    public Map<String, List<ItemLoot>> getLoots() {
        return this.loots;
    }

    public List<String> getWorlds() {
        return this.worlds;
    }

    public NamespacedKey getGiftKey() {
        return this.giftKey;
    }

    public NamespacedKey getMarriedWith() {
        return this.marriedWith;
    }

    public NamespacedKey getProcreationKey() {
        return this.procreationKey;
    }

    public NamespacedKey getMotherUUIDKey() {
        return this.motherUUIDKey;
    }

    public NamespacedKey getIsRingKey() {
        return this.isRingKey;
    }

    public NamespacedKey getIsWhistleKey() {
        return this.isWhistleKey;
    }

    public NamespacedKey getIsCrossKey() {
        return this.isCrossKey;
    }

    public NamespacedKey getEntityTypeKey() {
        return this.entityTypeKey;
    }

    public NamespacedKey getChatInteractionTypeKey() {
        return this.chatInteractionTypeKey;
    }

    public NamespacedKey getChildNameKey() {
        return this.childNameKey;
    }

    public NamespacedKey getChildSexKey() {
        return this.childSexKey;
    }

    public NamespacedKey getZombieTransformKey() {
        return this.zombieTransformKey;
    }

    public NamespacedKey getFishedKey() {
        return this.fishedKey;
    }

    public NamespacedKey getBedVillagerKey() {
        return this.bedVillagerKey;
    }

    public NamespacedKey getPlayerSexKey() {
        return this.playerSexKey;
    }

    public NamespacedKey getInventoryKey() {
        return this.inventoryKey;
    }

    public NamespacedKey getTamedByPlayerKey() {
        return this.tamedByPlayerKey;
    }

    public NamespacedKey getTamedByVillagerKey() {
        return this.tamedByVillagerKey;
    }

    public NamespacedKey getIsBeingLootedKey() {
        return this.isBeingLootedKey;
    }

    public NamespacedKey getIgnoreVillagerKey() {
        return this.ignoreVillagerKey;
    }

    public NamespacedKey getVillagerUUIDKey() {
        return this.villagerUUIDKey;
    }

    public NamespacedKey getDivorcePapersKey() {
        return this.divorcePapersKey;
    }

    public NamespacedKey getRaidStatsKey() {
        return this.raidStatsKey;
    }

    public NamespacedKey getSkinDataKey() {
        return this.skinDataKey;
    }

    public NamespacedKey getIgnoreItemKey() {
        return this.ignoreItemKey;
    }

    public NamespacedKey getPlayerUUIDKey() {
        return this.playerUUIDKey;
    }

    public NamespacedKey getItemIdKey() {
        return this.itemIdKey;
    }

    public Shape getRing() {
        return this.ring;
    }

    public Shape getWhistle() {
        return this.whistle;
    }

    public Shape getCross() {
        return this.cross;
    }

    public void setRing(Shape ring) {
        this.ring = ring;
    }

    public void setWhistle(Shape whistle) {
        this.whistle = whistle;
    }

    public void setCross(Shape cross) {
        this.cross = cross;
    }

    public void onEnable() {
        long now = System.nanoTime();
        Logger logger = this.getLogger();
        logger.info("****************************************");
        PluginManager manager = this.getServer().getPluginManager();
        if (manager.getPlugin("packetevents") == null) {
            this.getLogger().severe("This plugin requires PacketEvents, disabling...");
            manager.disablePlugin((Plugin)this);
            return;
        }
        if (this.converter == null) {
            logger.severe("NMSConverter failed to initialize \u2014 this server version is not supported. Disabling RealisticVillagers.");
            manager.disablePlugin((Plugin)this);
            return;
        }
        new Metrics((Plugin)this, 27463);
        logger.info("Loading skin files...");
        this.saveResource("skins/female.yml");
        this.saveResource("skins/male.yml");
        logger.info("Skins loaded!");
        logger.info("");
        this.saveResource("configs/names/default.yml");
        this.saveDefaultConfig();
        this.saveResource("configs/gui.yml");
        this.saveResource("configs/loot.yml");
        this.saveResource("configs/variable-text.yml");
        this.saveResource("configs/holograms.yml");
        this.saveResource("configs/gifts.yml");
        this.messages = new Messages(this);
        logger.info("Updating configuration files...");
        this.updateConfigs();
        logger.info("Configuration files updated!");
        logger.info("");
        logger.info("Creating managers...");
        this.annoyingManager = new AnnoyingMeterManager(this);
        this.reviveManager = new ReviveManager(this);
        this.giftManager = new GiftManager(this);
        this.chestManager = new ChestManager(this);
        this.expectingManager = new ExpectingManager(this);
        this.cooldownManager = new InteractCooldownManager(this);
        CustomBlockData.registerListener((Plugin)this);
        logger.info("Managers created!");
        logger.info("");
        logger.info("Creating recipes...");
        this.ring = this.createWeddingRing();
        this.whistle = this.createWhistle();
        this.cross = this.createCross();
        logger.info("Recipes created!");
        logger.info("");
        logger.info("Loading entity data from all worlds...");
        this.converter.loadData();
        logger.info("Data loaded!");
        logger.info("");
        logger.info("Loading loots from the configuration files...");
        this.reloadDefaultTargetEntities();
        this.reloadWantedItems();
        this.reloadLoots();
        logger.info("Loots loaded!");
        logger.info("");
        this.hologramManager = new HologramManager(this);
        Listener[] listenerArray = new Listener[6];
        listenerArray[0] = new BlockListeners(this);
        this.inventoryListeners = new InventoryListeners(this);
        listenerArray[1] = this.inventoryListeners;
        this.otherListeners = new OtherListeners(this);
        listenerArray[2] = this.otherListeners;
        this.playerListeners = new PlayerListeners(this);
        listenerArray[3] = this.playerListeners;
        this.villagerListeners = new VillagerListeners(this);
        listenerArray[4] = this.villagerListeners;
        listenerArray[5] = new HologramListener(this);
        this.registerEvents(listenerArray);
        FileUtils.deleteQuietly((File)new File(this.getDataFolder(), "villagers.yml"));
        PluginCommand command = this.getCommand("realisticvillagers");
        if (command == null) {
            return;
        }
        MainCommand main = new MainCommand(this);
        command.setExecutor((CommandExecutor)main);
        command.setTabCompleter((TabCompleter)main);
        PluginCommand genderCmd = this.getCommand("gender");
        if (genderCmd != null) {
            GenderCommand gender = new GenderCommand(this);
            genderCmd.setExecutor((CommandExecutor)gender);
            genderCmd.setTabCompleter((TabCompleter)gender);
        }
        this.logLoadingTime(false, now);
        logger.info("****************************************");
    }

    public void onDisable() {
        PacketEvents.getAPI().terminate();
        if (this.hologramManager != null) {
            this.hologramManager.closeAll();
        }
        if (this.converter == null || this.tracker == null) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (this.tracker.isInvalid((LivingEntity)villager, true)) continue;
                this.converter.getNPC((LivingEntity)villager).ifPresent(IVillagerNPC::stopExchangeables);
            }
        }
    }

    private void logLoadingTime(boolean loading, long now) {
        String time = String.format(Locale.ROOT, "%.3fs", (double)(System.nanoTime() - now) / 1.0E9);
        this.getLogger().info((loading ? "Loading" : "Enabling") + " took " + time + "!");
    }

    private void fillGuiIgnoredSections(FileConfiguration guiCfg) {
        for (String guiType : GUI_TYPES) {
            ConfigurationSection section = guiCfg.getConfigurationSection("gui." + guiType + ".items");
            if (section == null) continue;
            for (String key : section.getKeys(false)) {
                GUI_SPECIAL_SECTIONS.add("gui." + guiType + ".items." + key);
            }
        }
    }

    private void registerEvents(Listener ... listeners) {
        for (Listener listener : listeners) {
            this.getServer().getPluginManager().registerEvents(listener, (Plugin)this);
        }
    }

    public void updateConfigs() {
        YamlConfiguration diskCfg22;
        String pluginFolder = this.getDataFolder().getPath();
        String skinFolder = this.getSkinFolder();
        Predicate<FileConfiguration> noVersion = temp -> !temp.contains("config-version");
        this.updateConfig(pluginFolder, "config.yml", file -> {
            this.reloadConfig();
            for (World world : this.getServer().getWorlds()) {
                this.converter.addGameRuleListener(world);
            }
            this.getServer().getScheduler().runTask((Plugin)this, () -> {
                for (World world : this.getServer().getWorlds()) {
                    for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                        if (this.tracker.isInvalid((LivingEntity)villager, true)) continue;
                        this.converter.getNPC((LivingEntity)villager).ifPresent(IVillagerNPC::refreshBrain);
                    }
                }
            });
            if (this.tracker == null) {
                this.tracker = new VillagerTracker(this);
            }
            if (this.worlds == null) {
                this.worlds = Config.WORLDS_FILTER_WORLDS.asStringList();
            }
        }, file -> this.saveDefaultConfig(), config -> SPECIAL_SECTIONS.stream().filter(arg_0 -> ((FileConfiguration)config).isConfigurationSection(arg_0)).toList(), ConfigChanges.builder().addChange(noVersion, temp -> {
            String pathToInfoLore = "gui.main.items.information.lore";
            List lore = temp.getStringList(pathToInfoLore);
            if (lore.isEmpty()) {
                return;
            }
            lore.replaceAll(line -> line.replace("%partner%", "%current-partner%"));
            temp.set(pathToInfoLore, (Object)lore);
        }, 1).addChange(this.aimVersion(1), temp -> temp.set("gui.new-skin", null), 2).addChange(this.aimVersion(2), temp -> {
            String pathToSetHome = "gui.main.items.set-home.";
            temp.set(pathToSetHome + "only-for-family", null);
            temp.set(pathToSetHome + "only-if-allowed", (Object)false);
            String pathToCombat = "gui.main.items.combat.";
            temp.set(pathToCombat + "only-for-family", null);
            temp.set(pathToCombat + "only-if-allowed", (Object)false);
        }, 3).addChange(this.aimVersion(3), new Consumer<FileConfiguration>(){

            @Override
            public void accept(FileConfiguration temp) {
                this.handleEntityName(temp, "zombie_villager");
                this.handleEntityName(temp, "cave_spider");
                this.handleEntityName(temp, "elder_guardian");
                this.handleEntityName(temp, "wither_skeleton");
                this.handleEntityName(temp, "piglin_brute");
                this.handleEntityName(temp, "zombified_piglin");
                this.handleEntityName(temp, "ender_dragon");
            }

            private void handleEntityName(@NotNull FileConfiguration temp, String path) {
                String name = temp.getString(path);
                if (name != null) {
                    temp.set(path.replace("_", "-"), (Object)name);
                }
            }
        }, 4).addChange(this.aimVersion(4), temp -> {
            List lines = temp.getStringList("custom-nametags.lines");
            if (lines.isEmpty()) {
                return;
            }
            temp.set("custom-nametags.lines", null);
            temp.set("custom-nametags.lines.villager", (Object)lines);
        }, 5).addChange(this.aimVersion(5), temp -> {
            temp.set("villager-title-article", null);
            temp.set("variable-text.profession", null);
        }, 6).build());
        Function<FileConfiguration, List<String>> emptyIgnore = config -> Collections.emptyList();
        this.updateConfig(pluginFolder, "configs/gui.yml", file -> {
            this.guiConfig = YamlConfiguration.loadConfiguration((File)file);
            this.guiConfig.setDefaults((Configuration)new MemoryConfiguration());
        }, file -> this.saveResource("configs/gui.yml"), config -> {
            this.fillGuiIgnoredSections((FileConfiguration)config);
            return GUI_SPECIAL_SECTIONS.stream().filter(arg_0 -> ((FileConfiguration)config).isConfigurationSection(arg_0)).toList();
        }, Collections.emptyList());
        this.updateConfig(pluginFolder, "configs/loot.yml", file -> {
            this.lootConfig = YamlConfiguration.loadConfiguration((File)file);
            this.lootConfig.setDefaults((Configuration)new MemoryConfiguration());
        }, file -> this.saveResource("configs/loot.yml"), config -> config.contains("spawn-loot") ? List.of("spawn-loot") : Collections.emptyList(), Collections.emptyList());
        this.updateConfig(pluginFolder, "configs/gifts.yml", file -> {
            this.giftsConfig = YamlConfiguration.loadConfiguration((File)file);
            this.giftsConfig.setDefaults((Configuration)new MemoryConfiguration());
            if (this.giftManager != null) {
                this.giftManager.loadGiftCategories();
            }
        }, file -> this.saveResource("configs/gifts.yml"), config -> config.contains("items") ? List.of("items") : Collections.emptyList(), Collections.emptyList());
        this.updateConfig(pluginFolder, "configs/variable-text.yml", file -> {
            this.variableTextConfig = YamlConfiguration.loadConfiguration((File)file);
            this.variableTextConfig.setDefaults((Configuration)new MemoryConfiguration());
            this.converter.refreshSchedules();
        }, file -> this.saveResource("configs/variable-text.yml"), config -> config.contains("schedules") ? List.of("schedules") : Collections.emptyList(), Collections.emptyList());
        String[] hologramsFile = new File(pluginFolder, "configs/holograms.yml");
        if (!hologramsFile.exists()) {
            this.saveResource("configs/holograms.yml");
        }
        if ((diskCfg22 = YamlConfiguration.loadConfiguration((File)hologramsFile)).contains("hologram") && !diskCfg22.contains("hologram.menus")) {
            FileUtils.deleteQuietly((File)hologramsFile);
            this.saveResource("configs/holograms.yml");
            diskCfg22 = YamlConfiguration.loadConfiguration((File)hologramsFile);
        }
        try (InputStream jarStream = this.getResource("configs/holograms.yml");){
            if (jarStream != null) {
                YamlConfiguration jarCfg = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(jarStream, StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : jarCfg.getKeys(true)) {
                    if (jarCfg.isConfigurationSection(key) || diskCfg22.contains(key)) continue;
                    diskCfg22.set(key, jarCfg.get(key));
                    changed = true;
                }
                if (changed) {
                    diskCfg22.save((File)hologramsFile);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        this.hologramConfig = diskCfg22;
        this.hologramConfig.setDefaults((Configuration)new MemoryConfiguration());
        this.updateConfig(pluginFolder, "configs/messages/system.yml", file -> {
            this.messages.setConfiguration((FileConfiguration)YamlConfiguration.loadConfiguration((File)file));
            this.messages.loadRegionalConfigs();
        }, file -> this.saveResource("configs/messages/system.yml"), emptyIgnore, ConfigChanges.builder().addChange(noVersion, temp -> temp.set("interact-fail.not-allowed", null), 1).build());
        this.loadConsumer.accept(new File(skinFolder, "male.yml"));
        this.loadConsumer.accept(new File(skinFolder, "female.yml"));
        hologramsFile = new String[]{"desert", "plains", "snow", "savanna", "jungle", "swamp", "taiga"};
        int diskCfg22 = hologramsFile.length;
        for (int i = 0; i < diskCfg22; ++i) {
            String regionType = hologramsFile[i];
            for (String sexName : new String[]{"male", "female"}) {
                this.saveResource("skins/regions/" + regionType + "/" + sexName + ".yml");
                File regionalFile = new File(skinFolder + File.separator + "regions" + File.separator + regionType, sexName + ".yml");
                if (!regionalFile.exists()) continue;
                this.tracker.getFiles().put(sexName + "_" + regionType + ".yml", (Pair<File, FileConfiguration>)Pair.of((Object)regionalFile, (Object)YamlConfiguration.loadConfiguration((File)regionalFile)));
            }
        }
        String resourcePath = "configs/names/default.yml";
        this.saveResource(resourcePath);
        File defaultNamesFile = new File(pluginFolder, resourcePath);
        if (defaultNamesFile.exists()) {
            this.tracker.getFiles().put("names_default.yml", (Pair<File, FileConfiguration>)Pair.of((Object)defaultNamesFile, (Object)YamlConfiguration.loadConfiguration((File)defaultNamesFile)));
        }
        for (String type : new String[]{"desert", "plains", "snow", "savanna", "jungle", "swamp", "taiga"}) {
            String resourcePath2 = "configs/names/" + type + ".yml";
            String mapKey = "names_" + type + ".yml";
            this.saveResource(resourcePath2);
            File regionalFile = new File(pluginFolder, resourcePath2);
            if (!regionalFile.exists()) continue;
            this.tracker.getFiles().put(mapKey, (Pair<File, FileConfiguration>)Pair.of((Object)regionalFile, (Object)YamlConfiguration.loadConfiguration((File)regionalFile)));
        }
    }

    @Contract(pure=true)
    @NotNull
    private Predicate<FileConfiguration> aimVersion(int version) {
        return config -> config.getInt("config-version") == version;
    }

    public void updateConfig(String folderName, String fileName, Consumer<File> reloadAfterUpdating, Consumer<File> resetConfiguration, Function<FileConfiguration, List<String>> ignoreSection, List<ConfigChanges> changes) {
        File file = new File(folderName, fileName);
        FileConfiguration config = PluginUtils.reloadConfig(this, file, resetConfiguration);
        if (config == null) {
            this.getLogger().severe("Can't find {" + file.getName() + "}!");
            return;
        }
        for (ConfigChanges change : changes) {
            this.handleConfigChanges(file, config, change.predicate(), change.consumer(), change.newVersion());
        }
        try {
            ConfigUpdater.update((Plugin)this, fileName, file, ignoreSection.apply(config));
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
        reloadAfterUpdating.accept(file);
    }

    @Nullable
    public InputStream getResource(@NotNull String name) {
        InputStream resource = super.getResource(name);
        if (resource != null) {
            return resource;
        }
        if (!name.equals("male.yml") && !name.equals("female.yml")) {
            return null;
        }
        try {
            File file = new File(this.getSkinFolder(), name);
            if (!file.exists()) {
                return null;
            }
            URL url = file.toURI().toURL();
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        }
        catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    private void handleConfigChanges(@NotNull File file, FileConfiguration config, @NotNull Predicate<FileConfiguration> predicate, Consumer<FileConfiguration> consumer, int newVersion) {
        if (!predicate.test(config)) {
            return;
        }
        int previousVersion = config.getInt("config-version", 0);
        this.getLogger().info("Updated {%s} config to v{%s} (from v{%s})".formatted(file.getName(), newVersion, previousVersion));
        consumer.accept(config);
        config.set("config-version", (Object)newVersion);
        try {
            config.save(file);
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public ItemStack createBaby(boolean isBoy, String babyName, long procreation, @NotNull UUID motherUUID) {
        return this.getItem("baby." + (isBoy ? "boy" : "girl")).replace("%villager-name%", babyName).setData(this.getChildNameKey(), PersistentDataType.STRING, babyName).setData(this.getChildSexKey(), PersistentDataType.STRING, isBoy ? "male" : "female").setData(this.getProcreationKey(), PersistentDataType.LONG, procreation).setData(this.getMotherUUIDKey(), PersistentDataType.STRING, motherUUID.toString()).build();
    }

    @NotNull
    public Shape createWeddingRing() {
        return this.createCraftableItem("wedding-ring", "wedding_ring", this.isRingKey);
    }

    @NotNull
    public Shape createWhistle() {
        return this.createCraftableItem("whistle", this.isWhistleKey);
    }

    @NotNull
    public Shape createCross() {
        return this.createCraftableItem("cross", this.isCrossKey);
    }

    @NotNull
    private Shape createCraftableItem(String item, NamespacedKey identifier) {
        return this.createCraftableItem(item, item, identifier);
    }

    @NotNull
    private Shape createCraftableItem(String item, String recipeName, NamespacedKey identifier) {
        ItemBuilder builder = this.getItem(item).setData(identifier, PersistentDataType.INTEGER, 1);
        boolean shaped = this.getConfig().getBoolean(item + ".crafting.shaped");
        boolean enabled = this.getConfig().getBoolean(item + ".crafting.enabled", true);
        List ingredients = enabled ? this.getConfig().getStringList(item + ".crafting.ingredients") : List.of();
        List shapeList = this.getConfig().getStringList(item + ".crafting.shape");
        return new Shape(this, recipeName, shaped, ingredients, shapeList, builder.build());
    }

    public ItemStack getDivorcePapers() {
        return this.getItem("divorce-papers").setData(this.divorcePapersKey, PersistentDataType.INTEGER, 1).build();
    }

    public void reloadConfig() {
        super.reloadConfig();
        this.getConfig().setDefaults((Configuration)new MemoryConfiguration());
    }

    public ItemBuilder getItem(String path) {
        return this.getItem(path, null);
    }

    public ItemBuilder getItem(String path, @Nullable IVillagerNPC npc) {
        String damageString;
        Object leather;
        PotionType potionType;
        int modelData;
        String amountString;
        FileConfiguration config = path.startsWith("gui.") ? this.guiConfig : (path.startsWith("spawn-loot.") ? this.lootConfig : this.getConfig());
        String name = config.getString(path + ".display-name");
        List lore = config.getStringList(path + ".lore");
        String url = config.getString(path + ".url");
        String materialPath = path + ".material";
        String materialName = config.getString(materialPath, "STONE");
        Material material = PluginUtils.getOrNull(Material.class, materialName);
        ItemBuilder builder = new ItemBuilder(material).setData(this.itemIdKey, PersistentDataType.STRING, path.contains(".") ? path.substring(path.lastIndexOf(".") + 1) : path).setLore(lore);
        if (name != null) {
            builder.setDisplayName(name);
        }
        if ((amountString = config.getString(path + ".amount")) != null) {
            int amount = PluginUtils.getRangedAmount(amountString);
            builder.setAmount(amount);
        }
        if (material == Material.PLAYER_HEAD && url != null) {
            UUID itemUUID = UUID.nameUUIDFromBytes(path.getBytes());
            builder.setHead(itemUUID, url.equals("SELF") ? this.getNPCTextureURL(npc) : url, true);
        }
        if ((modelData = config.getInt(path + ".model-data", Integer.MIN_VALUE)) != Integer.MIN_VALUE) {
            builder.setCustomModelData(modelData);
        }
        for (String enchantmentString : config.getStringList(path + ".enchantments")) {
            int level;
            if (Strings.isNullOrEmpty((String)enchantmentString)) continue;
            String[] data = PluginUtils.splitData(enchantmentString);
            Enchantment enchantment = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft((String)data[0].toLowerCase(Locale.ROOT)));
            try {
                level = PluginUtils.getRangedAmount(data[1]);
            }
            catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
                level = 1;
            }
            if (enchantment == null) continue;
            builder.addEnchantment(enchantment, level);
        }
        for (String flag : config.getStringList(path + ".flags")) {
            ItemFlag flagValue = PluginUtils.getOrNull(ItemFlag.class, flag.toUpperCase(Locale.ROOT));
            if (flagValue == null) continue;
            builder.addItemFlags(flagValue);
        }
        String tippedArrow = config.getString(path + ".tipped");
        if (tippedArrow != null && (potionType = PluginUtils.getValidPotionType(tippedArrow)) != null) {
            builder.setBasePotionData(potionType);
        }
        if ((leather = config.get(path + ".leather-color")) instanceof String) {
            String leatherColor = (String)leather;
            Color color = PluginUtils.getColor(leatherColor);
            if (color != null) {
                builder.setLeatherArmorMetaColor(color);
            }
        } else if (leather instanceof List) {
            List list = (List)leather;
            ArrayList<Color> colors = new ArrayList<Color>();
            for (Object object : list) {
                Color color;
                String string;
                if (!(object instanceof String) || (string = (String)object).equalsIgnoreCase("$RANDOM") || (color = PluginUtils.getColor(string)) == null) continue;
                colors.add(color);
            }
            if (!colors.isEmpty()) {
                Color color = (Color)colors.get(RandomUtils.nextInt((int)0, (int)colors.size()));
                builder.setLeatherArmorMetaColor(color);
            }
        }
        if (config.contains(path + ".firework")) {
            ConfigurationSection section = config.getConfigurationSection(path + ".firework.firework-effects");
            if (section == null) {
                return builder;
            }
            HashSet<FireworkEffect> effects = new HashSet<FireworkEffect>();
            for (String effect : section.getKeys(false)) {
                FireworkEffect.Builder effectBuilder = FireworkEffect.builder();
                String type = config.getString(path + ".firework.firework-effects." + effect + ".type");
                if (type == null) continue;
                FireworkEffect.Type effectType = PluginUtils.getOrEitherRandomOrNull(FireworkEffect.Type.class, type);
                boolean flicker = config.getBoolean(path + ".firework.firework-effects." + effect + ".flicker");
                boolean trail = config.getBoolean(path + ".firework.firework-effects." + effect + ".trail");
                effects.add((effectType != null ? effectBuilder.with(effectType) : effectBuilder).flicker(flicker).trail(trail).withColor(this.getColors(config, path, effect, "colors")).withFade(this.getColors(config, path, effect, "fade-colors")).build());
            }
            String powerString = config.getString(path + ".firework.power");
            int power = PluginUtils.getRangedAmount(powerString != null ? powerString : "");
            if (!effects.isEmpty()) {
                builder.initializeFirework(power, effects.toArray(new FireworkEffect[0]));
            }
        }
        if ((damageString = config.getString(path + ".damage")) != null) {
            short maxDurability = builder.build().getType().getMaxDurability();
            int damage = damageString.equalsIgnoreCase("$RANDOM") ? RandomUtils.nextInt((int)1, (int)maxDurability) : (damageString.contains("%") ? Math.round((float)maxDurability * ((float)PluginUtils.getRangedAmount(damageString.replace("%", "")) / 100.0f)) : PluginUtils.getRangedAmount(damageString));
            if (damage > 0) {
                builder.setDamage(Math.min(damage, maxDurability));
            }
        }
        return builder;
    }

    public String getNPCTextureURL(@Nullable IVillagerNPC npc) {
        if (Config.DISABLE_SKINS.asBool()) {
            return VILLAGER_HEAD_TEXTURE;
        }
        if (npc == null) {
            return UNKNOWN_HEAD_TEXTURE;
        }
        TextureProperty textures = this.tracker.getTextures(npc.getSex(), "none", npc.getSkinTextureId());
        return textures.getName().equals("error") ? UNKNOWN_HEAD_TEXTURE : PluginUtils.getURLFromTexture(textures.getValue());
    }

    @NotNull
    private Set<Color> getColors(@NotNull FileConfiguration config, String path, String effect, String needed) {
        HashSet<Color> colors = new HashSet<Color>();
        for (String colorString : config.getStringList(path + ".firework.firework-effects." + effect + "." + needed)) {
            Color color = PluginUtils.getColor(colorString);
            if (color == null) continue;
            colors.add(color);
        }
        return colors;
    }

    public void saveResource(String name) {
        File file = new File(this.getDataFolder(), name);
        if (!file.exists()) {
            this.saveResource(name, false);
        }
    }

    public boolean isMarried(@NotNull Player player) {
        String partner = (String)player.getPersistentDataContainer().get(this.marriedWith, PersistentDataType.STRING);
        if (partner == null) {
            return false;
        }
        IVillagerNPC partnerInfo = this.tracker.getOffline(UUID.fromString(partner));
        if (partnerInfo == null) {
            player.getPersistentDataContainer().remove(this.marriedWith);
            return false;
        }
        return true;
    }

    public void reloadDefaultTargetEntities() {
        this.defaultTargets.clear();
        for (String entity : this.getConfig().getStringList("default-target-entities")) {
            Class clazz;
            EntityType type = PluginUtils.getOrNull(EntityType.class, entity.toUpperCase(Locale.ROOT));
            if (type == null || (clazz = type.getEntityClass()) == null || !Monster.class.isAssignableFrom(clazz)) continue;
            this.defaultTargets.add(entity);
        }
    }

    public void reloadWantedItems() {
        this.wantedItems.clear();
        this.wantedItems.addAll(this.giftManager.getAllGifts());
        EnumSet<Material> existing = EnumSet.noneOf(Material.class);
        for (Gift g : this.wantedItems) {
            existing.add(g.getType());
        }
        for (String entry : this.getConfig().getStringList("default-wanted-items")) {
            Material material;
            int paren;
            String s = entry.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            int colon = s.indexOf(58);
            if (colon >= 0) {
                s = s.substring(colon + 1);
            }
            if ((paren = s.indexOf(40)) >= 0) {
                s = s.substring(0, paren);
            }
            if ((material = Material.matchMaterial((String)(s = s.replace("*", "").trim()))) == null || material == Material.AIR || existing.contains(material)) continue;
            this.wantedItems.add(new Gift(material, GiftCategory.NEUTRAL, 0, false));
            existing.add(material);
        }
    }

    public void reloadLoots() {
        this.loots.clear();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.loots.put(this.slotName(slot), this.createLoot("equipment", slot));
        }
        this.loots.put("inventory-items", this.createLoot("inventory-items"));
    }

    public boolean isDisabledIn(@NotNull World world) {
        return !this.isEnabledIn(world.getName());
    }

    public boolean isEnabledIn(String world) {
        String type = Config.WORLDS_FILTER_TYPE.asString();
        if (type == null || !FILTER_TYPES.contains(type.toUpperCase(Locale.ROOT))) {
            return true;
        }
        boolean contains = this.worlds.contains(world);
        return type.equalsIgnoreCase("WHITELIST") == contains;
    }

    public Gift getWantedItem(IVillagerNPC npc, ItemStack item, boolean isItemPickup) {
        for (Gift wanted : this.wantedItems) {
            if (!wanted.is(item.getType()) || isItemPickup && wanted.isInventoryLootOnly()) continue;
            return wanted;
        }
        return null;
    }

    @Nullable
    public LivingEntity getUnloadedOffline(@NotNull IVillagerNPC offline) {
        Villager villager;
        LivingEntity bukkit = offline.bukkit();
        if (bukkit != null) {
            return bukkit;
        }
        Location location = offline.getLastKnownPosition().asLocation();
        if (location.getWorld() == null) {
            return null;
        }
        Chunk chunk = location.getWorld().getChunkAt(location);
        chunk.load();
        chunk.getEntities();
        Entity inChunk = Bukkit.getEntity((UUID)offline.getUniqueId());
        return inChunk instanceof Villager ? (villager = (Villager)inChunk) : null;
    }

    public void openWhistleGUI(Player player, @Nullable Integer page, @Nullable String keyword) {
        List<IVillagerNPC> family = this.tracker.getOfflineVillagers().stream().filter(offline -> {
            Villager villager;
            Villager bukkit;
            LivingEntity patt0$temp = offline.bukkit();
            Villager villager2 = bukkit = patt0$temp instanceof Villager ? (villager = (Villager)patt0$temp) : null;
            if (bukkit != null) {
                Optional<IVillagerNPC> online = this.converter.getNPC((LivingEntity)bukkit);
                return online.isPresent() && online.get().isFamily(player, true);
            }
            return offline.isFamily(player, true);
        }).toList();
        if (family.isEmpty()) {
            this.messages.send((CommandSender)player, Messages.Message.WHISTLE_NO_FAMILY);
            return;
        }
        new WhistleGUI(this, player, family.stream(), page, keyword);
    }

    public void equipVillager(LivingEntity living, boolean force) {
        if (this.invalidLoots()) {
            return;
        }
        Optional<IVillagerNPC> npc = this.converter.getNPC(living);
        if (npc.isEmpty() || npc.get().isEquipped() || !force || this.tracker.isInvalid(living, true)) {
            return;
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) {
            return;
        }
        HashMap<EquipmentSlot, ItemLoot> equipped = new HashMap<EquipmentSlot, ItemLoot>();
        npc.get().setEquipped(true);
        block0: for (EquipmentSlot slot : EquipmentSlot.values()) {
            String name = this.slotName(slot);
            List<ItemLoot> loots = this.loots.get(name);
            if (loots == null) continue;
            double chance = Math.random();
            for (ItemLoot loot : loots) {
                ItemStack item;
                if (chance > loot.chance() || (item = loot.getItem()) == null) continue;
                equipment.setItem(slot, item);
                equipped.put(slot, loot);
                continue block0;
            }
        }
        List<ItemLoot> loots = this.loots.get("inventory-items");
        if (loots == null) {
            return;
        }
        double chance = Math.random();
        for (ItemLoot loot : loots) {
            ItemStack item;
            if (chance > loot.chance() || (item = loot.getItem()) == null || !(loot.forRange() && this.testBothHand(equipped, ItemStackUtils::isRangeWeapon) || loot.bow() && this.testBothHand(equipped, inHand -> inHand.getType() == Material.BOW)) && (!loot.crossbow() || !this.testBothHand(equipped, inHand -> inHand.getType() == Material.CROSSBOW))) continue;
            if (loot.offHandIfPossible() && equipped.get(EquipmentSlot.OFF_HAND) == null) {
                equipment.setItemInOffHand(item);
                continue;
            }
            if (!(living instanceof InventoryHolder)) continue;
            InventoryHolder holder = (InventoryHolder)living;
            holder.getInventory().addItem(new ItemStack[]{item});
        }
    }

    private boolean invalidLoots() {
        if (this.loots.isEmpty()) {
            return true;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (this.loots.get(this.slotName(slot)) == null) continue;
            return false;
        }
        return this.loots.get("inventory-items") == null;
    }

    private boolean testBothHand(Map<EquipmentSlot, ItemLoot> equipped, Predicate<ItemStack> predicate) {
        return this.testHand(equipped, predicate, EquipmentSlot.HAND) || this.testHand(equipped, predicate, EquipmentSlot.OFF_HAND);
    }

    private boolean testHand(@NotNull Map<EquipmentSlot, ItemLoot> equipped, Predicate<ItemStack> predicate, EquipmentSlot slot) {
        ItemLoot hand = equipped.get(slot);
        if (hand == null) {
            return false;
        }
        return predicate.test(hand.getItem());
    }

    @NotNull
    public List<ItemLoot> createLoot(String sector) {
        return this.createLoot(sector, null);
    }

    @NotNull
    public List<ItemLoot> createLoot(String sector, @Nullable EquipmentSlot part) {
        FileConfiguration config = this.lootConfig;
        String name = sector + (String)(part != null ? "." + this.slotName(part) : "");
        ConfigurationSection section = config.getConfigurationSection("spawn-loot." + name);
        if (section == null) {
            return Collections.emptyList();
        }
        ArrayList<ItemLoot> loots = new ArrayList<ItemLoot>();
        for (String path : section.getKeys(false)) {
            boolean onlyForBow;
            boolean onlyForCrossbow;
            double chance = config.getDouble("spawn-loot." + name + "." + path + ".chance", 1.0);
            boolean onlyForRangeWeapon = config.getBoolean("spawn-loot." + name + "." + path + ".only-for-range-weapon");
            if (onlyForRangeWeapon) {
                onlyForCrossbow = true;
                onlyForBow = true;
            } else {
                onlyForBow = config.getBoolean("spawn-loot." + name + "." + path + ".only-for-bow");
                onlyForCrossbow = config.getBoolean("spawn-loot." + name + "." + path + ".only-for-crossbow");
            }
            boolean offHandIfPossible = config.getBoolean("spawn-loot." + name + "." + path + ".off-hand-if-possible");
            loots.add(new ItemLoot(() -> this.getItem("spawn-loot." + name + "." + path).build(), chance, onlyForBow, onlyForCrossbow, offHandIfPossible));
        }
        loots.sort(Comparator.comparingDouble(ItemLoot::chance));
        return loots;
    }

    @NotNull
    private String slotName(@NotNull EquipmentSlot slot) {
        return slot.name().toLowerCase(Locale.ROOT).replace("_", "-");
    }

    @Contract(value="_ -> new")
    @NotNull
    public NamespacedKey key(String name) {
        return new NamespacedKey((Plugin)this, name);
    }

    @Contract(pure=true)
    @NotNull
    public String getSkinFolder() {
        return String.valueOf(this.getDataFolder()) + File.separator + "skins";
    }

    public String getProfessionFormatted(@NotNull Villager.Profession profession, boolean isMale) {
        return this.getProfessionFormatted(profession.name().toLowerCase(Locale.ROOT), isMale);
    }

    public String getProfessionFormatted(String profession, boolean isMale) {
        String sex = isMale ? "male" : "female";
        return this.variableTextConfig.getString(String.format("variable-text.profession.%s.%s", sex, profession), PluginUtils.capitalizeFully(profession));
    }

    @Nullable
    public static OfflineDataWrapper villagerDataFromPDC(RealisticVillagers plugin, PersistentDataContainer container) {
        OfflineDataWrapper wrapper2;
        try {
            wrapper2 = (OfflineDataWrapper)container.get(plugin.getNpcValuesKey(), VILLAGER_DATA);
            if (wrapper2 != null) {
                return wrapper2;
            }
        }
        catch (Exception wrapper2) {
            // empty catch block
        }
        try {
            OfflineDataWrapper legacy = plugin.getConverter().getNPCFromPDC(container, plugin.getNpcValuesKey());
            if (legacy != null) {
                return legacy;
            }
        }
        catch (Exception legacy) {
            // empty catch block
        }
        try {
            wrapper2 = (OfflineDataWrapper)container.get(plugin.getLegacyNpcValuesKey(), VILLAGER_DATA);
            if (wrapper2 != null) {
                return wrapper2;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    @Nullable
    public static OfflineDataWrapper villagerDataFromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
        try {
            return (OfflineDataWrapper)VILLAGER_DATA.fromPrimitive((Object)primitive, context);
        }
        catch (Exception exception) {
            return null;
        }
    }

    public InventoryListeners getInventoryListeners() {
        return this.inventoryListeners;
    }

    public OtherListeners getOtherListeners() {
        return this.otherListeners;
    }

    public PlayerListeners getPlayerListeners() {
        return this.playerListeners;
    }

    public VillagerListeners getVillagerListeners() {
        return this.villagerListeners;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public Consumer<File> getLoadConsumer() {
        return this.loadConsumer;
    }

    static {
        ConfigurationSerialization.registerClass(GossipEntryWrapper.class);
        ConfigurationSerialization.registerClass(OfflineDataWrapper.class);
        VILLAGER_DATA = new ConfigurationSerializableDataType<OfflineDataWrapper>(OfflineDataWrapper.class);
    }

    public record ConfigChanges(Predicate<FileConfiguration> predicate, Consumer<FileConfiguration> consumer, int newVersion) {
        @NotNull
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final List<ConfigChanges> changes = new ArrayList<ConfigChanges>();

            public Builder addChange(Predicate<FileConfiguration> predicate, Consumer<FileConfiguration> consumer, int newVersion) {
                this.changes.add(new ConfigChanges(predicate, consumer, newVersion));
                return this;
            }

            public List<ConfigChanges> build() {
                return ImmutableList.copyOf(this.changes);
            }
        }
    }
}

