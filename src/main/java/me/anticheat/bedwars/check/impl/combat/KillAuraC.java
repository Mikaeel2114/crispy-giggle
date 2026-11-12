package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class KillAuraC extends Check {

    private double yawSnapThreshold;
    private double minTargetDistance;

    public KillAuraC(BedwarsACPlugin plugin) {
        super(plugin, "killaura-c", "KillAura C");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.yawSnapThreshold = plugin.getConfigManager().getCheckDouble("killaura-c", "yaw-snap-threshold");
        this.minTargetDistance = plugin.getConfigManager().getCheckDouble("killaura-c", "min-target-distance");
    }

    public void checkAimSnap(Player attacker, PlayerData data, Entity target) {
        if (isExempt(attacker, data) || target == null) {
            return;
        }

        double distSq = attacker.getLocation().distanceSquared(target.getLocation());
        if (distSq < (minTargetDistance * minTargetDistance)) {
            return;
        }

        float deltaYaw = data.getDeltaYaw();
        float lastDeltaYaw = data.getLastDeltaYaw();

        // Impossible rotation snap: yaw delta > 100 degrees in a single tick immediately followed by an attack hit
        if (deltaYaw > yawSnapThreshold && lastDeltaYaw < (yawSnapThreshold * 0.35D)) {
            fail(attacker, data, 1.0D, String.format("Snap yawDelta=%.1f prevYawDelta=%.1f", deltaYaw, lastDeltaYaw));
        } else {
            reward(data);
        }
    }
}
