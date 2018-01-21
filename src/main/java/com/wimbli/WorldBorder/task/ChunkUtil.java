package com.wimbli.WorldBorder.task;

import com.wimbli.WorldBorder.forge.Log;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public class ChunkUtil {

    public static void unloadChunksIfNotNearSpawn(WorldServer world, int par1, int par2)
    {
        if(world.provider.canRespawnHere())
        {
            BlockPos var3 = world.getSpawnPoint();
            int var4 = par1 * 16 + 8 - var3.getX();
            int var5 = par2 * 16 + 8 - var3.getZ();
            short var6 = 128;
            if(!(var4 < -var6 || var4 > var6 || var5 < -var6 || var5 > var6))
            {
            	Log.warn("Couldn't unload chunk from location (X: " + par1 + ", Z: " + par2 + ")");
            	return;
            }
        }
        world.getChunkProvider().queueUnload(world.getChunkFromChunkCoords(par1, par2));
    }
}
