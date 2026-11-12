package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpeedCheck extends Check {

    private double baseGroundSpeed;
    private double baseAirSpeed;
    private double airFriction;
    private double airAcceleration;
    private double jumpBoostAllowance;
    private double speedPotionMultiplier;

    public SpeedCheck(BedwarsACPlugin plugin) {
        super(plugin, "speed", "Speed");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.baseGroundSpeed = plugin.getConfigManager().getCheckDouble("speed", "base-ground-speed");
        this.baseAirSpeed = plugin.getConfigManager().getCheckDouble("speed", "base-air-speed");
        this.airFriction = plugin.getConfigManager().getCheckDouble("speed", "air-friction");
        this.airAcceleration = plugin.getConfigManager().getCheckDouble("speed", "air-acceleration");
        this.jumpBoostAllowance = plugin.getConfigManager().getCheckDouble("speed", "jump-boost-allowance");
        this.speedPotionMultiplier = plugin.getConfigManager().getCheckDouble("speed", "speed-potion-multiplier");
    }

    public void checkSpeed(Player player, PlayerData data) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }

        // Exempt ice, slime, soul sand, stairs/slabs, velocity, explosions, and teleports
        if (data.getIceTicks() > 0 || data.getSlimeTicks() > 0 || data.getSoulSandTicks() > 0
                || data.getStairSlabTicks() > 0 || data.getVelocityTicks() > 0
                || data.getExplosionExemptTicks() > 0 || data.getTeleportTicks() > 0
                || data.getUnderBlockTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        int speedAmplifier = 0;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType().equals(PotionEffectType.SPEED)) {
                speedAmplifier = effect.getAmplifier() + 1;
            }
        }
        // Exempt high speed potions or scale predictive limit accordingly
        if (speedAmplifier > 2) {
            return;
        }

        double walkSpeedScale = Math.max(1.0D, player.getWalkSpeed() / 0.2D);
        double potionMultiplier = 1.0D + (speedAmplifier * speedPotionMultiplier);

        double deltaXZ = data.getDeltaXZ();
        double lastDeltaXZ = data.getLastDeltaXZ();

        // 1. Predictive Air Friction Model: nextXZ <= lastXZ * 0.91 + 0.026 * potionMultiplier
        if (!data.isServerOnGround() && !data.isLastServerOnGround() && data.getAirTicks() > 1) {
            double predictedMaxAir = (lastDeltaXZ * airFriction) + (airAcceleration * potionMultiplier * walkSpeedScale) + 0.01D;
            double absoluteMaxAir = baseAirSpeed * potionMultiplier * walkSpeedScale;
            double allowedAir = Math.max(predictedMaxAir, absoluteMaxAir);

            if (deltaXZ > allowedAir) {
                fail(player, data, 1.0D, String.format("airXZ=%.3f > pred=%.3f", deltaXZ, allowedAir));
                return;
            }
        } else {
            // 2. Ground / Sprint-Jump Transition Model
            double allowedGround = baseGroundSpeed * potionMultiplier * walkSpeedScale;
            if (!data.isServerOnGround() && data.isLastServerOnGround() && data.getDeltaY() > 0.0D) {
                allowedGround += jumpBoostAllowance * potionMultiplier;
            } else if (data.getGroundTicks() <= 3) {
                allowedGround += 0.09D * potionMultiplier;
            }

            if (deltaXZ > allowedGround) {
                fail(player, data, 1.0D, String.format("groundXZ=%.3f > max=%.3f", deltaXZ, allowedGround));
                return;
            }
        }

        reward(data);
    }
}
