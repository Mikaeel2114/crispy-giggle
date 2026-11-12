package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.BoundingBox;
import me.anticheat.bedwars.util.TPSUtil;
import me.anticheat.bedwars.util.TimedLocation;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class ReachCheck extends Check {

    private double maxReach;
    private double highPingReach;
    private int highPingThreshold;
    private double hitboxExpand;
    private long backtrackWindowMs;

    public ReachCheck(BedwarsACPlugin plugin) {
        super(plugin, "reach", "Reach");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxReach = plugin.getConfigManager().getCheckDouble("reach", "max-reach");
        this.highPingReach = plugin.getConfigManager().getCheckDouble("reach", "high-ping-reach");
        this.highPingThreshold = plugin.getConfigManager().getCheckInt("reach", "high-ping-threshold");
        this.hitboxExpand = plugin.getConfigManager().getCheckDouble("reach", "hitbox-expand");
        this.backtrackWindowMs = plugin.getConfigManager().getCheckLong("reach", "backtrack-window-ms");
    }

    public void checkReach(Player attacker, PlayerData attackerData, Entity targetEntity) {
        if (isExempt(attacker, attackerData) || !(targetEntity instanceof Player)) {
            return;
        }
        Player target = (Player) targetEntity;
        PlayerData targetData = plugin.getPlayerDataManager().get(target);
        if (targetData == null) {
            return;
        }

        int ping = TPSUtil.getPing(attacker);
        double allowedReach = ping > highPingThreshold ? highPingReach : maxReach;

        Location eyeLocation = attacker.getEyeLocation();
        Vector eyePos = eyeLocation.toVector();
        Vector lookDir = eyeLocation.getDirection();

        long rewindTimestamp = System.currentTimeMillis() - ping - 50L;
        List<TimedLocation> historicalLocations = targetData.getLocationsNearTimestamp(rewindTimestamp, backtrackWindowMs);

        double bestDistance = Double.MAX_VALUE;

        for (TimedLocation timedLoc : historicalLocations) {
            BoundingBox box = timedLoc.getBoundingBox().expand(hitboxExpand);
            double rayDist = box.rayTraceDistance(eyePos, lookDir, 6.0D);
            double pointDist = box.distanceToPoint(eyePos);
            double candidate = Math.min(rayDist, pointDist);
            if (candidate < bestDistance) {
                bestDistance = candidate;
            }
        }

        // Also check current target location in case of low latency
        BoundingBox currentBox = BoundingBox.fromPlayerLocation(target.getLocation()).expand(hitboxExpand);
        double currentDist = Math.min(
                currentBox.rayTraceDistance(eyePos, lookDir, 6.0D),
                currentBox.distanceToPoint(eyePos)
        );
        bestDistance = Math.min(bestDistance, currentDist);

        if (bestDistance != Double.MAX_VALUE && bestDistance > allowedReach) {
            fail(attacker, attackerData, 1.0D, String.format("dist=%.2f > max=%.2f (ping=%dms)", bestDistance, allowedReach, ping));
        } else {
            reward(attackerData);
        }
    }
}
