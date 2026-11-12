package me.anticheat.bedwars.packet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.anticheat.bedwars.BedwarsACPlugin;
import me.anticheat.bedwars.data.PlayerData;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.PacketPlayInArmAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockDig;
import net.minecraft.server.v1_8_R3.PacketPlayInBlockPlace;
import net.minecraft.server.v1_8_R3.PacketPlayInEntityAction;
import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import net.minecraft.server.v1_8_R3.PacketPlayInWindowClick;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityVelocity;
import net.minecraft.server.v1_8_R3.PacketPlayOutExplosion;
import net.minecraft.server.v1_8_R3.PacketPlayOutPosition;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketInterceptor {

    private static final String HANDLER_NAME = "bedwars_ac_packet_handler";

    private final BedwarsACPlugin plugin;
    private final ExecutorService asyncPacketExecutor;
    private boolean protocolLibPresent = false;

    private Field useEntityIdField;
    private Field velocityEntityIdField;
    private Field velocityXField;
    private Field velocityYField;
    private Field velocityZField;

    public PacketInterceptor(BedwarsACPlugin plugin) {
        this.plugin = plugin;
        this.asyncPacketExecutor = Executors.newSingleThreadExecutor();
        this.protocolLibPresent = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        initReflectionFields();
        if (this.protocolLibPresent) {
            plugin.getLogger().info("ProtocolLib detected (optional compatibility enabled alongside v1_8_R3 Netty pipeline).");
        }
    }

    private void initReflectionFields() {
        try {
            useEntityIdField = PacketPlayInUseEntity.class.getDeclaredField("a");
            useEntityIdField.setAccessible(true);

            velocityEntityIdField = PacketPlayOutEntityVelocity.class.getDeclaredField("a");
            velocityEntityIdField.setAccessible(true);
            velocityXField = PacketPlayOutEntityVelocity.class.getDeclaredField("b");
            velocityXField.setAccessible(true);
            velocityYField = PacketPlayOutEntityVelocity.class.getDeclaredField("c");
            velocityYField.setAccessible(true);
            velocityZField = PacketPlayOutEntityVelocity.class.getDeclaredField("d");
            velocityZField.setAccessible(true);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to initialize v1_8_R3 packet reflection fields: " + ex.getMessage());
        }
    }

    public void inject(final Player player, final PlayerData data) {
        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            final Channel channel = nmsPlayer.playerConnection.networkManager.channel;
            if (channel == null) {
                return;
            }
            channel.eventLoop().submit(new Runnable() {
                @Override
                public void run() {
                    if (channel.pipeline().get(HANDLER_NAME) != null) {
                        channel.pipeline().remove(HANDLER_NAME);
                    }
                    if (channel.pipeline().get("packet_handler") != null) {
                        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, new ACPacketHandler(player, data));
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    public void uninject(final Player player) {
        try {
            EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            final Channel channel = nmsPlayer.playerConnection.networkManager.channel;
            if (channel == null) {
                return;
            }
            channel.eventLoop().submit(new Runnable() {
                @Override
                public void run() {
                    if (channel.pipeline().get(HANDLER_NAME) != null) {
                        channel.pipeline().remove(HANDLER_NAME);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    public void shutdown() {
        asyncPacketExecutor.shutdownNow();
    }

    private class ACPacketHandler extends ChannelDuplexHandler {

        private final Player player;
        private final PlayerData data;

        public ACPacketHandler(Player player, PlayerData data) {
            this.player = player;
            this.data = data;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, final Object msg) throws Exception {
            final long now = System.currentTimeMillis();

            if (msg instanceof PacketPlayInFlying) {
                final PacketPlayInFlying flying = (PacketPlayInFlying) msg;
                final boolean onGround = flying.f();
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleFlyingPacket(player, data, flying, onGround, now);
                    }
                });
            } else if (msg instanceof PacketPlayInUseEntity) {
                final PacketPlayInUseEntity usePacket = (PacketPlayInUseEntity) msg;
                final PacketPlayInUseEntity.EnumEntityUseAction action = usePacket.a();
                int targetEntityId = -1;
                try {
                    if (useEntityIdField != null) {
                        targetEntityId = useEntityIdField.getInt(usePacket);
                    }
                } catch (Exception ignored) {
                }
                final int finalTargetId = targetEntityId;
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleUseEntityPacket(player, data, action, finalTargetId, now);
                    }
                });
            } else if (msg instanceof PacketPlayInArmAnimation) {
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleArmAnimation(player, data, now);
                    }
                });
            } else if (msg instanceof PacketPlayInBlockDig) {
                final PacketPlayInBlockDig digPacket = (PacketPlayInBlockDig) msg;
                final PacketPlayInBlockDig.EnumPlayerDigType digType = digPacket.c();
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleBlockDigPacket(player, data, digType, now);
                    }
                });
            } else if (msg instanceof PacketPlayInBlockPlace) {
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleBlockPlacePacket(player, data, now);
                    }
                });
            } else if (msg instanceof PacketPlayInEntityAction) {
                final PacketPlayInEntityAction actionPacket = (PacketPlayInEntityAction) msg;
                final PacketPlayInEntityAction.EnumPlayerAction playerAction = actionPacket.b();
                if (playerAction == PacketPlayInEntityAction.EnumPlayerAction.START_SPRINTING) {
                    data.setSprinting(true);
                } else if (playerAction == PacketPlayInEntityAction.EnumPlayerAction.STOP_SPRINTING) {
                    data.setSprinting(false);
                } else if (playerAction == PacketPlayInEntityAction.EnumPlayerAction.START_SNEAKING) {
                    data.setSneaking(true);
                } else if (playerAction == PacketPlayInEntityAction.EnumPlayerAction.STOP_SNEAKING) {
                    data.setSneaking(false);
                }
            } else if (msg instanceof PacketPlayInWindowClick) {
                final PacketPlayInWindowClick clickPacket = (PacketPlayInWindowClick) msg;
                final int slot = clickPacket.b();
                asyncPacketExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        plugin.getCheckManager().handleWindowClickPacket(player, data, slot, now);
                    }
                });
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof PacketPlayOutEntityVelocity) {
                try {
                    if (velocityEntityIdField != null) {
                        int entityId = velocityEntityIdField.getInt(msg);
                        if (entityId == player.getEntityId()) {
                            double vx = velocityXField.getInt(msg) / 8000.0D;
                            double vy = velocityYField.getInt(msg) / 8000.0D;
                            double vz = velocityZField.getInt(msg) / 8000.0D;
                            data.recordVelocity(vx, vy, vz);
                        }
                    }
                } catch (Exception ignored) {
                }
            } else if (msg instanceof PacketPlayOutExplosion) {
                data.setExplosionExemptTicks(plugin.getConfigManager().getExemptExplosionKnockbackTicks());
            } else if (msg instanceof PacketPlayOutPosition) {
                data.setTeleportTicks(20);
            }
            super.write(ctx, msg, promise);
        }
    }
}
