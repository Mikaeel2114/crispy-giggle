package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class FastUseCheck extends Check {

    private long minConsumeMs;

    public FastUseCheck(BedwarsACPlugin plugin) {
        super(plugin, "fastuse", "FastUse");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minConsumeMs = plugin.getConfigManager().getCheckLong("fastuse", "min-consume-ms");
    }

    public void checkConsume(Player player, PlayerData data, long now) {
        if (isExempt(player, data)) {
            return;
        }
        long start = data.getItemUseStartMillis();
        if (start <= 0L) {
            return;
        }
        long elapsed = now - start;
        data.setItemUseStartMillis(0L);

        if (elapsed < minConsumeMs) {
            fail(player, data, 1.0D, "Consumed item in " + elapsed + "ms < " + minConsumeMs + "ms");
        } else {
            reward(data);
        }
    }
}
