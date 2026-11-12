package me.anticheat.bedwars.check;

import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.check.impl.combat.AimModulo360Check;
import me.anticheat.bedwars.check.impl.combat.AutoClickerA;
import me.anticheat.bedwars.check.impl.combat.BadPacketsCheck;
import me.anticheat.bedwars.check.impl.combat.AutoClickerB;
import me.anticheat.bedwars.check.impl.combat.CriticalsCheck;
import me.anticheat.bedwars.check.impl.combat.KillAuraA;
import me.anticheat.bedwars.check.impl.combat.KillAuraB;
import me.anticheat.bedwars.check.impl.combat.KillAuraC;
import me.anticheat.bedwars.check.impl.combat.NoSlowdownCheck;
import me.anticheat.bedwars.check.impl.combat.ReachCheck;
import me.anticheat.bedwars.check.impl.combat.VelocityCheck;
import me.anticheat.bedwars.check.impl.movement.FlyCheck;
import me.anticheat.bedwars.check.impl.movement.JesusCheck;
import me.anticheat.bedwars.check.impl.movement.NoFallCheck;
import me.anticheat.bedwars.check.impl.movement.PhaseCheck;
import me.anticheat.bedwars.check.impl.movement.SpeedCheck;
import me.anticheat.bedwars.check.impl.movement.StepCheck;
import me.anticheat.bedwars.check.impl.movement.TimerCheck;
import me.anticheat.bedwars.check.impl.world.BedThroughWallCheck;
import me.anticheat.bedwars.check.impl.world.ChestStealerCheck;
import me.anticheat.bedwars.check.impl.world.FastBreakCheck;
import me.anticheat.bedwars.check.impl.world.FastUseCheck;
import me.anticheat.bedwars.check.impl.world.InvMoveCheck;
import me.anticheat.bedwars.check.impl.world.NukerCheck;
import me.anticheat.bedwars.check.impl.world.ScaffoldCheck;
import me.anticheat.bedwars.data.PlayerData;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockDig;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CheckManager {

    private final BedwarsACPlugin plugin;
    private final List<Check> allChecks = new ArrayList<Check>();

    // Combat checks
    private final KillAuraA killAuraA;
    private final KillAuraB killAuraB;
    private final KillAuraC killAuraC;
    private final ReachCheck reachCheck;
    private final AutoClickerA autoClickerA;
    private final AutoClickerB autoClickerB;
    private final VelocityCheck velocityCheck;
    private final CriticalsCheck criticalsCheck;
    private final NoSlowdownCheck noSlowdownCheck;
    private final AimModulo360Check aimModulo360Check;
    private final BadPacketsCheck badPacketsCheck;

    // Movement checks
    private final SpeedCheck speedCheck;
    private final FlyCheck flyCheck;
    private final TimerCheck timerCheck;
    private final NoFallCheck noFallCheck;
    private final JesusCheck jesusCheck;
    private final StepCheck stepCheck;
    private final PhaseCheck phaseCheck;

    // World / Bedwars checks
    private final ScaffoldCheck scaffoldCheck;
    private final FastBreakCheck fastBreakCheck;
    private final BedThroughWallCheck bedThroughWallCheck;
    private final NukerCheck nukerCheck;
    private final ChestStealerCheck chestStealerCheck;
    private final InvMoveCheck invMoveCheck;
    private final FastUseCheck fastUseCheck;

    public CheckManager(BedwarsACPlugin plugin) {
        this.plugin = plugin;

        this.killAuraA = register(new KillAuraA(plugin));
        this.killAuraB = register(new KillAuraB(plugin));
        this.killAuraC = register(new KillAuraC(plugin));
        this.reachCheck = register(new ReachCheck(plugin));
        this.autoClickerA = register(new AutoClickerA(plugin));
        this.autoClickerB = register(new AutoClickerB(plugin));
        this.velocityCheck = register(new VelocityCheck(plugin));
        this.criticalsCheck = register(new CriticalsCheck(plugin));
        this.noSlowdownCheck = register(new NoSlowdownCheck(plugin));
        this.aimModulo360Check = register(new AimModulo360Check(plugin));
        this.badPacketsCheck = register(new BadPacketsCheck(plugin));

        this.speedCheck = register(new SpeedCheck(plugin));
        this.flyCheck = register(new FlyCheck(plugin));
        this.timerCheck = register(new TimerCheck(plugin));
        this.noFallCheck = register(new NoFallCheck(plugin));
        this.jesusCheck = register(new JesusCheck(plugin));
        this.stepCheck = register(new StepCheck(plugin));
        this.phaseCheck = register(new PhaseCheck(plugin));

        this.scaffoldCheck = register(new ScaffoldCheck(plugin));
        this.fastBreakCheck = register(new FastBreakCheck(plugin));
        this.bedThroughWallCheck = register(new BedThroughWallCheck(plugin));
        this.nukerCheck = register(new NukerCheck(plugin));
        this.chestStealerCheck = register(new ChestStealerCheck(plugin));
        this.invMoveCheck = register(new InvMoveCheck(plugin));
        this.fastUseCheck = register(new FastUseCheck(plugin));
    }

    private <T extends Check> T register(T check) {
        this.allChecks.add(check);
        return check;
    }

    public void reloadChecks() {
        for (Check check : allChecks) {
            check.reloadConfigValues();
        }
    }

    public List<Check> getAllChecks() {
        return Collections.unmodifiableList(allChecks);
    }

    public void handleFlyingPacket(Player player, PlayerData data, PacketPlayInFlying packet, boolean onGround, long now) {
        data.setLastFlyingPacketMillis(now);
        data.setSentFlyingSinceLastAttack(true);
        data.setSwungArmThisTick(false);
        data.incrementClientTick();

        timerCheck.checkTimer(player, data, now);
        noFallCheck.checkNoFall(player, data, onGround);
    }

    public void handleUseEntityPacket(Player player, PlayerData data,
                                      PacketPlayInUseEntity.EnumEntityUseAction action, int targetId, long now) {
        badPacketsCheck.checkSelfInteract(player, data, targetId);
        if (action == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
            killAuraB.checkPacketOrder(player, data, now);
        }
    }

    public void handleArmAnimation(Player player, PlayerData data, long now) {
        data.setSwungArmThisTick(true);
        autoClickerA.checkClick(player, data, now);
        autoClickerB.checkConsistency(player, data, now);
    }

    public void handleBlockDigPacket(Player player, PlayerData data,
                                     PacketPlayInBlockDig.EnumPlayerDigType digType, long now) {
        data.setLastDigPacketMillis(now);
        if (digType == PacketPlayInBlockDig.EnumPlayerDigType.START_DESTROY_BLOCK) {
            data.setDiggingBlock(true);
            data.setBlockBreakStartMillis(now);
        } else if (digType == PacketPlayInBlockDig.EnumPlayerDigType.STOP_DESTROY_BLOCK
                || digType == PacketPlayInBlockDig.EnumPlayerDigType.ABORT_DESTROY_BLOCK) {
            data.setDiggingBlock(false);
        }
    }

    public void handleBlockPlacePacket(Player player, PlayerData data, long now) {
        data.setPlacedBlockThisTick(true);
        data.setLastBlockPlaceMillis(now);
        if (player.getItemInHand() != null && player.getItemInHand().getType().isEdible()) {
            data.setItemUseStartMillis(now);
        }
    }

    public void handleWindowClickPacket(Player player, PlayerData data, int slot, long now) {
        if (data.isInventoryOpen()) {
            chestStealerCheck.checkWindowClick(player, data, slot, now);
        }
    }

    public void handleCombatHit(Player attacker, PlayerData data, Entity victim) {
        killAuraA.checkCombat(attacker, data, victim);
        killAuraC.checkAimSnap(attacker, data, victim);
        reachCheck.checkReach(attacker, data, victim);
        criticalsCheck.checkCriticals(attacker, data);
    }

    public void handleMovement(Player player, PlayerData data, Location from, Location to) {
        data.updateMovement(player, from, to, player.isOnGround());

        speedCheck.checkSpeed(player, data);
        flyCheck.checkFly(player, data);
        jesusCheck.checkJesus(player, data);
        stepCheck.checkStep(player, data);
        phaseCheck.checkPhase(player, data, from, to);
        velocityCheck.checkVelocity(player, data);
        noSlowdownCheck.checkMovement(player, data);
        invMoveCheck.checkInvMove(player, data);
        aimModulo360Check.checkRotation(player, data);
        badPacketsCheck.checkPitch(player, data);
    }

    public void handleBlockPlace(Player player, PlayerData data, Block placedBlock, Block placedAgainst) {
        scaffoldCheck.checkScaffold(player, data, placedBlock, placedAgainst);
    }

    public void handleBlockBreak(Player player, PlayerData data, BlockBreakEvent event) {
        Block block = event.getBlock();
        bedThroughWallCheck.checkBedBreak(player, data, event);
        if (!event.isCancelled()) {
            fastBreakCheck.checkBlockBreak(player, data, block);
            nukerCheck.checkNuker(player, data, block);
        }
    }

    public void handleItemConsume(Player player, PlayerData data, long now) {
        fastUseCheck.checkConsume(player, data, now);
    }
}
