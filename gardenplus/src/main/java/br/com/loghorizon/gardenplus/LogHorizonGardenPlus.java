package br.com.loghorizon.gardenplus;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.user.SkillsUser;
import dev.jsinco.brewery.garden.Garden;
import dev.jsinco.brewery.garden.plant.GardenPlant;
import dev.jsinco.brewery.garden.plant.PlacedFruitDisplays;
import dev.jsinco.brewery.garden.plant.PlantType;
import dev.jsinco.brewery.garden.plant.item.PlantItem;
import dev.jsinco.brewery.garden.plant.item.PlayerHeadBased;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Complemento de progressão para Garden 2.0.1.
 *
 * Não adiciona receitas, bebidas ou efeitos de poção. As frutas mantêm todos os
 * metadados e propriedades alimentares fornecidos pelo Garden.
 */
public final class LogHorizonGardenPlus extends JavaPlugin implements Listener, CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<UUID, SpecialPlant> specialPlants = new ConcurrentHashMap<>();
    private final Map<String, Integer> actionCooldowns = new ConcurrentHashMap<>();

    private NamespacedKey qualityKey;
    private File plantsFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        qualityKey = new NamespacedKey(this, "fruit_quality");
        plantsFile = new File(getDataFolder(), "plants.yml");
        loadSpecialPlants();

        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("lhgarden") != null) {
            getCommand("lhgarden").setExecutor(this);
        }

        getLogger().info("GardenPlus ativo: Garden 2.0.1 + AuraSkills 2.3.12.");
    }

    @Override
    public void onDisable() {
        saveSpecialPlants();
    }

    /* --------------------------------------------------------------------- */
    /* Raridade individual das sementes                                      */
    /* --------------------------------------------------------------------- */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSeedSourceBroken(BlockBreakEvent event) {
        if (!getConfig().getBoolean("seed-rarity.enabled", true)) {
            return;
        }

        Location location = event.getBlock().getLocation().toCenterLocation();
        Set<UUID> itemsBefore = nearbyItemIds(location, 1.75);
        Bukkit.getScheduler().runTask(this, () -> filterNewSeedDrops(location, itemsBefore));
    }

    private void filterNewSeedDrops(Location location, Set<UUID> itemsBefore) {
        for (Item dropped : nearbyItems(location, 1.75)) {
            if (itemsBefore.contains(dropped.getUniqueId())) {
                continue;
            }

            ItemStack stack = dropped.getItemStack();
            if (!PlantItem.isSeeds(stack)) {
                continue;
            }

            PlantType type = PlantItem.plantType(stack);
            if (type == null) {
                continue;
            }

            String species = species(type);
            double acceptance = clampPercent(getConfig().getDouble(
                    "seed-rarity.acceptance-percent." + species,
                    100.0
            ));
            if (ThreadLocalRandom.current().nextDouble(100.0) >= acceptance) {
                dropped.remove();
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Plantas férteis e antigas                                              */
    /* --------------------------------------------------------------------- */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onGardenSeedPlaced(PlayerInteractEvent event) {
        if (!getConfig().getBoolean("special-plants.enabled", true)
                || !event.getAction().isRightClick()
                || event.getClickedBlock() == null
                || event.getBlockFace() != BlockFace.UP
                || event.getItem() == null
                || !PlantItem.isSeeds(event.getItem())) {
            return;
        }

        PlantType type = PlantItem.plantType(event.getItem());
        if (type == null) {
            return;
        }

        Block targetBlock = event.getClickedBlock().getRelative(BlockFace.UP);
        String cooldownKey = "place:" + event.getPlayer().getUniqueId() + ':' + blockKey(targetBlock);
        if (!beginAction(cooldownKey)) {
            return;
        }

        Set<UUID> plantsBefore = currentPlantIds();
        Location target = targetBlock.getLocation();
        Bukkit.getScheduler().runTask(this, () -> registerSpecialPlant(
                event.getPlayer(), type, target, plantsBefore
        ));
    }

    private void registerSpecialPlant(Player player, PlantType expectedType, Location target, Set<UUID> plantsBefore) {
        GardenPlant planted = Garden.getGardenRegistry().getPlants().stream()
                .filter(plant -> !plantsBefore.contains(plant.getId()))
                .filter(plant -> sameSpecies(plant.getType(), expectedType))
                .filter(plant -> sameWorld(plant.origin(), target))
                .min(Comparator.comparingDouble(plant -> plant.origin().distanceSquared(target)))
                .orElse(null);

        if (planted == null || planted.origin().distanceSquared(target) > 36.0) {
            return;
        }

        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        double ancientChance = clampPercent(getConfig().getDouble(
                "special-plants.ancient-chance-percent", 0.75
        ));
        double fertileChance = clampPercent(getConfig().getDouble(
                "special-plants.fertile-chance-percent", 4.0
        ));

        SpecialPlant special = SpecialPlant.NONE;
        if (roll < ancientChance) {
            special = SpecialPlant.ANCIENT;
        } else if (roll < ancientChance + fertileChance) {
            special = SpecialPlant.FERTILE;
        }

        if (special == SpecialPlant.NONE) {
            return;
        }

        specialPlants.put(planted.getId(), special);
        saveSpecialPlants();

        String path = special == SpecialPlant.ANCIENT
                ? "messages.ancient-plant"
                : "messages.fertile-plant";
        player.sendMessage(message(path));
    }

    /* --------------------------------------------------------------------- */
    /* Colheita em frutas representadas por entidades                        */
    /* --------------------------------------------------------------------- */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityFruitHarvest(PlayerInteractEntityEvent event) {
        ItemStack hand = event.getPlayer().getInventory().getItem(event.getHand());
        if (hand.getType() != Material.SHEARS) {
            return;
        }

        PersistentDataContainer pdc = event.getRightClicked().getPersistentDataContainer();
        String plantType = pdc.get(PlantItem.PLANT_TYPE_KEY, PersistentDataType.STRING);
        byte[] ownerBytes = pdc.get(PlacedFruitDisplays.OWNING_PLANT, PersistentDataType.BYTE_ARRAY);
        if (plantType == null) {
            return;
        }

        String cooldownKey = "entity:" + event.getPlayer().getUniqueId() + ':'
                + event.getRightClicked().getUniqueId();
        if (!beginAction(cooldownKey)) {
            return;
        }

        UUID owner = uuidFromBytes(ownerBytes);
        Location location = event.getRightClicked().getLocation().clone();
        Set<UUID> itemsBefore = nearbyItemIds(location, scanRadius());
        Bukkit.getScheduler().runTask(this, () -> enhanceHarvest(
                event.getPlayer(), location, itemsBefore, normalizePlantKey(plantType), owner, 0
        ));
    }

    /* --------------------------------------------------------------------- */
    /* Colheita em frutas representadas por blocos/cabeças                   */
    /* --------------------------------------------------------------------- */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFruitHarvest(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()
                || event.getClickedBlock() == null
                || event.getItem() == null
                || event.getItem().getType() != Material.SHEARS) {
            return;
        }

        PlantType type = PlayerHeadBased.getPlantType(event.getClickedBlock());
        if (type == null) {
            return;
        }

        String cooldownKey = "block:" + event.getPlayer().getUniqueId() + ':'
                + blockKey(event.getClickedBlock());
        if (!beginAction(cooldownKey)) {
            return;
        }

        Location location = event.getClickedBlock().getLocation().toCenterLocation();
        GardenPlant ownerPlant = findNearestPlant(type, location, 20.0);
        UUID owner = ownerPlant == null ? null : ownerPlant.getId();
        Set<UUID> itemsBefore = nearbyItemIds(location, scanRadius());

        Bukkit.getScheduler().runTask(this, () -> enhanceHarvest(
                event.getPlayer(), location, itemsBefore, species(type), owner, 0
        ));
    }

    private void enhanceHarvest(
            Player player,
            Location location,
            Set<UUID> itemsBefore,
            String expectedSpecies,
            UUID ownerPlant,
            int attempt
    ) {
        List<Item> matching = nearbyItems(location, scanRadius()).stream()
                .filter(item -> !itemsBefore.contains(item.getUniqueId()))
                .filter(item -> PlantItem.isFruit(item.getItemStack()))
                .filter(item -> {
                    PlantType type = PlantItem.plantType(item.getItemStack());
                    return type != null && species(type).equals(expectedSpecies);
                })
                .toList();

        if (matching.isEmpty()) {
            if (attempt < 1) {
                Bukkit.getScheduler().runTaskLater(this, () -> enhanceHarvest(
                        player, location, itemsBefore, expectedSpecies, ownerPlant, attempt + 1
                ), 1L);
            }
            return;
        }

        // Uma interação válida do Garden gera uma fruta. Processar apenas a primeira
        // evita XP duplicado caso outro plugin também derrube itens na mesma posição.
        Item dropped = matching.getFirst();
        ItemStack fruit = dropped.getItemStack();
        SpecialPlant special = ownerPlant == null
                ? SpecialPlant.NONE
                : specialPlants.getOrDefault(ownerPlant, SpecialPlant.NONE);

        YieldRange yield = yieldFor(expectedSpecies);
        int amount = ThreadLocalRandom.current().nextInt(yield.min(), yield.max() + 1);
        if (special == SpecialPlant.FERTILE) {
            amount += Math.max(0, getConfig().getInt("special-plants.fertile-extra-fruit", 1));
        } else if (special == SpecialPlant.ANCIENT) {
            amount += Math.max(0, getConfig().getInt("special-plants.ancient-extra-fruit", 2));
        }

        int farmingLevel = farmingLevel(player);
        if (rollFarmingLuck(farmingLevel)) {
            amount += Math.max(0, getConfig().getInt("farming-luck.extra-fruit", 1));
        }

        amount = Math.max(1, Math.min(amount, fruit.getType().getMaxStackSize()));
        fruit.setAmount(amount);

        FruitQuality quality = rollQuality(special);
        applyQuality(fruit, quality, expectedSpecies);
        dropped.setItemStack(fruit);

        awardFarmingXp(player, expectedSpecies, special);
    }

    private int farmingLevel(Player player) {
        if (!getConfig().getBoolean("aura-skills.enabled", true)) {
            return 0;
        }
        try {
            SkillsUser user = AuraSkillsApi.get().getUser(player.getUniqueId());
            return user != null && user.isLoaded() ? user.getSkillLevel(Skills.FARMING) : 0;
        } catch (RuntimeException exception) {
            getLogger().warning("Não foi possível consultar o nível de Agricultura: " + exception.getMessage());
            return 0;
        }
    }

    private void awardFarmingXp(Player player, String species, SpecialPlant special) {
        if (!getConfig().getBoolean("aura-skills.enabled", true)) {
            return;
        }

        double xp = Math.max(0.0, getConfig().getDouble("aura-skills.xp." + species, 2.0));
        if (special == SpecialPlant.ANCIENT) {
            xp *= Math.max(1.0, getConfig().getDouble(
                    "special-plants.ancient-xp-multiplier", 1.5
            ));
        }

        try {
            SkillsUser user = AuraSkillsApi.get().getUser(player.getUniqueId());
            if (user != null && user.isLoaded() && user.hasSkillPermission(Skills.FARMING)) {
                user.addSkillXp(Skills.FARMING, xp);
            }
        } catch (RuntimeException exception) {
            getLogger().warning("Não foi possível entregar XP de Agricultura: " + exception.getMessage());
        }
    }

    private boolean rollFarmingLuck(int farmingLevel) {
        if (!getConfig().getBoolean("farming-luck.enabled", true)) {
            return false;
        }

        double base = getConfig().getDouble("farming-luck.base-chance-percent", 2.0);
        double perLevel = getConfig().getDouble("farming-luck.chance-per-farming-level", 0.15);
        double maximum = getConfig().getDouble("farming-luck.max-chance-percent", 12.0);
        double chance = Math.min(maximum, base + Math.max(0, farmingLevel) * perLevel);
        return ThreadLocalRandom.current().nextDouble(100.0) < clampPercent(chance);
    }

    private FruitQuality rollQuality(SpecialPlant special) {
        if (!getConfig().getBoolean("quality.enabled", true)) {
            return FruitQuality.NORMAL;
        }

        double qualityChance = getConfig().getDouble("quality.quality-percent", 13.0);
        double perfectChance = getConfig().getDouble("quality.perfect-percent", 2.0);

        if (special == SpecialPlant.FERTILE) {
            qualityChance += getConfig().getDouble("quality.fertile-quality-bonus", 5.0);
            perfectChance += getConfig().getDouble("quality.fertile-perfect-bonus", 1.0);
        } else if (special == SpecialPlant.ANCIENT) {
            qualityChance += getConfig().getDouble("quality.ancient-quality-bonus", 12.0);
            perfectChance += getConfig().getDouble("quality.ancient-perfect-bonus", 4.0);
        }

        perfectChance = clampPercent(perfectChance);
        qualityChance = Math.min(100.0 - perfectChance, clampPercent(qualityChance));
        double roll = ThreadLocalRandom.current().nextDouble(100.0);

        if (roll < perfectChance) {
            return FruitQuality.PERFECT;
        }
        if (roll < perfectChance + qualityChance) {
            return FruitQuality.QUALITY;
        }
        return FruitQuality.NORMAL;
    }

    private void applyQuality(ItemStack fruit, FruitQuality quality, String species) {
        ItemMeta meta = fruit.getItemMeta();
        meta.getPersistentDataContainer().set(
                qualityKey,
                PersistentDataType.STRING,
                quality.name().toLowerCase(Locale.ROOT)
        );

        if (quality == FruitQuality.NORMAL) {
            fruit.setItemMeta(meta);
            return;
        }

        Component originalName = meta.displayName();
        if (originalName == null) {
            originalName = Component.text(prettySpecies(species));
        }

        String prefixPath = quality == FruitQuality.PERFECT
                ? "quality.perfect-prefix"
                : "quality.quality-prefix";
        String lorePath = quality == FruitQuality.PERFECT
                ? "quality.perfect-lore"
                : "quality.quality-lore";

        meta.displayName(parse(getConfig().getString(prefixPath, "")).append(originalName));
        List<Component> lore = meta.lore() == null
                ? new ArrayList<>()
                : new ArrayList<>(meta.lore());
        lore.add(parse(getConfig().getString(lorePath, "")));
        meta.lore(lore);
        fruit.setItemMeta(meta);
    }

    /* --------------------------------------------------------------------- */
    /* Utilidades do Garden                                                   */
    /* --------------------------------------------------------------------- */

    private GardenPlant findNearestPlant(PlantType type, Location location, double maximumDistance) {
        return Garden.getGardenRegistry().getPlants().stream()
                .filter(plant -> sameSpecies(plant.getType(), type))
                .filter(plant -> sameWorld(plant.origin(), location))
                .filter(plant -> plant.origin().distanceSquared(location) <= maximumDistance * maximumDistance)
                .min(Comparator.comparingDouble(plant -> plant.origin().distanceSquared(location)))
                .orElse(null);
    }

    private Set<UUID> currentPlantIds() {
        Set<UUID> result = new HashSet<>();
        for (GardenPlant plant : Garden.getGardenRegistry().getPlants()) {
            result.add(plant.getId());
        }
        return result;
    }

    private Set<UUID> nearbyItemIds(Location location, double radius) {
        Set<UUID> result = new HashSet<>();
        for (Item item : nearbyItems(location, radius)) {
            result.add(item.getUniqueId());
        }
        return result;
    }

    private List<Item> nearbyItems(Location location, double radius) {
        if (location.getWorld() == null) {
            return List.of();
        }
        Collection<Entity> entities = location.getWorld().getNearbyEntities(
                location, radius, radius, radius, entity -> entity instanceof Item
        );
        List<Item> result = new ArrayList<>();
        for (Entity entity : entities) {
            result.add((Item) entity);
        }
        return result;
    }

    private YieldRange yieldFor(String species) {
        String path = "harvest-yield." + species;
        int min = Math.max(1, getConfig().getInt(path + ".min", 1));
        int max = Math.max(min, getConfig().getInt(path + ".max", min));
        return new YieldRange(min, max);
    }

    private boolean beginAction(String key) {
        int currentTick = Bukkit.getCurrentTick();
        int cooldown = Math.max(1, getConfig().getInt("anti-abuse.harvest-cooldown-ticks", 3));
        Integer previous = actionCooldowns.put(key, currentTick);
        if (previous != null && currentTick - previous <= cooldown) {
            return false;
        }

        if (actionCooldowns.size() > 4096) {
            actionCooldowns.entrySet().removeIf(entry -> currentTick - entry.getValue() > 200);
        }
        return true;
    }

    private double scanRadius() {
        return Math.max(1.0, getConfig().getDouble("anti-abuse.scan-radius", 2.25));
    }

    private String species(PlantType type) {
        return normalizePlantKey(type.key().asString());
    }

    private String normalizePlantKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        int colon = normalized.indexOf(':');
        if (colon >= 0 && colon + 1 < normalized.length()) {
            normalized = normalized.substring(colon + 1);
        }
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < normalized.length()) {
            normalized = normalized.substring(slash + 1);
        }
        return normalized.replace('-', '_');
    }

    private boolean sameSpecies(PlantType first, PlantType second) {
        return species(first).equals(species(second));
    }

    private boolean sameWorld(Location first, Location second) {
        return first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private UUID uuidFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private String prettySpecies(String species) {
        String text = species.replace('_', ' ');
        if (text.isEmpty()) {
            return "Fruta";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /* --------------------------------------------------------------------- */
    /* Persistência das plantas especiais                                    */
    /* --------------------------------------------------------------------- */

    private void loadSpecialPlants() {
        specialPlants.clear();
        if (!plantsFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(plantsFile);
        ConfigurationSection section = yaml.getConfigurationSection("plants");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                SpecialPlant type = SpecialPlant.valueOf(
                        section.getString(key, "NONE").toUpperCase(Locale.ROOT)
                );
                if (type != SpecialPlant.NONE) {
                    specialPlants.put(id, type);
                }
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Registro de planta especial inválido: " + key);
            }
        }
    }

    private synchronized void saveSpecialPlants() {
        if (plantsFile == null) {
            return;
        }
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Não foi possível criar a pasta de dados do GardenPlus.");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, SpecialPlant> entry : specialPlants.entrySet()) {
            yaml.set("plants." + entry.getKey(), entry.getValue().name());
        }
        try {
            yaml.save(plantsFile);
        } catch (IOException exception) {
            getLogger().severe("Não foi possível salvar plants.yml: " + exception.getMessage());
        }
    }

    /* --------------------------------------------------------------------- */
    /* Comando e mensagens                                                    */
    /* --------------------------------------------------------------------- */

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            sender.sendMessage(message("messages.reloaded"));
            return true;
        }
        sender.sendMessage(Component.text("Uso: /lhgarden reload"));
        return true;
    }

    private Component message(String path) {
        return parse(getConfig().getString(path, ""));
    }

    private Component parse(String text) {
        return MINI_MESSAGE.deserialize(text == null ? "" : text);
    }

    private enum SpecialPlant {
        NONE,
        FERTILE,
        ANCIENT
    }

    private enum FruitQuality {
        NORMAL,
        QUALITY,
        PERFECT
    }

    private record YieldRange(int min, int max) {
    }
}
