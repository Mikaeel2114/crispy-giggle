package me.anticheat.bedwars.check.impl.movement;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PhaseCheck extends Check {

    private double maxPhaseDistance;

    public PhaseCheck(BedwarsACPlugin plugin) {
        super(plugin, "phase", "Phase");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxPhaseDistance = plugin.getConfigManager().getCheckDouble("phase", "max-phase-distance");
    }

    public void checkPhase(Player player, PlayerData data, Location from, Location to) {
        if (isExempt(player, data) || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (data.getTeleportTicks() > 0 || data.getRespawnExemptTicks() > 0 || player.getVehicle() != null) {
            return;
        }

        if (data.getDeltaXZ() < maxPhaseDistance) {
            return;
        }

        Block headBlock = to.clone().add(0.0D, 1.5D, 0.0D).getBlock();
        Block feetBlock = to.clone().add(0.0D, 0.4D, 0.0D).getBlock();

        if (isFullSolidPhaseBlock(headBlock.getType()) && isFullSolidPhaseBlock(feetBlock.getType())) {
            fail(player, data, 1.0D, "Clipped into " + feetBlock.getType().name());
        } else {
            reward(data);
        }
    }

    private boolean isFullSolidPhaseBlock(Material mat) {
        if (mat == null || !mat.isSolid() || !mat.isOccluding()) {
            return false;
        }
        String name = mat.name();
        if (name.contains("DOOR") || name.contains("GATE") || name.contains("FENCE")
                || name.contains("STAIRS") || name.contains("STEP") || name.contains("SLAB")
                || name.contains("SIGN") || name.contains("BANNER") || name.contains("PISTON")
                || mat == Material.SAND || mat == Material.GRAVEL || mat == Material.ANVIL) {
            return false;
        }
        return true;
    }
}
