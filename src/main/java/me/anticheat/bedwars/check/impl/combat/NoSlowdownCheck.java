package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NoSlowdownCheck extends Check {

    private double maxItemUseSpeed;

    public NoSlowdownCheck(BedwarsACPlugin plugin) {
        super(plugin, "noslowdown", "NoSlowdown");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxItemUseSpeed = plugin.getConfigManager().getCheckDouble("noslowdown", "max-item-use-speed");
    }

    public void checkMovement(Player player, PlayerData data) {
        if (isExempt(player, data)) {
            return;
        }
        if (data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || data.getIceTicks() > 0 || data.getSlimeTicks() > 0 || data.getStairSlabTicks() > 0) {
            return;
        }

        // Ignore brief right-click sword block taps during 10+ CPS block-hit PvP (< 450ms)
        long now = System.currentTimeMillis();
        if (now - data.getLastAttackPacketMillis() < 450L) {
            reward(data);
            return;
        }

        double speedLimit = maxItemUseSpeed;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                speedLimit += (effect.getAmplifier() + 1) * 0.05D;
            }
        }

        // Flag sustained sword-blocking while sprinting on flat ground for > 6 ticks without attacking
        if (player.isBlocking() && data.isSprinting() && data.isServerOnGround()
                && data.getGroundTicks() > 6 && data.getDeltaXZ() > speedLimit) {
            fail(player, data, 0.75D, String.format("NoSlow blocking speed=%.3f > %.3f", data.getDeltaXZ(), speedLimit));
            return;
        }

        reward(data);
    }
}
