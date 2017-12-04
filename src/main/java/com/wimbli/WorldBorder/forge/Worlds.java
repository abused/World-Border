package com.wimbli.WorldBorder.forge;

import com.wimbli.WorldBorder.WorldBorder;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/** Static utility class for managing and dealing with Forge worlds */
public class Worlds
{
    /**
     * Generates a canonical name from a given world.
     *
     * Instead of using internal folder names, this uses the Dynmap method of using the
     * dimension number with the 'DIM' prefix. This improves compatibility with Dynmap
     * and makes world handling provider-agnostic.
     */
    public static String getWorldName(World world)
    {
        // Dimension 0 will always use the name configured in server.properties
        return (world.provider.getDimension() == 0)
            ? world.getWorldInfo().getWorldName()
            : "DIM" + world.provider.getDimension();
    }

    /**
     * Performs a case-sensitive search for a loaded world by a given name.
     *
     * First, it tries to match the name with dimension 0 (overworld), then it tries to
     * match from the world's save folder name (e.g. DIM_MYST10) and then finally the
     * Dynmap compatible identifier (e.g. DIM10)
     *
     * @param name Name of world to find
     * @return World if found, else null
     */
    public static WorldServer getWorld(String name)
    {
        if ( name == null || name.isEmpty() )
            throw new IllegalArgumentException("World name cannot be empty or null");

        for ( WorldServer world : DimensionManager.getWorlds() )
        {
            String dimName    = "DIM" + world.provider.getDimension();
            String saveFolder = world.provider.getSaveFolder();

            if (world.provider.getDimension() == 0)
            {   // Special case for dimension 0 (overworld)
                if ( WorldBorder.SERVER.getFolderName().equals(name) )
                    return world;
            }
            else if ( saveFolder.equals(name) || dimName.equals(name) )
                return world;
        }

        return null;
    }

    /** Safely saves a given world to disk */
    public static void saveWorld(WorldServer world)
    {
        try
        {
            Boolean saveFlag  = world.disableLevelSaving;
            world.disableLevelSaving = true;
            world.saveAllChunks(true, null);
            world.disableLevelSaving = saveFlag;
        }
        catch (MinecraftException e)
        {
            e.printStackTrace();
        }
    }

    /** Gets the ID of the block type of the given block position in a world */
    public static int getBlockID(World world, int x, int y, int z)
    {
        return Block.getIdFromBlock(world.getBlockState(new BlockPos(x, y, z)).getBlock());
    }
}
