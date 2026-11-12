package me.anticheat.bedwars.check.impl.combat;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.Check;
import me.anticheat.bedwars.data.PlayerData;
import org.bukkit.entity.Player;

public class KillAuraB extends Check {

    private int maxAttacksPerTick;

    public KillAuraB(BedwarsACPlugin plugin) {
        super(plugin, "killaura-b", "KillAura B");
    }

    @Override
    public void reloadConfigValues() {
        super.reloadConfigValues();
        this.maxAttacksPerTick = Math.max(2, plugin.getConfigManager().getCheckInt("killaura-b", "max-attacks-per-tick"));
    }

    public void checkPacketOrder(Player attacker, PlayerData data, long now) {
        if (isExempt(attacker, data)) {
            return;
        }

        data.incrementAttacksThisTick();

        // 1. Grim PacketOrder / NoSwing: In 1.8.8, attacking an entity MUST be preceded by PacketPlayInArmAnimation
        if (!data.isSwungArmThisTick() && (now - data.getLastArmAnimationMillis() > 250L)) {
            fail(attacker, data, 1.0D, "NoSwing attack (missing ArmAnimation before UseEntity)");
            return;
        }

        // 2. Grim MultiActions: Attacking while digging a block or while an inventory GUI is open
        if (data.isDiggingBlock() || data.isInventoryOpen()) {
            fail(attacker, data, 1.0D, "MultiActions attack (digging=" + data.isDiggingBlock() + ", gui=" + data.isInventoryOpen() + ")");
            return;
        }

        // 3. Impossible attacks per single client flying tick (exempting normal double-clicks up to maxAttacksPerTick)
        if (data.getAttacksThisTick() > maxAttacksPerTick) {
            fail(attacker, data, 1.0D, "PacketOrder attacksInTick=" + data.getAttacksThisTick() + " > " + maxAttacksPerTick);
            return;
        }

        reward(data);
        data.setLastAttackPacketMillis(now);
        data.resetTicksSinceAttack();
    }
}
