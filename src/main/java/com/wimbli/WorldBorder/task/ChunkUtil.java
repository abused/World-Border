package com.wimbli.WorldBorder.task;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class ChunkUtil {

    public static void unloadChunksIfNotNearSpawn(WorldServer world, int par1, int par2)
    {
        //Attempt to unload the chunk if the player can't respawn here, otherwise always unload
        if(world.provider.canRespawnHere())
        {
            BlockPos var3 = world.getSpawnPoint();
            int var4 = par1 * 16 + 8 - var3.getX();
            int var5 = par2 * 16 + 8 - var3.getZ();
            short var6 = 128;
            if(var4 < -var6 || var4 > var6 || var5 < -var6 || var5 > var6)
            {
                world.getChunkProvider().queueUnload(world.getChunkFromChunkCoords(par1, par2));
            }
        } else
        {
            world.getChunkProvider().queueUnload(world.getChunkFromChunkCoords(par1, par2));
        }
    }
}
