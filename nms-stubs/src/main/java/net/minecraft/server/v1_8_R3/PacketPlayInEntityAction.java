package net.minecraft.server.v1_8_R3;

public class PacketPlayInEntityAction {
    public EnumPlayerAction b() {
        return EnumPlayerAction.START_SPRINTING;
    }

    public enum EnumPlayerAction {
        START_SNEAKING,
        STOP_SNEAKING,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        RIDING_JUMP,
        OPEN_INVENTORY
    }
}
