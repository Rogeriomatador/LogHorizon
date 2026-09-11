package com.loghorizon.stellarityaltarbridge;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class LogHorizonStellarityAltarBridge extends JavaPlugin implements Listener {
    private static final NamespacedKey ALTAR_MODEL = Objects.requireNonNull(
        NamespacedKey.fromString("stellarity:block/altar_of_the_sacred")
    );

    private final Set<String> scheduled = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("B5.3-DIAG1 enabled: exact Crying Obsidian fallback for Stellarity Altar only.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        final Block placed = event.getBlockPlaced();
        if (placed.getType() != Material.CRYING_OBSIDIAN) {
            return;
        }

        final ItemStack item = event.getItemInHand();
        final NamespacedKey model = readItemModel(item);

        if (!ALTAR_MODEL.equals(model)) {
            getLogger().info(
                "B5.3 observed CRYING_OBSIDIAN but ignored: player=" + event.getPlayer().getName()
                    + " pos=" + format(placed)
                    + " itemModel=" + (model == null ? "<none>" : model)
            );
            return;
        }

        final String key = placed.getWorld().getUID() + ":" + placed.getX() + ":" + placed.getY() + ":" + placed.getZ();
        if (!scheduled.add(key)) {
            return;
        }

        getLogger().info(
            "B5.3 accepted Altar CRYING_OBSIDIAN carrier: player=" + event.getPlayer().getName()
                + " pos=" + format(placed)
                + " itemModel=" + model
        );

        Bukkit.getScheduler().runTask(this, () -> {
            try {
                final Block target = placed.getWorld().getBlockAt(placed.getX(), placed.getY(), placed.getZ());
                if (target.getType() != Material.CRYING_OBSIDIAN) {
                    getLogger().info(
                        "B5.3 skipped delayed repair because carrier changed before repair: pos=" + format(target)
                            + " current=" + target.getType()
                    );
                    return;
                }

                target.setType(Material.CHISELED_QUARTZ_BLOCK, false);

                final String command = "execute in " + target.getWorld().getKey()
                    + " positioned " + target.getX() + " " + target.getY() + " " + target.getZ()
                    + " run function loghorizon:stellarity_safe/altar";

                final CommandSender silent = getServer().createCommandSender(component -> { });
                final boolean dispatched = getServer().dispatchCommand(silent, command);

                getLogger().info(
                    "B5.3 repaired Altar carrier at " + format(target)
                        + " functionDispatched=" + dispatched
                );
            } catch (Throwable t) {
                getLogger().log(java.util.logging.Level.SEVERE, "B5.3 altar fallback failed", t);
            } finally {
                scheduled.remove(key);
            }
        });
    }

    private NamespacedKey readItemModel(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return null;
        }
        final ItemMeta meta = item.getItemMeta();
        return meta == null ? null : meta.getItemModel();
    }

    private String format(Block block) {
        return block.getWorld().getKey() + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
