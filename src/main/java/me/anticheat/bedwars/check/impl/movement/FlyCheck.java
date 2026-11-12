package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class FlyCheck extends Check {

    private int maxAirTicks;
    private double gravityDrag;
    private double gravityDrop;
    private double hoverTolerance;

    public FlyCheck(BedwarsACPlugin plugin) {
        super(plugin, "fly", "Fly");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxAirTicks = plugin.getConfigManager().getCheckInt("fly", "max-air-ticks");
        this.gravityDrag = plugin.getConfigManager().getCheckDouble("fly", "gravity-drag");
        this.gravityDrop = plugin.getConfigManager().getCheckDouble("fly", "gravity-drop");
        this.hoverTolerance = plugin.getConfigManager().getCheckDouble("fly", "hover-tolerance");
    }

    public void checkFly(Player player, PlayerData data) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }

        if (data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || data.getTeleportTicks() > 0 || data.getLiquidTicks() > 0
                || data.getClimbableTicks() > 0 || data.getWebTicks() > 0
                || data.getSlimeTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        int airTicks = data.getAirTicks();
        double deltaY = data.getDeltaY();
        double lastDeltaY = data.getLastDeltaY();

        // 1. Airtime > maxAirTicks (10) without a Y decrease and no velocity
        if (airTicks > maxAirTicks && deltaY >= -hoverTolerance) {
            fail(player, data, 1.0D, String.format("Hover/Ascend airTicks=%d deltaY=%.4f", airTicks, deltaY));
            return;
        }

        // 2. Gravity envelope verification in mid-air after jump peak
        if (airTicks > (maxAirTicks + 4)) {
            double predictedDeltaY = (lastDeltaY - gravityDrop) * gravityDrag;
            double diff = deltaY - predictedDeltaY;
            if (diff > 0.085D) {
                fail(player, data, 1.0D, String.format("Glide deltaY=%.3f pred=%.3f airTicks=%d", deltaY, predictedDeltaY, airTicks));
                return;
            }
        }

        reward(data);
    }
}
