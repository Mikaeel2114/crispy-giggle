package net.minecraft.server.v1_8_R3;

public class PacketPlayInUseEntity {
    private int a;

    public EnumEntityUseAction a() {
        return EnumEntityUseAction.ATTACK;
    }

    public enum EnumEntityUseAction {
        INTERACT,
        ATTACK,
        INTERACT_AT
    }
}
