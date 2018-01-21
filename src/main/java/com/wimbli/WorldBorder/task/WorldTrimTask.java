package com.wimbli.WorldBorder.task;

import com.wimbli.WorldBorder.*;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton tick handler that performs a trim task over a long running series of ticks
 */
public class WorldTrimTask
{
    private static WorldTrimTask INSTANCE = null;

    /** Gets the singleton instance of this task, or null if none exists */
    public static WorldTrimTask getInstance()
    {
        return INSTANCE;
    }

    public static WorldTrimTask create(
        ICommandSender player, String worldName,
        int trimDistance, int chunksPerRun, int tickFrequency)
    {
        if (INSTANCE != null)
            throw new IllegalStateException("There can only be one WorldTrimTask");
        else try
        {
            INSTANCE = new WorldTrimTask(player, worldName, trimDistance, chunksPerRun, tickFrequency);
            return INSTANCE;
        }
        catch (Exception e)
        {
            INSTANCE = null;
            throw e;
        }
    }

    // Per-task shortcut references
    private final WorldServer    world;
    private final WorldFileData  worldData;
    private final BorderData     border;
    private final ICommandSender requester;

    // Per-task state variables
    private List<CoordXZ> regionChunks = new ArrayList<>(1024);
    private List<CoordXZ> trimChunks   = new ArrayList<>(1024);

    private int     tickFrequency = 1;
    private int     chunksPerRun  = 1;
    private boolean readyToGo     = false;
    private boolean paused        = false;
    private boolean deleteError   = false;

    // Per-task state region progress tracking
    private int currentRegion = -1;  // region(file) we're at in regionFiles
    private int currentChunk  = 0;   // chunk we've reached in the current region (regionChunks)

    private int regionX = 0;  // X location value of the current region
    private int regionZ = 0;  // X location value of the current region
    private int counter = 0;

    // Per-task state for progress reporting
    private long lastReport   = Util.now();
    private int  reportTarget = 0;
    private int  reportTotal  = 0;

    private int reportTrimmedRegions = 0;
    private int reportTrimmedChunks  = 0;

