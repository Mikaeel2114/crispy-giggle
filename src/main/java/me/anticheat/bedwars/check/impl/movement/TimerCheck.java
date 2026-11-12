package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import me.anticheat.bedwars.util.TPSUtil;
import org.bukkit.entity.Player;

import java.util.Deque;

public class TimerCheck extends Check {

    private double minTimerTps;
    private int sampleSize;
    private double expectedTickMs;
    private double maxSpeedMultiplier;

    public TimerCheck(BedwarsACPlugin plugin) {
        super(plugin, "timer", "Timer");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minTimerTps = plugin.getConfigManager().getCheckDouble("timer", "min-tps");
        this.sampleSize = plugin.getConfigManager().getCheckInt("timer", "sample-size");
        this.expectedTickMs = plugin.getConfigManager().getCheckDouble("timer", "expected-tick-ms");
        this.maxSpeedMultiplier = plugin.getConfigManager().getCheckDouble("timer", "max-speed-multiplier");
    }

    public void checkTimer(Player player, PlayerData data, long now) {
        if (isExempt(player, data)) {
            return;
        }
        // Strictly exempt when TPS < 19.5 (or configured timer.min-tps)
        if (TPSUtil.getTPS() < minTimerTps) {
            return;
        }
        if (data.getTeleportTicks() > 0) {
            data.getFlyingSamples().clear();
            return;
        }

        Deque<Long> samples = data.getFlyingSamples();
        synchronized (samples) {
            samples.addLast(now);
            if (samples.size() < sampleSize) {
                return;
            }

            long first = samples.peekFirst();
            long elapsed = now - first;
            samples.clear();

            if (elapsed <= 0L) {
                return;
            }

            double averageTickMs = elapsed / (double) (sampleSize - 1);
            double timerMultiplier = expectedTickMs / averageTickMs;

            if (timerMultiplier > maxSpeedMultiplier) {
                fail(player, data, 1.0D, String.format("timer=%.2fx (avg=%.1fms)", timerMultiplier, averageTickMs));
            } else {
                reward(data);
            }
        }
    }
}
