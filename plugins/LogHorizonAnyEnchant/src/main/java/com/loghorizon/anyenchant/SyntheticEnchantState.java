package com.loghorizon.anyenchant;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SyntheticEnchantState {
    private SyntheticEnchantState() {
    }

    static Map<Enchantment, Integer> originalEnchantments(ItemStack item, NamespacedKey stateKey) {
        Map<Enchantment, Integer> result = new HashMap<>(item.getEnchantments());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return result;
        }

        for (Map.Entry<String, Integer> entry : decode(meta.getPersistentDataContainer(), stateKey).entrySet()) {
            NamespacedKey key = NamespacedKey.fromString(entry.getKey());
            if (key == null) {
                continue;
            }
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            if (enchantment == null) {
                continue;
            }
            if (entry.getValue() <= 0) {
                result.remove(enchantment);
            } else {
                result.put(enchantment, entry.getValue());
            }
        }
        return result;
    }

    static boolean reconcile(ItemStack item,
                             Map<Enchantment, Integer> desired,
                             NamespacedKey stateKey) {
        ItemMeta currentMeta = item.getItemMeta();
        if (currentMeta == null) {
            return false;
        }

        Map<Enchantment, Integer> originals = originalEnchantments(item, stateKey);
        Map<Enchantment, Integer> desiredSynthetic = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : desired.entrySet()) {
            int original = originals.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > original) {
                desiredSynthetic.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Integer> currentState = decode(currentMeta.getPersistentDataContainer(), stateKey);
        if (matchesCurrent(item, originals, desiredSynthetic, currentState)) {
            return false;
        }

        restore(item, stateKey);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        Map<String, Integer> newState = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : desiredSynthetic.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int original = meta.getEnchantLevel(enchantment);
            newState.put(enchantment.getKey().toString(), original);
            meta.addEnchant(enchantment, entry.getValue(), true);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (newState.isEmpty()) {
            pdc.remove(stateKey);
        } else {
            pdc.set(stateKey, PersistentDataType.STRING, encode(newState));
        }
        item.setItemMeta(meta);
        return true;
    }

    static boolean restore(ItemStack item, NamespacedKey stateKey) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Map<String, Integer> state = decode(meta.getPersistentDataContainer(), stateKey);
        if (state.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Integer> entry : state.entrySet()) {
            NamespacedKey key = NamespacedKey.fromString(entry.getKey());
            if (key == null) {
                continue;
            }
            Enchantment enchantment = Registry.ENCHANTMENT.get(key);
            if (enchantment == null) {
                continue;
            }
            meta.removeEnchant(enchantment);
            if (entry.getValue() > 0) {
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        meta.getPersistentDataContainer().remove(stateKey);
        item.setItemMeta(meta);
        return true;
    }

    private static boolean matchesCurrent(ItemStack item,
                                          Map<Enchantment, Integer> originals,
                                          Map<Enchantment, Integer> desiredSynthetic,
                                          Map<String, Integer> currentState) {
        if (currentState.size() != desiredSynthetic.size()) {
            return false;
        }
        for (Map.Entry<Enchantment, Integer> entry : desiredSynthetic.entrySet()) {
            String key = entry.getKey().getKey().toString();
            Integer recordedOriginal = currentState.get(key);
            if (recordedOriginal == null || recordedOriginal != originals.getOrDefault(entry.getKey(), 0)) {
                return false;
            }
            if (item.getEnchantmentLevel(entry.getKey()) != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static String encode(Map<String, Integer> values) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : entries) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static Map<String, Integer> decode(PersistentDataContainer pdc, NamespacedKey stateKey) {
        String raw = pdc.get(stateKey, PersistentDataType.STRING);
        Map<String, Integer> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(";")) {
            int separator = part.lastIndexOf('=');
            if (separator <= 0 || separator == part.length() - 1) {
                continue;
            }
            try {
                result.put(part.substring(0, separator), Integer.parseInt(part.substring(separator + 1)));
            } catch (NumberFormatException ignored) {
                // Entrada antiga/corrompida: é ignorada sem impedir a restauração das demais.
            }
        }
        return result;
    }
}
