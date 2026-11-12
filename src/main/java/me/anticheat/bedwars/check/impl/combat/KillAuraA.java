package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.BoundingBox;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class KillAuraA extends Check {

    private double maxFovAngle;
    private int maxTargetsPerTick;

    public KillAuraA(BedwarsACPlugin plugin) {
        super(plugin, "killaura-a", "KillAura A");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxFovAngle = plugin.getConfigManager().getCheckDouble("killaura-a", "fov-angle");
        this.maxTargetsPerTick = plugin.getConfigManager().getCheckInt("killaura-a", "max-targets-per-tick");
    }

    public void checkCombat(Player attacker, PlayerData data, Entity target) {
        if (isExempt(attacker, data) || target == null) {
            return;
        }

        // 1. Multi-Aura check: hitting multiple distinct entities in the exact same client tick
        data.getAttackedEntitiesThisTick().add(target.getEntityId());
        int targetsHit = data.getAttackedEntitiesThisTick().size();
        if (targetsHit > maxTargetsPerTick) {
            fail(attacker, data, 2.0D, "MultiAura targets=" + targetsHit + " in 1 tick");
            return;
        }

        // 2. FOV Angle check: angle between attacker's eye look vector and vector to target hitbox center
        Location eyeLoc = attacker.getEyeLocation();
        BoundingBox targetBox = BoundingBox.fromEntity(target);
        Vector toTarget = targetBox.getCenter().subtract(eyeLoc.toVector());
        toTarget.setY(0.0D);

        Vector lookDir = eyeLoc.getDirection().clone();
        lookDir.setY(0.0D);

        if (toTarget.lengthSquared() > 0.25D && lookDir.lengthSquared() > 0.001D) {
            double dot = lookDir.normalize().dot(toTarget.normalize());
            dot = Math.max(-1.0D, Math.min(1.0D, dot));
            double angleDeg = Math.toDegrees(Math.acos(dot));

            if (angleDeg > maxFovAngle) {
                fail(attacker, data, 1.0D, String.format("FOV angle=%.1f > %.1f", angleDeg, maxFovAngle));
                return;
            }
        }

        reward(data);
    }
}
