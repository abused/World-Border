package com.wimbli.WorldBorder.forge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

/**
 * Represents a position, pitch and yaw of a specific world. Modelled after Bukkit's API
 * structure of Location, but does not use any of its implementation
 */
public class Location
{
    public WorldServer world = null;

    public double posX  = 0;
    public double posY  = 0;
    public double posZ  = 0;
    public float  pitch = 0.0f;
    public float  yaw   = 0.0f;

    /**
     * Creates a Location based on the target position of a player and a fired
     * {@link EnderTeleportEvent}
     */
    public Location(EnderTeleportEvent event, EntityPlayerMP player)
    {
        world = (WorldServer) player.worldObj;
        posX  = event.getTargetX();
        posY  = event.getTargetY();
        posZ  = event.getTargetZ();
        pitch = player.rotationPitch;
        yaw   = player.rotationYaw;
    }

    /** Creates a Location based on the latest (target) position of a player */
    public Location(EntityPlayer player)
    {
        world = (WorldServer) player.worldObj;
        posX  = player.posX;
        posY  = player.posY;
        posZ  = player.posZ;
        pitch = player.rotationPitch;
        yaw   = player.rotationYaw;
    }

    /** Clones an existing Location */
    public Location(Location loc)
    {
        world = loc.world;
        posX  = loc.posX;
        posY  = loc.posY;
        posZ  = loc.posZ;
        pitch = loc.pitch;
        yaw   = loc.yaw;
    }

    /** Creates a location from a world's spawn point */
    public Location(WorldServer world)
    {
        BlockPos spawn = world.getSpawnPoint();

        this.world = world;
        this.posX  = spawn.getX();
        this.posY  = spawn.getY();
        this.posZ  = spawn.getZ();
        this.pitch = 0;
        this.yaw   = 0;
    }

    /** Creates a new Location with all data provided */
    public Location(WorldServer world, double x, double y, double z, float yaw, float pitch)
    {
        this.world = world;
        this.posX  = x;
        this.posY  = y;
        this.posZ  = z;
        this.pitch = pitch;
        this.yaw   = yaw;
    }

    /**
     * TODO: Find faster algorithm than native
     */
    public static int locToBlock(double loc)
    {
        return (int) Math.floor(loc);
    }
}
