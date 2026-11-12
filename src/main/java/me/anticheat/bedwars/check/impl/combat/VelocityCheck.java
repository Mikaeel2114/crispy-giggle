package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class VelocityCheck extends Check {

    private double minPercent;
    private int requiredStreak;
    private double minExpectedVy;

    public VelocityCheck(BedwarsACPlugin plugin) {
        super(plugin, "velocity", "Velocity");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minPercent = plugin.getConfigManager().getCheckDouble("velocity", "min-percent");
        this.requiredStreak = plugin.getConfigManager().getCheckInt("velocity", "required-streak");
        this.minExpectedVy = plugin.getConfigManager().getCheckDouble("velocity", "min-expected-vy");
    }

    public void checkVelocity(Player player, PlayerData data) {
        if (isExempt(player, data) || !data.isWaitingForVelocityResponse()) {
            return;
        }

        if (data.getExplosionExemptTicks() > 0 || data.getLiquidTicks() > 0
                || data.getClimbableTicks() > 0 || data.getWebTicks() > 0
                || data.getUnderBlockTicks() > 0 || data.getTeleportTicks() > 0) {
            data.setWaitingForVelocityResponse(false);
            return;
        }

        long elapsed = System.currentTimeMillis() - data.getVelocityReceivedMillis();
        if (elapsed > 350L) {
            data.setWaitingForVelocityResponse(false);
            return;
        }

        double expectedVy = data.getPendingVelocityY();
        double expectedHorizontal = data.getPendingVelocityHorizontal();

        if (expectedVy < minExpectedVy && expectedHorizontal < minExpectedVy) {
            data.setWaitingForVelocityResponse(false);
            return;
        }

        double actualVy = data.getDeltaY();
        double actualHorizontal = data.getDeltaXZ();

        double verticalRatio = expectedVy > minExpectedVy ? (actualVy / expectedVy) : 1.0D;
        double horizontalRatio = expectedHorizontal > minExpectedVy ? (actualHorizontal / expectedHorizontal) : 1.0D;
        double bestRatio = Math.max(verticalRatio, horizontalRatio);

        data.setWaitingForVelocityResponse(false);

        if (bestRatio < minPercent) {
            int streak = data.getLowVelocityStreak() + 1;
            data.setLowVelocityStreak(streak);
            if (streak >= requiredStreak) {
                fail(player, data, 1.5D, String.format("kbRatio=%.0f%% < %.0f%% (streak=%d)", bestRatio * 100.0D, minPercent * 100.0D, streak));
            }
        } else {
            data.setLowVelocityStreak(Math.max(0, data.getLowVelocityStreak() - 1));
            reward(data);
        }
    }
}
