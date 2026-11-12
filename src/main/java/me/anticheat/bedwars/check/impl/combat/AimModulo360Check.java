package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Ported from Grim 2.0 AimModulo360: detects clients that modulo-360 their rotation internally.
 */
public class AimModulo360Check extends Check {

    public AimModulo360Check(BedwarsACPlugin plugin) {
        super(plugin, "aim-modulo360", "AimModulo360");
    }

    public void checkRotation(Player player, PlayerData data) {
        if (isExempt(player, data) || data.getTeleportTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        float yaw = player.getLocation().getYaw();
        float rawDeltaYaw = Math.abs(data.getRawDeltaYaw());
        float lastRawDeltaYaw = Math.abs(data.getLastRawDeltaYaw());

        if (yaw < 360.0F && yaw > -360.0F && rawDeltaYaw > 320.0F && lastRawDeltaYaw < 30.0F) {
            fail(player, data, 1.0D, String.format("Modulo360 rawDeltaYaw=%.1f last=%.1f", rawDeltaYaw, lastRawDeltaYaw));
        } else {
            reward(data);
        }
    }
}
