package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class NoFallCheck extends Check {

    private double minFallDistance;

    public NoFallCheck(BedwarsACPlugin plugin) {
        super(plugin, "nofall", "NoFall");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minFallDistance = plugin.getConfigManager().getCheckDouble("nofall", "min-fall-distance");
    }

    public void checkNoFall(Player player, PlayerData data, boolean packetOnGround) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (data.getLiquidTicks() > 0 || data.getClimbableTicks() > 0 || data.getWebTicks() > 0
                || data.getSlimeTicks() > 0 || data.getTeleportTicks() > 0
                || data.getExplosionExemptTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        // Client claims onGround = true while server-side block collision shows player is in mid-air
        if (packetOnGround && !data.isServerOnGround() && !data.isLastServerOnGround()
                && data.getFallDistance() >= minFallDistance) {
            fail(player, data, 1.0D, String.format("GroundSpoof fallDist=%.2f airTicks=%d", data.getFallDistance(), data.getAirTicks()));
        } else {
            reward(data);
        }
    }
}
