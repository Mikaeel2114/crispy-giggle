package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class CriticalsCheck extends Check {

    public CriticalsCheck(BedwarsACPlugin plugin) {
        super(plugin, "criticals", "Criticals");
    }

    public void checkCriticals(Player attacker, PlayerData data) {
        if (isExempt(attacker, data)) {
            return;
        }
        if (data.getLiquidTicks() > 0 || data.getClimbableTicks() > 0 || data.getSlimeTicks() > 0
                || data.getStairSlabTicks() > 0 || data.getUnderBlockTicks() > 0
                || data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || attacker.getVehicle() != null
                || attacker.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            return;
        }

        float bukkitFallDist = attacker.getFallDistance();
        boolean bukkitCriticalState = bukkitFallDist > 0.0F && !attacker.isOnGround();

        // Only flag packet criticals when the player has been solidly on the ground for >= 5 ticks with zero vertical motion
        if (bukkitCriticalState && data.isServerOnGround() && data.isLastServerOnGround()
                && data.getGroundTicks() >= 5 && Math.abs(data.getDeltaY()) < 1.0E-5D && Math.abs(data.getLastDeltaY()) < 1.0E-5D) {
            fail(attacker, data, 1.0D, String.format("PacketCrit fallDist=%.3f groundTicks=%d", bukkitFallDist, data.getGroundTicks()));
            return;
        }

        reward(data);
    }
}
