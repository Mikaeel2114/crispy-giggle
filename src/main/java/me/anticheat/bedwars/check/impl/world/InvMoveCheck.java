package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class InvMoveCheck extends Check {

    private double minWalkSpeed;

    public InvMoveCheck(BedwarsACPlugin plugin) {
        super(plugin, "invmove", "InvMove");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minWalkSpeed = plugin.getConfigManager().getCheckDouble("invmove", "min-walk-speed");
    }

    public void checkInvMove(Player player, PlayerData data) {
        if (isExempt(player, data) || !data.isInventoryOpen()) {
            return;
        }
        if (data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || data.getIceTicks() > 0 || data.getSlimeTicks() > 0
                || data.getLiquidTicks() > 0 || data.getTeleportTicks() > 0) {
            return;
        }

        // Ignore the first 400ms after opening an inventory to allow momentum decay
        if (System.currentTimeMillis() - data.getInventoryOpenMillis() < 400L) {
            return;
        }

        if (data.isServerOnGround() && data.getDeltaXZ() > minWalkSpeed && data.getDeltaXZ() >= data.getLastDeltaXZ()) {
            fail(player, data, 1.0D, String.format("Moving with open GUI speed=%.3f", data.getDeltaXZ()));
        } else {
            reward(data);
        }
    }
}