    /** Starts this task by registering the tick handler */
    public void start()
    {
        if (INSTANCE != this)
            throw new IllegalStateException("Cannot start a stopped task");

        //FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    /** Stops this task by unregistering the tick handler and removing the instance */
    public void stop()
    {
        if (INSTANCE != this)
            throw new IllegalStateException("Task has already been stopped");
        else
            MinecraftForge.EVENT_BUS.unregister(this);

        regionChunks.clear();
        trimChunks.clear();

        INSTANCE = null;
    }

    // TODO: Optimize this away
    public void pause()
    {
        pause(!this.paused);
    }

    public void pause(boolean pause)
    {
        this.paused = pause;
        if (pause)
            reportProgress();
    }

    public boolean isPaused()
    {
        return this.paused;
    }

    private WorldTrimTask(ICommandSender player, String worldName, int trimDistance, int chunksPerRun, int tickFrequency)
    {
        this.requester     = player;
        this.tickFrequency = tickFrequency;
        this.chunksPerRun  = chunksPerRun;

        this.world = Worlds.getWorld(worldName);
        if (this.world == null)
            throw new IllegalArgumentException("World \"" + worldName + "\" not found!");

        this.border = (Config.Border(worldName) == null)
            ? null
            : Config.Border(worldName).copy();

        if (this.border == null)
            throw new IllegalStateException("No border found for world \"" + worldName + "\"!");

        this.worldData = new WorldFileData(world, requester);

        this.border.setRadiusX(border.getRadiusX() + trimDistance);
        this.border.setRadiusZ(border.getRadiusZ() + trimDistance);

        // each region file covers up to 1024 chunks; with all operations we might need to do, let's figure 3X that
        this.reportTarget = worldData.regionFileCount() * 3072;

        // queue up the first file
        if (!nextFile())
            return;

        this.readyToGo = true;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        // Only run at start of tick
        if (event.phase == TickEvent.Phase.END)
            return;

        if (WorldBorder.SERVER.getTickCounter() % tickFrequency != 0)
            return;

        if (!readyToGo || paused)
            return;

        // TODO: make this less crude by kicking or teleporting players in dimension
//        if (DimensionManager.getWorld(world.provider.dimensionId) != null)
//        {
//            Log.debug( "Trying to unload dimension %s", Util.getWorldName(world) );
//            DimensionManager.unloadWorld(world.provider.dimensionId);
//            return;
//        }

        // this is set so it only does one iteration at a time, no matter how frequently the timer fires
        readyToGo = false;
        // and this is tracked to keep one iteration from dragging on too long and possibly choking the system if the user specified a really high frequency
        long loopStartTime = Util.now();

        counter = 0;
        while (counter <= chunksPerRun)
        {
            // in case the task has been paused while we're repeating...
            if (paused)
                return;

            long now = Util.now();

            // every 5 seconds or so, give basic progress report to let user know how it's going
            if (now > lastReport + 5000)
                reportProgress();

            // if this iteration has been running for 45ms (almost 1 tick) or more, stop to take a breather; shouldn't normally be possible with Trim, but just in case
            if (now > loopStartTime + 45)
            {
                readyToGo = true;
                return;
            }

            if (regionChunks.isEmpty())
                addCornerChunks();
            else if (currentChunk == 4)
            {	// determine if region is completely _inside_ border based on corner chunks
                if (trimChunks.isEmpty())
                {	// it is, so skip it and move on to next file
                    counter += 4;
                    nextFile();
                    continue;
                }
                addEdgeChunks();
                addInnerChunks();
            }
            else if (currentChunk == 124 && trimChunks.size() == 124)
            {	// region is completely _outside_ border based on edge chunks, so delete file and move on to next
                counter += 16;
                trimChunks = regionChunks;
                unloadChunks();
                reportTrimmedRegions++;
                File regionFile = worldData.regionFile(currentRegion);

                try
                {
                    Files.delete( regionFile.toPath() );

                    Log.trace(
                        "Deleted region file '%s' for world '%s'",
                        regionFile.getAbsolutePath(),
                        Worlds.getWorldName(world)
                    );
                }
                catch (Exception e)
                {
                    Log.warn(
                        "Exception when deleting region file '%s': %s",
                        regionFile.getName(),
                        e.getMessage().replaceAll("\n", "")
                    );

                    deleteError = true;
                    wipeChunks();
                }

                // if DynMap is installed, re-render the trimmed region
                DynMapFeatures.renderRegion(world, new CoordXZ(regionX, regionZ));

                nextFile();
                continue;
            }
            else if (currentChunk == 1024)
            {	// last chunk of the region has been checked, time to wipe out whichever chunks are outside the border
                counter += 32;
                unloadChunks();
                wipeChunks();
                nextFile();
                continue;
            }

            // check whether chunk is inside the border or not, add it to the "trim" list if not
            CoordXZ chunk = regionChunks.get(currentChunk);
            if (!isChunkInsideBorder(chunk))
                trimChunks.add(chunk);

            currentChunk++;
            counter++;
        }

        reportTotal += counter;

        // ready for the next iteration to run
        readyToGo = true;
    }

    // Advance to the next region file. Returns true if successful, false if the next file isn't accessible for any reason
    private boolean nextFile()
    {
        reportTotal = currentRegion * 3072;
        currentRegion++;
        regionX = regionZ = currentChunk = 0;
        regionChunks = new ArrayList<>(1024);
        trimChunks   = new ArrayList<>(1024);

        // have we already handled all region files?
        if (currentRegion >= worldData.regionFileCount())
        {	// hey, we're done
            paused = true;
            readyToGo = false;
            finish();
            return false;
        }

        counter += 16;

        // get the X and Z coordinates of the current region
        CoordXZ coord = worldData.regionFileCoordinates(currentRegion);
        if (coord == null)
            return false;

        regionX = coord.x;
        regionZ = coord.z;
        return true;
    }

    // add just the 4 corner chunks of the region; can determine if entire region is _inside_ the border
    private void addCornerChunks()
    {
        regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX), CoordXZ.regionToChunk(regionZ)));
        regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX) + 31, CoordXZ.regionToChunk(regionZ)));
        regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX), CoordXZ.regionToChunk(regionZ) + 31));
        regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX) + 31, CoordXZ.regionToChunk(regionZ) + 31));
    }

    // add all chunks along the 4 edges of the region (minus the corners); can determine if entire region is _outside_ the border
    private void addEdgeChunks()
    {
        int chunkX = 0, chunkZ;

        for (chunkZ = 1; chunkZ < 31; chunkZ++)
            regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));

        chunkX = 31;
        for (chunkZ = 1; chunkZ < 31; chunkZ++)
            regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));

        chunkZ = 0;
        for (chunkX = 1; chunkX < 31; chunkX++)
            regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));

        chunkZ = 31;
        for (chunkX = 1; chunkX < 31; chunkX++)
            regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));

        counter += 4;
    }

    // add the remaining interior chunks (after corners and edges)
    private void addInnerChunks()
    {
        for (int chunkX = 1; chunkX < 31; chunkX++)
        for (int chunkZ = 1; chunkZ < 31; chunkZ++)
            regionChunks.add(new CoordXZ(CoordXZ.regionToChunk(regionX)+chunkX, CoordXZ.regionToChunk(regionZ)+chunkZ));

        counter += 32;
    }

    // make sure chunks set to be trimmed are not currently loaded by the server
    private void unloadChunks()
    {
        for (CoordXZ unload : trimChunks)
            ChunkUtil.unloadChunksIfNotNearSpawn(world, unload.x, unload.z);

        world.getChunkProvider().tick();
        counter += trimChunks.size();
    }

    // edit region file to wipe all chunk pointers for chunks outside the border
    private void wipeChunks()
    {
        File regionFile = worldData.regionFile(currentRegion);
        if (!regionFile.canWrite())
        {
            if (!regionFile.setWritable(true))
                throw new RuntimeException();

            if (!regionFile.canWrite())
            {
                sendMessage("Error! region file is locked and can't be trimmed: " + regionFile.getName());
                return;
            }
        }

        // since our stored chunk positions are based on world, we need to offset those to positions in the region file
        int  offsetX    = CoordXZ.regionToChunk(regionX);
        int  offsetZ    = CoordXZ.regionToChunk(regionZ);
        int  chunkCount = 0;
        long wipePos;

        try ( RandomAccessFile unChunk = new RandomAccessFile(regionFile, "rwd") )
        {
            for (CoordXZ wipe : trimChunks)
            {
                // if the chunk pointer is empty (chunk doesn't technically exist), no need to wipe the already empty pointer
                if (!worldData.doesChunkExist(wipe.x, wipe.z))
                    continue;

                // wipe this extraneous chunk's pointer... note that this method isn't perfect since the actual chunk data is left orphaned,
                // but Minecraft will overwrite the orphaned data sector if/when another chunk is created in the region, so it's not so bad
                wipePos = 4 * ((wipe.x - offsetX) + ((wipe.z - offsetZ) * 32));
                unChunk.seek(wipePos);
                unChunk.writeInt(0);
                chunkCount++;
            }

            // if DynMap is installed, re-render the trimmed chunks
            // TODO: check if this now works
            DynMapFeatures.renderChunks(world, trimChunks);

            reportTrimmedChunks += chunkCount;
        }
        catch (FileNotFoundException ex)
        {
            sendMessage("Error! Could not open region file to wipe individual chunks: "+regionFile.getName());
        }
        catch (IOException ex)
        {
            sendMessage("Error! Could not modify region file to wipe individual chunks: "+regionFile.getName());
        }

        counter += trimChunks.size();
    }

    private boolean isChunkInsideBorder(CoordXZ chunk)
    {
        return border.insideBorder(CoordXZ.chunkToBlock(chunk.x) + 8, CoordXZ.chunkToBlock(chunk.z) + 8);
    }

    // for successful completion
    private void finish()
    {
        reportTotal = reportTarget;
        reportProgress();
        sendMessage("Task successfully completed for world \"" + Worlds.getWorldName(world) + "\"!");

        if (deleteError)
            sendMessage(
                "One or more region files could not be deleted. It may be that the world " +
                "spawn point covers those regions, or the server is running on Windows. "  +
                "Restart the server and retry trimming without players logged in."
            );

        this.stop();
    }

    // let the user know how things are coming along
    private void reportProgress()
    {
        lastReport = Util.now();
        double perc = ((double)(reportTotal) / (double)reportTarget) * 100;
        sendMessage(reportTrimmedRegions + " entire region(s) and " + reportTrimmedChunks + " individual chunk(s) trimmed so far (" + Config.COORD_FORMAT.format(perc) + "%% done" + ")");
    }

    // send a message to the server console/log and possibly to an in-game player
    private void sendMessage(String text)
    {
        Log.info("[Trim] " + text);
        if (requester instanceof EntityPlayerMP)
            Util.chat(requester, "[Trim] " + text);
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        Log.debug( "WorldTrimTask cleaned up for %s", Worlds.getWorldName(world) );
    }
}
