package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class JesusCheck extends Check {

    private double maxLiquidSpeed;

    public JesusCheck(BedwarsACPlugin plugin) {
        super(plugin, "jesus", "Jesus");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxLiquidSpeed = plugin.getConfigManager().getCheckDouble("jesus", "max-liquid-speed");
    }

    public void checkJesus(Player player, PlayerData data) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || data.getTeleportTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        Location loc = data.getCurrentLocation();
        Block below = loc.clone().add(0.0D, -0.1D, 0.0D).getBlock();
        Block feet = loc.getBlock();

        if (!below.isLiquid() || data.isServerOnGround()) {
            reward(data);
            return;
        }

        // Ensure no lily pad or solid block nearby
        for (double ox = -0.3D; ox <= 0.3D; ox += 0.3D) {
            for (double oz = -0.3D; oz <= 0.3D; oz += 0.3D) {
                Material m1 = loc.clone().add(ox, -0.1D, oz).getBlock().getType();
                Material m2 = loc.clone().add(ox, 0.0D, oz).getBlock().getType();
                if (m1.isSolid() || m2 == Material.WATER_LILY || m1 == Material.WATER_LILY) {
                    return;
                }
            }
        }

        boolean flatSurfaceWalk = !feet.isLiquid() && Math.abs(data.getDeltaY()) < 0.001D && data.getDeltaXZ() > 0.12D;
        boolean fastLiquidSkim = data.getDeltaXZ() > maxLiquidSpeed && Math.abs(data.getDeltaY()) < 0.02D;

        if (flatSurfaceWalk || fastLiquidSkim) {
            fail(player, data, 1.0D, String.format("WaterWalk xz=%.3f dy=%.4f", data.getDeltaXZ(), data.getDeltaY()));
        } else {
            reward(data);
        }
    }
}
