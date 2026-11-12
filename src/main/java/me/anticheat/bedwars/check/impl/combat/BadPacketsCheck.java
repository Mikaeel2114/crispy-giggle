package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Ported from Grim 2.0 BadPackets (Pitch > 90 degrees, SelfInteract).
 */
public class BadPacketsCheck extends Check {

    public BadPacketsCheck(BedwarsACPlugin plugin) {
        super(plugin, "badpackets", "BadPackets");
    }

    public void checkPitch(Player player, PlayerData data) {
        if (isExempt(player, data) || data.getTeleportTicks() > 0) {
            return;
        }
        float pitch = Math.abs(player.getLocation().getPitch());
        if (pitch > 90.0001F) {
            fail(player, data, 2.0D, String.format("Impossible pitch=%.2f > 90.0", pitch));
        } else {
            reward(data);
        }
    }

    public void checkSelfInteract(Player player, PlayerData data, int targetEntityId) {
        if (isExempt(player, data)) {
            return;
        }
        if (targetEntityId == player.getEntityId()) {
            fail(player, data, 2.0D, "SelfInteract entityId=" + targetEntityId);
        }
    }
}
