package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

public class BedThroughWallCheck extends Check {

    private double rayStep;
    private double maxReach;

    public BedThroughWallCheck(BedwarsACPlugin plugin) {
        super(plugin, "bed-through-wall", "BedThroughWall");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.rayStep = plugin.getConfigManager().getCheckDouble("bed-through-wall", "ray-step");
        this.maxReach = plugin.getConfigManager().getCheckDouble("bed-through-wall", "max-reach");
    }

    public void checkBedBreak(Player player, PlayerData data, BlockBreakEvent event) {
        Block broken = event.getBlock();
        if (broken == null || broken.getType() != Material.BED_BLOCK) {
            return;
        }
        if (isExempt(player, data)) {
            return;
        }

        Location eyeLoc = player.getEyeLocation();
        Vector eyeVec = eyeLoc.toVector();
        Location bedBase = broken.getLocation();

        double dist = eyeVec.distance(bedBase.toVector().add(new Vector(0.5D, 0.28D, 0.5D)));
        if (dist > maxReach) {
            event.setCancelled(true);
            fail(player, data, 1.0D, String.format("BedReach dist=%.2f > %.2f", dist, maxReach));
            return;
        }

        // Check multi-point line-of-sight rays from player's eyes to points across the bed hitbox (and adjacent bed half)
        if (!hasLineOfSightToBed(eyeLoc.getWorld(), eyeVec, broken)) {
            event.setCancelled(true);
            fail(player, data, 1.0D, "Broke BED_BLOCK through solid wall with zero line-of-sight");
        } else {
            reward(data);
        }
    }

    private boolean hasLineOfSightToBed(World world, Vector eye, Block bedBlock) {
        // Test 9 sample points on the broken bed block + any adjacent BED_BLOCK half
        double[] offsetsX = new double[]{0.15D, 0.5D, 0.85D};
        double[] offsetsY = new double[]{0.1D, 0.50D};
        double[] offsetsZ = new double[]{0.15D, 0.5D, 0.85D};

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 1) {
                    continue;
                }
                Block candidate = bedBlock.getRelative(dx, 0, dz);
                if (candidate.getType() != Material.BED_BLOCK) {
                    continue;
                }
                for (double ox : offsetsX) {
                    for (double oy : offsetsY) {
                        for (double oz : offsetsZ) {
                            Vector targetPoint = new Vector(
                                    candidate.getX() + ox,
                                    candidate.getY() + oy,
                                    candidate.getZ() + oz
                            );
                            if (isRayClear(world, eye, targetPoint, bedBlock, candidate)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isRayClear(World world, Vector start, Vector end, Block bedPartA, Block bedPartB) {
        Vector diff = end.clone().subtract(start);
        double totalLength = diff.length();
        if (totalLength < 0.2D) {
            return true;
        }
        Vector stepVec = diff.normalize().multiply(Math.max(0.05D, rayStep));
        double traveled = 0.1D;
        Vector current = start.clone().add(stepVec);

        while (traveled < (totalLength - 0.15D)) {
            Block stepBlock = world.getBlockAt(current.getBlockX(), current.getBlockY(), current.getBlockZ());
            if (!stepBlock.equals(bedPartA) && !stepBlock.equals(bedPartB)) {
                Material mat = stepBlock.getType();
                if (mat != Material.AIR && mat != Material.BED_BLOCK && mat.isSolid()) {
                    return false;
                }
            }
            current.add(stepVec);
            traveled += rayStep;
        }
        return true;
    }
}
