package br.com.loghorizon.gardengifts;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventExecutor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class LogHorizonGardenGifts extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final String RV_EVENT = "me.matsubara.realisticvillagers.event.VillagerPickGiftEvent";
    private static final Pattern COLOR_CODE = Pattern.compile("(?i)[&§][0-9A-FK-ORX]");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_TEXT = Pattern.compile("[^a-z0-9: ]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private boolean hookRegistered;
    private String hookError = "Ainda não inicializado";

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginCommand command = getCommand("lhgardengifts");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        Bukkit.getScheduler().runTask(this, this::registerRealisticVillagersHook);
    }

    private void registerRealisticVillagersHook() {
        Plugin realisticVillagers = getServer().getPluginManager().getPlugin("RealisticVillagers");
        if (realisticVillagers == null || !realisticVillagers.isEnabled()) {
            hookError = "RealisticVillagers não foi encontrado ou está desativado";
            getLogger().severe(hookError + ". O complemento ficará inativo sem afetar os outros plugins.");
            return;
        }

        try {
            ClassLoader loader = realisticVillagers.getClass().getClassLoader();
            Class<? extends Event> eventClass = Class.forName(RV_EVENT, true, loader).asSubclass(Event.class);
            Listener listener = new Listener() { };
            EventExecutor executor = (ignored, event) -> handleGiftEvent(event);

            getServer().getPluginManager().registerEvent(
                    eventClass,
                    listener,
                    EventPriority.NORMAL,
                    executor,
                    this,
                    true);

            hookRegistered = true;
            hookError = "Nenhum";
            getLogger().info("Integração ativa com RealisticVillagers " + realisticVillagers.getDescription().getVersion() + ".");
        } catch (ReflectiveOperationException | LinkageError exception) {
            hookError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            getLogger().log(Level.SEVERE, "Não foi possível registrar o evento de presentes do RealisticVillagers.", exception);
        }
    }

    private void handleGiftEvent(Event event) {
        if (!getConfig().getBoolean("enabled", true)) return;

        try {
            Object npc = invoke(event, "getNPC");
            Object playerObject = invoke(event, "getGifter");
            Object giftObject = invoke(event, "getGift");
            if (!(playerObject instanceof Player player) || !(giftObject instanceof ItemStack gift) || npc == null) return;

            GiftInfo info = classify(gift);
            if (info == null) return;

            DesiredGift desired = calculateDesired(npc, player, info);
            if (desired.total() <= 0) {
                debug("Item reconhecido, mas sem reputação para este aldeão: " + info.searchableName());
                return;
            }

            int before = getReputation(npc, player);
            Material bridge = Material.matchMaterial(getConfig().getString("compatibility.bridge-material", "APPLE"));
            if (bridge == null || bridge.isAir()) {
                getLogger().warning("compatibility.bridge-material é inválido; o presente não foi alterado.");
                return;
            }

            // O evento é chamado antes de o RealisticVillagers classificar o presente. Ao usar
            // um material já aceito, preservamos toda a animação, consumo, mensagens e cooldown
            // nativos. O nome/PDC já foi lido e o item será consumido pelo aldeão.
            gift.setType(bridge);

            long delay = Math.max(1L, getConfig().getLong("compatibility.delay-ticks", 1L));
            Bukkit.getScheduler().runTaskLater(this, () -> applyRemainingReputation(npc, player.getUniqueId(), before, desired), delay);
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING, "Falha ao processar um presente do Garden.", throwable);
        }
    }

    private void applyRemainingReputation(Object npc, UUID playerId, int before, DesiredGift desired) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        try {
            int afterNative = getReputation(npc, player);
            int nativeGain = Math.max(0, afterNative - before);
            boolean requireNativeGain = getConfig().getBoolean("compatibility.require-native-reputation-gain", true);

            // Se o sistema nativo não concedeu reputação, geralmente o cooldown ou o limite
            // diário bloqueou o presente. Não ultrapassamos essa proteção com um bônus direto.
            if (requireNativeGain && nativeGain <= 0) {
                debug("Bônus cancelado porque o RealisticVillagers não registrou ganho nativo.");
                return;
            }

            int bonus = Math.max(0, desired.total() - nativeGain);
            if (bonus <= 0) {
                debug("Presente processado sem bônus adicional. Ganho nativo=" + nativeGain);
                return;
            }

            invokeCompatible(npc, "addMinorPositive", player, bonus);
            sendBonusMessage(player, bonus, desired);
            debug("Bônus aplicado: " + bonus + " (desejado=" + desired.total() + ", nativo=" + nativeGain + ")");
        } catch (Throwable throwable) {
            getLogger().log(Level.WARNING, "Falha ao aplicar reputação extra do presente.", throwable);
        }
    }

    private DesiredGift calculateDesired(Object npc, Player player, GiftInfo info) throws ReflectiveOperationException {
        FileConfiguration config = getConfig();
        boolean farmer = isFarmer(npc);

        int base;
        if (info.seed()) {
            if (!farmer) return new DesiredGift(0, false, false, false, info);
            base = Math.max(0, config.getInt("reputation.farmer-seed", 3));
        } else {
            base = switch (info.quality()) {
                case NORMAL -> Math.max(0, config.getInt("reputation.normal-fruit", 2));
                case QUALITY -> Math.max(0, config.getInt("reputation.quality-fruit", 4));
                case PERFECT -> Math.max(0, config.getInt("reputation.perfect-fruit", 7));
            };
        }

        boolean childBonus = !info.seed()
                && config.getBoolean("bonuses.child.enabled", true)
                && isChild(npc)
                && configuredSpecies("bonuses.child.species", info.species());

        boolean partnerBonus = !info.seed()
                && config.getBoolean("bonuses.partner.enabled", true)
                && isPartner(npc, player)
                && configuredSpecies("bonuses.partner.species", info.species());

        int total = base;
        if (childBonus) total += Math.max(0, config.getInt("bonuses.child.amount", 1));
        if (partnerBonus) total += Math.max(0, config.getInt("bonuses.partner.amount", 1));

        return new DesiredGift(total, info.seed() && farmer, childBonus, partnerBonus, info);
    }

    private @Nullable GiftInfo classify(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        boolean requireName = getConfig().getBoolean("recognition.require-custom-display-name", true);
        if (requireName && !meta.hasDisplayName()) return null;

        StringBuilder text = new StringBuilder();
        if (meta.hasDisplayName()) text.append(meta.getDisplayName()).append(' ');

        if (getConfig().getBoolean("recognition.read-lore", true) && meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) text.append(line).append(' ');
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        boolean gardenPdc = false;
        for (NamespacedKey key : container.getKeys()) {
            String keyText = normalize(key.getNamespace() + ":" + key.getKey());
            text.append(keyText).append(' ');
            if (keyText.contains("garden") || keyText.contains("loghorizon") || keyText.contains("fruit") || keyText.contains("plant")) {
                gardenPdc = true;
            }
            if (getConfig().getBoolean("recognition.read-string-pdc-values", true)) {
                String value = container.get(key, PersistentDataType.STRING);
                if (value != null) text.append(value).append(' ');
            }
        }

        if (getConfig().getBoolean("recognition.require-garden-pdc", false) && !gardenPdc) return null;

        String searchable = normalize(text.toString());
        Species species = Species.find(searchable);
        if (species == null) return null;

        boolean seed = searchable.contains("semente") || searchable.contains("seed");
        Quality quality;
        if (searchable.contains("perfeit") || searchable.contains("perfect")) {
            quality = Quality.PERFECT;
        } else if (searchable.contains("qualidade") || searchable.contains("quality")) {
            quality = Quality.QUALITY;
        } else {
            quality = Quality.NORMAL;
        }

        return new GiftInfo(species, quality, seed, searchable);
    }

    private int getReputation(Object npc, Player player) throws ReflectiveOperationException {
        Object value;
        try {
            value = invokeCompatible(npc, "getReputation", player);
        } catch (NoSuchMethodException ignored) {
            value = invokeCompatible(npc, "getReputation", player.getUniqueId());
        }
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean isFarmer(Object npc) throws ReflectiveOperationException {
        Object bukkit = invokeCompatible(npc, "bukkit");
        return bukkit instanceof Villager villager && villager.getProfession() == Villager.Profession.FARMER;
    }

    private boolean isChild(Object npc) throws ReflectiveOperationException {
        Object bukkit = invokeCompatible(npc, "bukkit");
        return bukkit instanceof Villager villager && !villager.isAdult();
    }

    private boolean isPartner(Object npc, Player player) throws ReflectiveOperationException {
        try {
            Object value = invokeCompatible(npc, "isPartner", player);
            return value instanceof Boolean bool && bool;
        } catch (NoSuchMethodException ignored) {
            Object value = invokeCompatible(npc, "isPartner", player.getUniqueId());
            return value instanceof Boolean bool && bool;
        }
    }

    private boolean configuredSpecies(String path, Species species) {
        Set<String> configured = Set.copyOf(getConfig().getStringList(path));
        return configured.stream().map(LogHorizonGardenGifts::normalize).anyMatch(species.id()::equals);
    }

    private void sendBonusMessage(Player player, int bonus, DesiredGift desired) {
        if (!getConfig().getBoolean("messages.enabled", true)) return;

        String path;
        if (desired.farmerSeed()) {
            path = "messages.farmer-seed";
        } else if (desired.childBonus() && !desired.partnerBonus() && desired.info().quality() == Quality.NORMAL) {
            path = "messages.child-bonus";
        } else if (desired.partnerBonus() && !desired.childBonus() && desired.info().quality() == Quality.NORMAL) {
            path = "messages.partner-bonus";
        } else {
            path = "messages.extra-reputation";
        }

        String message = getConfig().getString(path, "&aPresente especial! &e+%amount% reputação extra.")
                .replace("%amount%", Integer.toString(bonus));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object invokeCompatible(Object target, String methodName, Object... arguments) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != arguments.length) continue;

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                Object argument = arguments[index];
                if (argument != null && !wrap(parameterTypes[index]).isAssignableFrom(argument.getClass())) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) continue;
            return method.invoke(target, arguments);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName + Arrays.toString(arguments));
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private void debug(String message) {
        if (getConfig().getBoolean("debug", false)) getLogger().info("[DEBUG] " + message);
    }

    private static String normalize(String input) {
        if (input == null || input.isBlank()) return "";
        String output = ChatColor.stripColor(COLOR_CODE.matcher(input).replaceAll(""));
        if (output == null) output = input;
        output = Normalizer.normalize(output, Normalizer.Form.NFD);
        output = COMBINING_MARKS.matcher(output).replaceAll("");
        output = output.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        output = NON_TEXT.matcher(output).replaceAll(" ");
        return MULTI_SPACE.matcher(output).replaceAll(" ").trim();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("loghorizon.gardengifts.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão.");
            return true;
        }

        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("reload")) {
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "LogHorizonGardenGifts recarregado.");
            return true;
        }

        if (subcommand.equals("status")) {
            Plugin rv = getServer().getPluginManager().getPlugin("RealisticVillagers");
            Plugin gardenPlus = getServer().getPluginManager().getPlugin("LogHorizonGardenPlus");
            sender.sendMessage(ChatColor.GOLD + "LogHorizonGardenGifts 1.0.0");
            sender.sendMessage(ChatColor.GRAY + "Hook de presentes: " + (hookRegistered ? ChatColor.GREEN + "ATIVO" : ChatColor.RED + "INATIVO"));
            sender.sendMessage(ChatColor.GRAY + "RealisticVillagers: " + pluginStatus(rv));
            sender.sendMessage(ChatColor.GRAY + "LogHorizonGardenPlus: " + pluginStatus(gardenPlus));
            if (!hookRegistered) sender.sendMessage(ChatColor.RED + "Motivo: " + hookError);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Use /" + label + " status ou /" + label + " reload.");
        return true;
    }

    private static String pluginStatus(Plugin plugin) {
        if (plugin == null) return ChatColor.RED + "NÃO ENCONTRADO";
        return plugin.isEnabled() ? ChatColor.GREEN + "ATIVO " + plugin.getDescription().getVersion() : ChatColor.RED + "DESATIVADO";
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String typed = args[0].toLowerCase(Locale.ROOT);
        List<String> output = new ArrayList<>();
        for (String option : List.of("status", "reload")) {
            if (option.startsWith(typed)) output.add(option);
        }
        return output;
    }

    private enum Quality {
        NORMAL,
        QUALITY,
        PERFECT
    }

    private enum Species {
        APPLE("apple", "maca", "apple"),
        BLUEBERRY("blueberry", "mirtilo", "blueberry"),
        CHERRY("cherry", "cereja", "cherry"),
        CRANBERRY("cranberry", "oxicoco", "cranberry"),
        GRAPE("grape", "uva", "grape"),
        LEMON("lemon", "limao siciliano", "lemon"),
        LIME("lime", "limao verde", "lime"),
        ORANGE("orange", "laranja", "orange"),
        PEACH("peach", "pessego", "peach"),
        RASPBERRY("raspberry", "framboesa", "raspberry"),
        STRAWBERRY("strawberry", "morango", "strawberry");

        private final String id;
        private final List<String> aliases;

        Species(String id, String... aliases) {
            this.id = id;
            this.aliases = Arrays.stream(aliases).map(LogHorizonGardenGifts::normalize).toList();
        }

        String id() {
            return id;
        }

        static @Nullable Species find(String text) {
            for (Species species : values()) {
                for (String alias : species.aliases) {
                    if (text.contains(alias)) return species;
                }
            }
            return null;
        }
    }

    private record GiftInfo(Species species, Quality quality, boolean seed, String searchableName) { }

    private record DesiredGift(
            int total,
            boolean farmerSeed,
            boolean childBonus,
            boolean partnerBonus,
            GiftInfo info) { }
}
