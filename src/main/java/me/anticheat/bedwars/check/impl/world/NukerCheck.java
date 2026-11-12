package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;

public class NukerCheck extends Check {

    private int maxBreaksPerSecond;
    private double maxAngle;

    public NukerCheck(BedwarsACPlugin plugin) {
        super(plugin, "nuker", "Nuker");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxBreaksPerSecond = plugin.getConfigManager().getCheckInt("nuker", "max-breaks-per-second");
        this.maxAngle = plugin.getConfigManager().getCheckDouble("nuker", "max-angle");
    }

    public void checkNuker(Player player, PlayerData data, Block block) {
        if (isExempt(player, data) || block == null) {
            return;
        }

        long now = System.currentTimeMillis();
        Deque<Long> breaks = data.getBlockBreakTimestamps();
        int count;
        synchronized (breaks) {
            breaks.addLast(now);
            while (!breaks.isEmpty() && (now - breaks.peekFirst() > 1000L)) {
                breaks.removeFirst();
            }
            count = breaks.size();
        }

        if (count > maxBreaksPerSecond) {
            fail(player, data, 1.5D, "BreakRate bps=" + count + " > " + maxBreaksPerSecond);
            return;
        }

        Vector eye = player.getEyeLocation().toVector();
        Vector blockCenter = block.getLocation().toVector().add(new Vector(0.5D, 0.5D, 0.5D));
        Vector toBlock = blockCenter.subtract(eye);
        if (toBlock.lengthSquared() > 1.0D) {
            Vector look = player.getEyeLocation().getDirection().normalize();
            double dot = Math.max(-1.0D, Math.min(1.0D, look.dot(toBlock.normalize())));
            double angle = Math.toDegrees(Math.acos(dot));
            if (angle > maxAngle) {
                fail(player, data, 1.0D, String.format("OutOfFOV angle=%.1f > %.1f", angle, maxAngle));
                return;
            }
        }

        reward(data);
    }
}
