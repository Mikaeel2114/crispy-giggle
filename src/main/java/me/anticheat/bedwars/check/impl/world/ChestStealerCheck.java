package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

import java.util.Deque;

public class ChestStealerCheck extends Check {

    private long minClickIntervalMs;
    private int sampleSize;

    public ChestStealerCheck(BedwarsACPlugin plugin) {
        super(plugin, "cheststealer", "ChestStealer");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minClickIntervalMs = plugin.getConfigManager().getCheckLong("cheststealer", "min-click-interval-ms");
        this.sampleSize = plugin.getConfigManager().getCheckInt("cheststealer", "sample-size");
    }

    public void checkWindowClick(Player player, PlayerData data, int slot, long now) {
        if (isExempt(player, data) || slot < 0 || slot == data.getLastChestSlot()) {
            return;
        }
        data.setLastChestSlot(slot);

        Deque<Long> clicks = data.getChestClickTimestamps();
        synchronized (clicks) {
            clicks.addLast(now);
            if (clicks.size() < sampleSize) {
                return;
            }

            long first = clicks.peekFirst();
            long totalElapsed = now - first;
            clicks.clear();

            long avgInterval = totalElapsed / Math.max(1, sampleSize - 1);
            if (avgInterval < minClickIntervalMs) {
                fail(player, data, 1.0D, "ChestClick avgInterval=" + avgInterval + "ms < " + minClickIntervalMs + "ms");
            } else {
                reward(data);
            }
        }
    }
}
