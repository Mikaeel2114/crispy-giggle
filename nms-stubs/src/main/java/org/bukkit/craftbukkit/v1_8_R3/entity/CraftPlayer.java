package org.bukkit.craftbukkit.v1_8_R3.entity;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.entity.Player;

public interface CraftPlayer extends Player {
    EntityPlayer getHandle();
}
