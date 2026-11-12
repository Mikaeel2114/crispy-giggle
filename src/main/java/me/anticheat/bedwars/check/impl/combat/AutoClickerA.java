package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class AutoClickerA extends Check {

    private int maxCps;
    private int windowSeconds;

    public AutoClickerA(BedwarsACPlugin plugin) {
        super(plugin, "autoclicker-a", "AutoClicker A");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxCps = plugin.getConfigManager().getCheckInt("autoclicker-a", "max-cps");
        this.windowSeconds = Math.max(1, plugin.getConfigManager().getCheckInt("autoclicker-a", "window-seconds"));
    }

    public void checkClick(Player player, PlayerData data, long now) {
        if (data.isDiggingBlock() || (now - data.getLastDigPacketMillis() < 200L)
                || (now - data.getLastBlockPlaceMillis() < 60L)) {
            return;
        }

        data.recordClick(now);

        if (isExempt(player, data)) {
            return;
        }

        double sustainedCps = data.getSustainedCps(windowSeconds);
        if (sustainedCps > maxCps) {
            fail(player, data, 1.0D, String.format("sustained CPS=%.1f > %d over %ds", sustainedCps, maxCps, windowSeconds));
        } else {
            reward(data);
        }
    }
}
