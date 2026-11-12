package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Deque;

public class AutoClickerB extends Check {

    private int sampleSize;
    private double minStdDev;
    private long maxClickIntervalMs;

    public AutoClickerB(BedwarsACPlugin plugin) {
        super(plugin, "autoclicker-b", "AutoClicker B");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.sampleSize = plugin.getConfigManager().getCheckInt("autoclicker-b", "sample-size");
        this.minStdDev = plugin.getConfigManager().getCheckDouble("autoclicker-b", "min-std-dev");
        this.maxClickIntervalMs = plugin.getConfigManager().getCheckLong("autoclicker-b", "max-click-interval-ms");
    }

    public void checkConsistency(Player player, PlayerData data, long now) {
        if (data.isDiggingBlock() || (now - data.getLastDigPacketMillis() < 200L)
                || (now - data.getLastBlockPlaceMillis() < 60L)) {
            return;
        }

        long lastClick = data.getLastArmAnimationMillis();
        data.setLastArmAnimationMillis(now);
        if (lastClick <= 0L) {
            return;
        }

        long delta = now - lastClick;
        if (delta <= 0L || delta > maxClickIntervalMs) {
            return;
        }

        Deque<Long> intervals = data.getClickIntervals();
        synchronized (intervals) {
            intervals.addLast(delta);
            if (intervals.size() < sampleSize) {
                return;
            }

            double sum = 0.0D;
            for (Long val : intervals) {
                sum += val;
            }
            double mean = sum / intervals.size();

            double varianceSum = 0.0D;
            for (Long val : intervals) {
                double diff = val - mean;
                varianceSum += (diff * diff);
            }
            double stdDev = Math.sqrt(varianceSum / intervals.size());
            data.setLastStdDevMs(stdDev);
            intervals.clear();

            if (isExempt(player, data)) {
                return;
            }

            // Only flag robotic consistency when average click rate is >= 12 CPS (mean <= 83ms)
            if (mean <= 83.0D && stdDev < minStdDev) {
                fail(player, data, 1.0D, String.format("stdDev=%.2fms < %.2fms (mean=%.1fms)", stdDev, minStdDev, mean));
            } else {
                reward(data);
            }
        }
    }
}
