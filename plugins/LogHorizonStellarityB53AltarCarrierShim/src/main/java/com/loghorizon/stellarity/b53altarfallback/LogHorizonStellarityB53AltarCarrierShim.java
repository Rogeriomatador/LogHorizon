package com.loghorizon.stellarity.b53altarfallback;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TEST SHIM B5.3
 *
 * Não substitui a B5.2. Ele cobre somente o caminho provado pelos logs:
 * uma colocação Bedrock do Altar pode chegar ao Paper como CRYING_OBSIDIAN,
 * embora o ItemStack do próprio BlockPlaceEvent continue identificando
 * stellarity:altar_of_the_sacred.
 *
 * Segurança fail-closed:
 * - só age em CRYING_OBSIDIAN;
 * - exige identidade DIRETA no event.getItemInHand();
 * - usa exatamente event.getBlockPlaced();
 * - nunca procura carrier por câmera/raycast/proximidade;
 * - se o Stellarity já criou Marker/ItemDisplay no mesmo X/Z, não chama a
 *   função novamente, evitando duplicação; apenas corrige o carrier.
 */
public final class LogHorizonStellarityB53AltarCarrierShim extends JavaPlugin implements Listener {
    private static final String ALTAR_MODEL = "stellarity:altar_of_the_sacred";
    private static final String ALTAR_TAG = "stellarity.altar_of_the_sacred";
    private static final String ALTAR_DISPLAY_TAG = "stellarity.altar_of_the_sacred_display";
    private static final long LOCATION_DEDUPE_MS = 1200L;

    private final Map<String, Long> handledLocationsUntil = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("B5.3 TEST shim ativo: fallback fail-closed para CRYING_OBSIDIAN do Altar.");
    }

    @Override
    public void onDisable() {
        handledLocationsUntil.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.CRYING_OBSIDIAN) {
            return;
        }

        ItemStack eventItem = event.getItemInHand();
        if (!isAltarItem(eventItem)) {
            // Crying Obsidian normal nunca é tocada pelo shim.
            getLogger().fine(() -> "Ignorando CRYING_OBSIDIAN sem identidade direta de Altar em "
                    + describe(placed) + " player=" + event.getPlayer().getName());
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

        Player player = event.getPlayer();
        getLogger().info("B5.3 carrier confirmado: player=" + player.getName()
                + " block=" + describe(placed)
                + " eventItem=" + eventItem.getType()
                + " itemModel=" + ALTAR_MODEL);

        // Aguarda um tick para deixar o Stellarity concluir qualquer criação de
        // Marker/ItemDisplay da colocação original. Depois corrige SOMENTE o
        // carrier exato recebido pelo BlockPlaceEvent.
        Bukkit.getScheduler().runTask(this, () -> repairAltarCarrier(player, placed, locationKey));
    }

    private void repairAltarCarrier(Player player, Block block, String locationKey) {
        if (block.getType() != Material.CRYING_OBSIDIAN) {
            getLogger().info("B5.3 skip: carrier já mudou antes da correção: " + describe(block)
                    + " atual=" + block.getType());
            return;
        }

        boolean existingAltarEntity = hasAltarEntityInExactColumn(block);

        // Corrige apenas o carrier. O log histórico provou que, no caminho que
        // falha, Marker/ItemDisplay podem já existir mesmo enquanto o bloco-base
        // permanece CRYING_OBSIDIAN.
        block.setType(Material.CHISELED_QUARTZ_BLOCK, false);

        boolean dispatched = false;
        if (!existingAltarEntity) {
            String dimension = block.getWorld().getKey().toString();
            String command = "execute in " + dimension
                    + " positioned " + block.getX() + " " + block.getY() + " " + block.getZ()
                    + " run function loghorizon:stellarity_safe/altar";
            dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        getLogger().info("B5.3 altar fallback: player=" + player.getName()
                + " carrier=" + locationKey
                + " crying_obsidian->chiseled_quartz"
                + " existingAltarEntity=" + existingAltarEntity
                + " functionDispatched=" + dispatched);
    }

    private boolean hasAltarEntityInExactColumn(Block block) {
        double cx = block.getX() + 0.5D;
        double cy = block.getY() + 1.0D;
        double cz = block.getZ() + 0.5D;

        for (Entity entity : block.getWorld().getNearbyEntities(
                new org.bukkit.Location(block.getWorld(), cx, cy, cz), 0.9D, 1.6D, 0.9D)) {
            int ex = entity.getLocation().getBlockX();
            int ez = entity.getLocation().getBlockZ();
            if (ex != block.getX() || ez != block.getZ()) {
                continue;
            }

            if (entity.getScoreboardTags().contains(ALTAR_TAG)
                    || entity.getScoreboardTags().contains(ALTAR_DISPLAY_TAG)) {
                return true;
            }
        }
        return false;
    }

    /**
     * O Bug Reporter já registrou eventos CRYING_OBSIDIAN cujo eventItem era
     * CHISELED_QUARTZ_BLOCK com item_model=stellarity:altar_of_the_sacred.
     * Lemos a identidade por múltiplas representações para tolerar diferenças
     * entre builds Paper 26.2 sem aceitar um bloco comum por material apenas.
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
