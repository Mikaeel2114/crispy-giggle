package me.anticheat.bedwars.check.impl.world;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Deque;

public class ScaffoldCheck extends Check {

    private double minValidPitch;
    private double godbridgeMinPitch;
    private double godbridgeMaxPitch;
    private int maxBlocksPerSecond;
    private double maxRotationMismatchAngle;
    private double impossibleSnapYaw;

    public ScaffoldCheck(BedwarsACPlugin plugin) {
        super(plugin, "scaffold", "Scaffold");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.minValidPitch = plugin.getConfigManager().getCheckDouble("scaffold", "min-valid-pitch");
        this.godbridgeMinPitch = plugin.getConfigManager().getCheckDouble("scaffold", "godbridge-min-pitch");
        this.godbridgeMaxPitch = plugin.getConfigManager().getCheckDouble("scaffold", "godbridge-max-pitch");
        this.maxBlocksPerSecond = plugin.getConfigManager().getCheckInt("scaffold", "max-blocks-per-second");
        this.maxRotationMismatchAngle = plugin.getConfigManager().getCheckDouble("scaffold", "max-rotation-mismatch-angle");
        this.impossibleSnapYaw = plugin.getConfigManager().getCheckDouble("scaffold", "impossible-snap-yaw");
    }

    public void checkScaffold(Player player, PlayerData data, Block placedBlock, Block placedAgainst) {
        if (isExempt(player, data) || placedBlock == null || placedAgainst == null) {
            return;
        }

        Location playerLoc = player.getLocation();
        // Only inspect bridging placements directly beneath the player's feet level
        if (placedBlock.getY() != (playerLoc.getBlockY() - 1)) {
            return;
        }

        double horizDist = Math.hypot(
                (placedBlock.getX() + 0.5D) - playerLoc.getX(),
                (placedBlock.getZ() + 0.5D) - playerLoc.getZ()
        );
        if (horizDist > 1.85D) {
            return;
        }

        float pitch = playerLoc.getPitch();

        // 1. Looking up while placing a block directly underneath feet
        if (pitch < minValidPitch) {
            fail(player, data, 2.0D, String.format("LookingUp pitch=%.1f < %.1f", pitch, minValidPitch));
            return;
        }

        // Legitimate Ninja-Bridging (sneaking at edge) and God-Bridging (steady pitch 70..85.5, low yaw snap) allowance
        boolean isLegitBridgingPosture = (data.isSneaking() && pitch >= 55.0F)
                || (pitch >= godbridgeMinPitch && pitch <= godbridgeMaxPitch && data.getDeltaYaw() < 45.0F);

        // 2. Rate limit check (blocks placed per second while horizontal bridging)
        long now = System.currentTimeMillis();
        Deque<Long> places = data.getScaffoldPlaceTimestamps();
        int placesInLastSecond;
        synchronized (places) {
            places.addLast(now);
            while (!places.isEmpty() && (now - places.peekFirst() > 1000L)) {
                places.removeFirst();
            }
            placesInLastSecond = places.size();
        }

        if (placesInLastSecond > maxBlocksPerSecond) {
            fail(player, data, 1.5D, "HighRate bps=" + placesInLastSecond + " > " + maxBlocksPerSecond);
            return;
        }

        // 3. Rotation mismatch & snap check: compare eye look vector to vector toward placedAgainst block center
        Vector eyePos = player.getEyeLocation().toVector();
        Vector targetCenter = placedAgainst.getLocation().toVector().add(new Vector(0.5D, 0.5D, 0.5D));
        Vector toBlock = targetCenter.subtract(eyePos).normalize();
        Vector lookDir = player.getEyeLocation().getDirection().normalize();

        double dot = Math.max(-1.0D, Math.min(1.0D, lookDir.dot(toBlock)));
        double angleDeg = Math.toDegrees(Math.acos(dot));

        if (angleDeg > maxRotationMismatchAngle && !isLegitBridgingPosture) {
            fail(player, data, 1.25D, String.format("RotMismatch angle=%.1f > %.1f", angleDeg, maxRotationMismatchAngle));
            return;
        }

        // 4. Impossible single-tick rotation snap during rapid bridging
        if (data.getDeltaYaw() > impossibleSnapYaw && data.getDeltaXZ() > 0.18D && !data.isSneaking()) {
            fail(player, data, 1.25D, String.format("SnapBridge deltaYaw=%.1f", data.getDeltaYaw()));
            return;
        }

        // 5. Invalid downward face placement when player is above the block
        BlockFace face = placedBlock.getFace(placedAgainst);
        if (face == BlockFace.UP && playerLoc.getY() > (placedBlock.getY() + 1.0D)) {
            fail(player, data, 1.5D, "Impossible downward face placement");
            return;
        }

        reward(data);
    }
}
