package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FastBreakCheck extends Check {

    private double speedRatioThreshold;

    public FastBreakCheck(BedwarsACPlugin plugin) {
        super(plugin, "fastbreak", "FastBreak");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.speedRatioThreshold = plugin.getConfigManager().getCheckDouble("fastbreak", "speed-Ratio-threshold");
    }

    public void checkBlockBreak(Player player, PlayerData data, Block block) {
        if (isExempt(player, data) || block == null) {
            return;
        }

        Material mat = block.getType();
        long expectedMinMillis = estimateVanillaBreakTimeMillis(player, mat);
        if (expectedMinMillis <= 100L) {
            return;
        }

        long started = data.getBlockBreakStartMillis();
        long now = System.currentTimeMillis();
        if (started <= 0L) {
            return;
        }

        long actualElapsed = now - started;
        long thresholdMillis = (long) (expectedMinMillis * speedRatioThreshold);

        if (actualElapsed < thresholdMillis) {
            fail(player, data, 1.0D, String.format("%s broke in %dms < %dms", mat.name(), actualElapsed, thresholdMillis));
        } else {
            reward(data);
        }
    }

    private long estimateVanillaBreakTimeMillis(Player player, Material mat) {
        double hardness;
        String name = mat.name();
        if (mat == Material.OBSIDIAN) {
            hardness = 50.0D;
        } else if (mat == Material.ENDER_STONE) {
            hardness = 3.0D;
        } else if (name.contains("WOOD") || name.contains("LOG") || mat == Material.WOOD) {
            hardness = 2.0D;
        } else if (mat == Material.WOOL) {
            hardness = 0.8D;
        } else if (mat == Material.STAINED_CLAY || mat == Material.HARD_CLAY) {
            hardness = 1.25D;
        } else if (mat == Material.BED_BLOCK) {
            hardness = 0.2D;
        } else {
            return 0L;
        }

        ItemStack held = player.getItemInHand();
        double toolMultiplier = getToolSpeedMultiplier(held, mat);
        if (held != null && toolMultiplier > 1.0D && held.containsEnchantment(Enchantment.DIG_SPEED)) {
            int effLevel = held.getEnchantmentLevel(Enchantment.DIG_SPEED);
            if (effLevel > 0) {
                toolMultiplier += (effLevel * effLevel + 1);
            }
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.FAST_DIGGING)) {
                toolMultiplier *= (1.0D + (effect.getAmplifier() + 1) * 0.20D);
            }
        }

        boolean canHarvest = canHarvestBlock(held, mat);
        double damagePerTick = (toolMultiplier / hardness) / (canHarvest ? 30.0D : 100.0D);
        if (damagePerTick >= 1.0D) {
            return 0L;
        }
        int ticks = (int) Math.ceil(1.0D / damagePerTick);
        return ticks * 50L;
    }

    private double getToolSpeedMultiplier(ItemStack item, Material blockMat) {
        if (item == null) {
            return 1.0D;
        }
        String itemName = item.getType().name();
        if (blockMat == Material.WOOL && item.getType() == Material.SHEARS) {
            return 5.0D;
        }
        boolean isPickaxeBlock = blockMat == Material.OBSIDIAN || blockMat == Material.ENDER_STONE
                || blockMat == Material.STAINED_CLAY || blockMat == Material.HARD_CLAY;
        boolean isAxeBlock = blockMat.name().contains("WOOD") || blockMat == Material.WOOD;

        if ((isPickaxeBlock && itemName.endsWith("_PICKAXE")) || (isAxeBlock && itemName.endsWith("_AXE"))) {
            if (itemName.startsWith("WOOD_")) return 2.0D;
            if (itemName.startsWith("STONE_")) return 4.0D;
            if (itemName.startsWith("IRON_")) return 6.0D;
            if (itemName.startsWith("DIAMOND_")) return 8.0D;
            if (itemName.startsWith("GOLD_")) return 12.0D;
        }
        return 1.0D;
    }

    private boolean canHarvestBlock(ItemStack item, Material blockMat) {
        if (blockMat == Material.OBSIDIAN) {
            return item != null && item.getType() == Material.DIAMOND_PICKAXE;
        }
        if (blockMat == Material.ENDER_STONE || blockMat == Material.STAINED_CLAY || blockMat == Material.HARD_CLAY) {
            return item != null && item.getType().name().endsWith("_PICKAXE");
        }
        return true;
    }
}
