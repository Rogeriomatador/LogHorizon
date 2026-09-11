package com.loghorizon.stellarity.b53altarfallback;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TEST SHIM B5.3
 *
 * Não substitui a B5.2. Ele só cobre o caminho que o log de 11/09/2026 provou:
 * no Bedrock, algumas colocações laterais do Altar chegam ao Paper como
 * CRYING_OBSIDIAN em vez de CHISELED_QUARTZ_BLOCK.
 *
 * Regra de segurança: Crying Obsidian só é convertido quando a identidade do
 * item confirma stellarity:altar_of_the_sacred de forma direta, na mão atual,
 * ou por um cache curtíssimo criado ao interagir com esse item.
 */
public final class LogHorizonStellarityB53AltarCarrierShim extends JavaPlugin implements Listener {
    private static final String ALTAR_MODEL = "stellarity:altar_of_the_sacred";
    private static final long ALTAR_CACHE_MS = 1800L;
    private static final long LOCATION_DEDUPE_MS = 1200L;

    private final Map<UUID, Long> altarCacheUntil = new HashMap<>();
    private final Map<String, Long> handledLocationsUntil = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("B5.3 TEST shim ativo: CRYING_OBSIDIAN só será tratado como Altar com identidade Stellarity confirmada.");
    }

    @Override
    public void onDisable() {
        altarCacheUntil.clear();
        handledLocationsUntil.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }

        if (isAltarItem(event.getItem())) {
            rememberAltar(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack eventItem = event.getItemInHand();

        if (isAltarItem(eventItem)) {
            rememberAltar(player);
        }

        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.CRYING_OBSIDIAN) {
            return;
        }

        boolean direct = isAltarItem(eventItem);
        boolean mainHand = isAltarItem(player.getInventory().getItemInMainHand());
        boolean offHand = isAltarItem(player.getInventory().getItemInOffHand());
        boolean cached = hasFreshAltarCache(player);

        if (!direct && !mainHand && !offHand && !cached) {
            getLogger().fine(() -> "Ignorando CRYING_OBSIDIAN comum em " + describe(placed)
                    + " player=" + player.getName());
            return;
        }

        String locationKey = locationKey(placed);
        long now = System.currentTimeMillis();
        Long alreadyUntil = handledLocationsUntil.get(locationKey);
        if (alreadyUntil != null && alreadyUntil >= now) {
            getLogger().warning("Evento duplicado ignorado no mesmo carrier: " + locationKey);
            return;
        }
        handledLocationsUntil.put(locationKey, now + LOCATION_DEDUPE_MS);

        getLogger().info("B5.3 carrier detectado: player=" + player.getName()
                + " block=" + describe(placed)
                + " direct=" + direct
                + " main=" + mainHand
                + " off=" + offHand
                + " cached=" + cached);

        // Executa no tick seguinte para não disputar a mutação do bloco dentro do
        // BlockPlaceEvent com o Geyser/Stellarity. Só age se o carrier ainda for
        // exatamente CRYING_OBSIDIAN naquele X/Y/Z.
        Bukkit.getScheduler().runTask(this, () -> repairAltarCarrier(player, placed, locationKey));
    }

    private void repairAltarCarrier(Player player, Block block, String locationKey) {
        if (block.getType() != Material.CRYING_OBSIDIAN) {
            getLogger().info("B5.3 skip: carrier mudou antes do tick seguinte: " + describe(block)
                    + " atual=" + block.getType());
            return;
        }

        block.setType(Material.CHISELED_QUARTZ_BLOCK, false);

        String dimension = block.getWorld().getKey().toString();
        String command = "execute in " + dimension
                + " positioned " + block.getX() + " " + block.getY() + " " + block.getZ()
                + " run function loghorizon:stellarity_safe/altar";

        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        getLogger().info("B5.3 altar fallback: player=" + player.getName()
                + " carrier=" + locationKey
                + " crying_obsidian->chiseled_quartz"
                + " functionDispatched=" + dispatched);
    }

    private void rememberAltar(Player player) {
        altarCacheUntil.put(player.getUniqueId(), System.currentTimeMillis() + ALTAR_CACHE_MS);
    }

    private boolean hasFreshAltarCache(Player player) {
        Long until = altarCacheUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            altarCacheUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * Usa várias formas de leitura sem depender de uma única API específica de
     * item_model. O Paper 26.2 expõe item_model, mas reflexão + serialize/toString
     * deixa o shim mais tolerante entre builds 65 e 84.
     */
    private boolean isAltarItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        Object reflectedModel = invokeNoArgs(meta, "getItemModel");
        if (containsAltarModel(reflectedModel)) {
            return true;
        }

        Object asString = invokeNoArgs(meta, "getAsString");
        if (containsAltarModel(asString)) {
            return true;
        }

        try {
            if (containsAltarModel(meta.serialize())) {
                return true;
            }
        } catch (Throwable ignored) {
            // Fallbacks abaixo continuam válidos.
        }

        return containsAltarModel(meta) || containsAltarModel(item);
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private boolean containsAltarModel(Object value) {
        if (value == null) {
            return false;
        }
        return String.valueOf(value).toLowerCase(Locale.ROOT).contains(ALTAR_MODEL);
    }

    private String locationKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String describe(Block block) {
        return block.getWorld().getKey() + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
