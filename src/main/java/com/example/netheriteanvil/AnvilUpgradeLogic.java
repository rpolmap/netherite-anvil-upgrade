package com.example.netheriteanvil;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Map;

public final class AnvilUpgradeLogic {

    public record Result(ItemStack stack, int cost) {}

    /** Netherite item -> its matching Diamond item. */
    private static final Map<Item, Item> PAIRS = Map.ofEntries(
            Map.entry(Items.NETHERITE_SWORD, Items.DIAMOND_SWORD),
            Map.entry(Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE),
            Map.entry(Items.NETHERITE_AXE, Items.DIAMOND_AXE),
            Map.entry(Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL),
            Map.entry(Items.NETHERITE_HOE, Items.DIAMOND_HOE),
            Map.entry(Items.NETHERITE_SPEAR, Items.DIAMOND_SPEAR),
            Map.entry(Items.NETHERITE_HELMET, Items.DIAMOND_HELMET),
            Map.entry(Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE),
            Map.entry(Items.NETHERITE_LEGGINGS, Items.DIAMOND_LEGGINGS),
            Map.entry(Items.NETHERITE_BOOTS, Items.DIAMOND_BOOTS)
    );

    private AnvilUpgradeLogic() {}

    public static boolean isUpgradePair(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        Item diamond = PAIRS.get(left.getItem());
        return diamond != null && right.is(diamond);
    }

    /** Mirrors vanilla AnvilMenu math for item+item. Returns null if no valid output. */
    public static Result combine(ItemStack left, ItemStack right, String itemName, boolean creative) {
        ItemStack result = left.copy();

        ItemEnchantments rightEnch = right.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(
                left.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));

        int leftPenalty = left.getOrDefault(DataComponents.REPAIR_COST, 0);
        int rightPenalty = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        long penalty = (long) leftPenalty + rightPenalty;

        int added = 0;
        int renameCost = 0;

        // --- Repair: sacrificed item's remaining durability + 12% of result's max ---
        if (result.isDamageableItem()) {
            int max = result.getMaxDamage();
            int leftRemaining = max - left.getDamageValue();
            int rightRemaining = right.getMaxDamage() - right.getDamageValue();
            int newDamage = Math.max(0, max - (leftRemaining + rightRemaining + max * 12 / 100));
            if (newDamage < result.getDamageValue()) {
                result.setDamageValue(newDamage);
                added += 2;
            }
        }

        // --- Enchantment merge (vanilla rules; left slot wins conflicts) ---
        boolean anyApplied = false;
        boolean anyRejected = false;

        for (Holder<Enchantment> holder : new ArrayList<>(rightEnch.keySet())) {
            int leftLevel = merged.getLevel(holder);
            int level = rightEnch.getLevel(holder);
            level = (leftLevel == level) ? level + 1 : Math.max(level, leftLevel);

            Enchantment enchantment = holder.value();
            boolean compatible = enchantment.canEnchant(left) || creative;

            for (Holder<Enchantment> existing : merged.keySet()) {
                if (!existing.equals(holder) && !Enchantment.areCompatible(holder, existing)) {
                    compatible = false;
                    added++; // vanilla: +1 per conflict
                }
            }

            if (!compatible) {
                anyRejected = true;
            } else {
                anyApplied = true;
                if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();
                merged.set(holder, level);
                added += enchantment.getAnvilCost() * level;
            }
        }

        if (anyRejected && !anyApplied && added <= 0) {
            return null;
        }

        // --- Rename (same as vanilla) ---
        if (itemName != null && !itemName.isBlank()) {
            if (!itemName.equals(left.getHoverName().getString())) {
                renameCost = 1;
                added += renameCost;
                result.set(DataComponents.CUSTOM_NAME, Component.literal(itemName));
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            renameCost = 1;
            added += renameCost;
            result.remove(DataComponents.CUSTOM_NAME);
        }

        if (added <= 0) return null; // nothing to repair, merge or rename

        int total = (int) Math.min(Integer.MAX_VALUE, penalty + added);
        if (total >= 40 && !creative) return null; // "Too Expensive!"

        // --- Prior work penalty: max(left, right) * 2 + 1 (not bumped on rename-only) ---
        int newPenalty = Math.max(leftPenalty, rightPenalty);
        if (renameCost != added || renameCost == 0) {
            newPenalty = (int) Math.min(Integer.MAX_VALUE, newPenalty * 2L + 1L);
        }
        result.set(DataComponents.REPAIR_COST, newPenalty);
        result.set(DataComponents.ENCHANTMENTS, merged.toImmutable());

        return new Result(result, total);
    }
}
