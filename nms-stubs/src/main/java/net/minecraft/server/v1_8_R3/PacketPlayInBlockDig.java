package net.minecraft.server.v1_8_R3;

public class PacketPlayInBlockDig {
    public EnumPlayerDigType c() {
        return EnumPlayerDigType.START_DESTROY_BLOCK;
    }

    public enum EnumPlayerDigType {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM
    }
}
