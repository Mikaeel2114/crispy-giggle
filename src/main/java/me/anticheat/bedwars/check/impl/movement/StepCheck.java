package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class StepCheck extends Check {

    private double maxStepHeight;

    public StepCheck(BedwarsACPlugin plugin) {
        super(plugin, "step", "Step");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxStepHeight = plugin.getConfigManager().getCheckDouble("step", "max-step-height");
    }

    public void checkStep(Player player, PlayerData data) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (data.getVelocityTicks() > 0 || data.getExplosionExemptTicks() > 0
                || data.getTeleportTicks() > 0 || data.getSlimeTicks() > 0
                || player.getVehicle() != null) {
            return;
        }

        double jumpBoostExtra = 0.0D;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.JUMP)) {
                jumpBoostExtra = (effect.getAmplifier() + 1) * 0.125D;
            }
        }

        double deltaY = data.getDeltaY();
        double limit = maxStepHeight + jumpBoostExtra;

        if (data.isLastServerOnGround() && deltaY > limit) {
            fail(player, data, 1.0D, String.format("Step deltaY=%.3f > %.3f", deltaY, limit));
        } else {
            reward(data);
        }
    }
}
