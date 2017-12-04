package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.io.*;
import java.util.*;

/**
 * Represents the file structure of a world.
 *
 * This region file handler was created based on the divulged region file format:
 * http://mojang.com/2011/02/16/minecraft-save-file-format-in-beta-1-3/
 */
public class WorldFileData
{
    private static final FileFilter MCA_FILTER = new ExtFileFilter(".MCA");

    private Map<CoordXZ, List<Boolean>> regionChunkExistence = new HashMap<>();

    private File[]         regionFiles  = null;
    private ICommandSender requester    = null;

    /** Creates a region file handler for a given world and requester */
    public WorldFileData(World world, ICommandSender requester)
    {
        this.requester = requester;

        // TODO: Move this to "getRootDir" of new Worlds class
        File rootDir = (world.provider.getDimension() == 0)
            ? DimensionManager.getCurrentSaveRootDirectory()
            : new File(
                DimensionManager.getCurrentSaveRootDirectory(),
                world.provider.getSaveFolder()
            );

        File regionFolder = new File(rootDir, "region");
        if ( !regionFolder.exists() || !regionFolder.isDirectory() )
            throw new RuntimeException(
                "Could not validate folder for world's region files. Tried to use "
                + regionFolder.getPath() + " as valid region folder."
            );

        this.regionFiles = regionFolder.listFiles(MCA_FILTER);
        if (this.regionFiles == null || this.regionFiles.length == 0)
            throw new RuntimeException(
                "Could not find any region files. Looked in: " + regionFolder.getPath()
            );

        Log.debug(
            "Using path '%s' for world '%s'",
            regionFolder.getAbsolutePath(),
            Worlds.getWorldName(world)
        );
    }

    /** Shortcut for number of region files this world has */
    public int regionFileCount()
    {
        return regionFiles.length;
    }

    /** Gets a region file by index, or null if out of bounds */
    public File regionFile(int index)
    {
        if (regionFiles.length < index)
            return null;

        return regionFiles[index];
    }

    /** Gets the X and Z world coordinates of the region from the filename */
    public CoordXZ regionFileCoordinates(int index)
    {
        File     regionFile = this.regionFile(index);
        String[] coords     = regionFile.getName().split("\\.");
        int x, z;
        try
        {
            x = Integer.parseInt(coords[1]);
            z = Integer.parseInt(coords[2]);
            return new CoordXZ (x, z);
        }
        catch(Exception ex)
        {
            sendMessage("Error! Region file found with abnormal name: "+regionFile.getName());
            return null;
        }
    }

    /** Find out if the chunk at the given coordinates exists */
    public boolean doesChunkExist(int x, int z)
    {
        CoordXZ region = new CoordXZ(
            CoordXZ.chunkToRegion(x),
            CoordXZ.chunkToRegion(z)
        );

        return this.getRegionData(region).get( coordToRegionOffset(x, z) );
    }

    /**
     * Checks if the chunk at the given coordinates has been fully generated.
     * Minecraft only fully generates a chunk when adjacent chunks are also loaded.
     */
    public boolean isChunkFullyGenerated(int x, int z)
    {	// if all adjacent chunks exist, it should be a safe enough bet that this one is fully generated
        return
            ! (
            ! doesChunkExist(x, z)
         || ! doesChunkExist(x+1, z)
         || ! doesChunkExist(x-1, z)
         || ! doesChunkExist(x, z+1)
         || ! doesChunkExist(x, z-1)
            );
    }

    /** Callback for when a chunk is generate, to update our region map */
    public void chunkExistsNow(int x, int z)
    {
        CoordXZ region = new CoordXZ(
            CoordXZ.chunkToRegion(x),
            CoordXZ.chunkToRegion(z)
        );

        this.getRegionData(region).set(coordToRegionOffset(x, z), true);
    }

    /**
     * Calculates region offset of the given coordinates.
     *
     * Region is 32 * 32 chunks; chunk pointers are stored in region file at position:
     *     x + z * 32 (32 * 32 chunks = 1024)
     * Input x and z values can be world-based chunk coordinates or local-to-region
     * chunk coordinates, either one
     */
    private int coordToRegionOffset(int x, int z)
    {
        // "%" modulus is used to convert potential world coordinates to definitely be local region coordinates
        x = x % 32;
        z = z % 32;
        // similarly, for local coordinates, we need to wrap negative values around
        if (x < 0) x += 32;
        if (z < 0) z += 32;
        // return offset position for the now definitely local x and z values
        return x + (z * 32);
    }

    private List<Boolean> getRegionData(CoordXZ region)
    {
        List<Boolean> data = regionChunkExistence.get(region);
        if (data != null)
            return data;

        // data for the specified region isn't loaded yet, so init it as empty and try to
        // find the file and load the data
        data = new ArrayList<Boolean>(1024);
        for (int i = 0; i < 1024; i++)
            data.add(Boolean.FALSE);

        for (int i = 0; i < regionFiles.length; i++)
        {
            CoordXZ coord = regionFileCoordinates(i);
            // is this region file the one we're looking for?
            if ( !coord.equals(region) )
                continue;

            try (RandomAccessFile regionData = new RandomAccessFile(this.regionFile(i), "r"))
            {
                Log.trace( "Trying to read region file '%s'", regionFile(i) );

                // first 4096 bytes of region file consists of 4-byte int pointers to chunk data in the file
                // (32*32 chunks = 1024; 1024 chunks * 4 bytes each = 4096)
                // if chunk pointer data is 0, chunk doesn't exist yet; otherwise, it does
                for (int j = 0; j < 1024; j++)
                    if (regionData.readInt() != 0)
                        data.set(j, true);
            }
            catch (FileNotFoundException ex)
            {
                sendMessage("Error! Could not open region file to find generated chunks: " + this.regionFile(i).getName());
            }
            catch (IOException ex)
            {
                sendMessage("Error! Could not read region file to find generated chunks: " + this.regionFile(i).getName());
            }
        }
        regionChunkExistence.put(region, data);
        return data;
    }

    /** Send a message to the server console/log and possibly to an in-game player */
    private void sendMessage(String text)
    {
        Log.info("[WorldData] " + text);
        if (requester != null)
            Util.chat(requester, "[WorldData] " + text);
    }

    /** File filter used for region files */
    private static class ExtFileFilter implements FileFilter
    {
        String ext;
        public ExtFileFilter(String extension)
        {
            this.ext = extension.toLowerCase();
        }

        @Override
        public boolean accept(File file)
        {
            return (
                file.exists()
             && file.isFile()
             && file.getName().toLowerCase().endsWith(ext)
            );
        }
    }
}
