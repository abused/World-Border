package com.wimbli.WorldBorder.task;

import com.wimbli.WorldBorder.*;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

/**
 * Singleton tick handler that performs a fill task over a long running series of ticks
 */
public class WorldFillTask
{
    private static WorldFillTask INSTANCE = null;

    /** Gets the singleton instance of this task, or null if none exists */
    public static WorldFillTask getInstance()
    {
        return INSTANCE;
    }

    /** Creates a singleton instance of this task, rethrowing any errors */
    public static WorldFillTask create(
        ICommandSender requester, String worldName,
        boolean forceLoad, int fillDistance, int chunksPerRun, int tickFrequency)
    {
        if (INSTANCE != null)
            throw new IllegalStateException("There can only be one WorldFillTask");
        else try
        {
            INSTANCE = new WorldFillTask(requester, worldName, forceLoad, fillDistance, chunksPerRun, tickFrequency);
            return INSTANCE;
        }
        catch (Exception e)
        {
            INSTANCE = null;
            throw e;
        }
    }

    // Per-task shortcut references
    private final WorldServer         world;
    private final WorldFileData       worldData;
    private final ChunkProviderServer provider;
    private final BorderData          border;
    private final ICommandSender 	  requester;

    // Per-task state variables
    private final List<CoordXZ> storedChunks   = new LinkedList<>();
    private final Set<CoordXZ>  originalChunks = new HashSet<>();
    private final CoordXZ       lastChunk      = new CoordXZ(0, 0);

    private int     chunksPerRun   = 1;
    private boolean readyToGo      = false;
    private boolean paused         = false;
    private boolean memoryPause    = false;
    private boolean continueNotice = false;
    private boolean forceLoad      = false;

    // Per-task state for the spiral fill pattern
    private int     x       = 0;
    private int     z       = 0;
    private boolean isZLeg  = false;
    private boolean isNeg   = false;
    private boolean inside  = true;
    private int     length  = -1;
    private int     current = 0;

    // Per-task state for progress reporting
    private long lastReport   = Util.now();
    private long lastAutosave = Util.now();
    private int  reportTarget = 0;
    private int  reportTotal  = 0;
    private int  reportNum    = 0;

    // Per-task persistent settings
    private int fillDistance  = 208;
    private int tickFrequency = 1;
    private int refLength     = -1;

    private int refX     = 0, lastLegX     = 0;
    private int refZ     = 0, lastLegZ     = 0;
    private int refTotal = 0, lastLegTotal = 0;

    // <editor-fold desc="Getters">
    /** Gets X of last chunk to be processed */
    public int getRefX()
    {
        return refX;
    }

    /** Gets Z of last chunk to be processed */
    public int getRefZ()
    {
        return refZ;
    }

    /** Gets progress amount of chunks to process */
    public int getRefLength()
    {
        return refLength;
    }

    /** Gets total amount of chunks to process */
    public int getRefTotal()
    {
        return refTotal;
    }

    /** Gets configured fill distance of this task */
    public int getFillDistance()
    {
        return fillDistance;
    }

    /** Gets configured how many ticks are each run */
    public int getTickFrequency()
    {
        return tickFrequency;
    }

    /** Gets configured amount of chunks to fill per run */
    public int getChunksPerRun()
    {
        return chunksPerRun;
    }

    /** Gets configured world of this task */
    public String getWorld()
    {
        return Worlds.getWorldName(world);
    }

    /** Gets whether this task forces loading of existing chunks */
    public boolean getForceLoad()
    {
        return forceLoad;
    }
    // </editor-fold>

    /** Starts this task by registering the tick handler */
    public void start()
    {
        if (INSTANCE != this)
            throw new IllegalStateException("Cannot start a stopped task");

        FMLCommonHandler.instance().bus().register(this);
    }

    /** Starts this task by resuming from prior progress */
    public void startFrom(int x, int z, int length, int totalDone)
    {
        this.x 				= x;
        this.z 				= z;
        this.length         = length;
        this.reportTotal    = totalDone;
        this.continueNotice = true;
        start();
    }

    /** Stops this task by unregistering the tick handler and removing the instance */
    public void stop()
    {
        if (INSTANCE != this)
            throw new IllegalStateException("Task has already been stopped");
        else
            FMLCommonHandler.instance().bus().unregister(this);

        // Unload chunks that are still loaded
        while( !storedChunks.isEmpty() )
        {
            CoordXZ coord = storedChunks.remove(0);

            if ( !originalChunks.contains(coord) )
                ChunkUtil.unloadChunksIfNotNearSpawn(world, coord.x, coord.z);
        }

        originalChunks.clear();

        INSTANCE = null;
    }

    // TODO: Optimize this away
    public void pause()
    {
        if(this.memoryPause)
            pause(false);
        else
            pause(!this.paused);
    }

    public void pause(boolean pause)
    {
        if (this.memoryPause && !pause)
            this.memoryPause = false;
        else
            this.paused = pause;
        if (this.paused)
        {
            Config.storeFillTask();
            reportProgress();
        }
        else
            Config.deleteFillTask();
    }

    public boolean isPaused()
    {
        return this.paused || this.memoryPause;
    }

    private WorldFillTask(ICommandSender requester, String worldName, boolean forceLoad, int fillDistance, int chunksPerRun, int tickFrequency)
    {
        this.requester     = requester;
        this.fillDistance  = fillDistance;
        this.tickFrequency = tickFrequency;
        this.chunksPerRun  = chunksPerRun;
        this.forceLoad     = forceLoad;

        this.world = Worlds.getWorld(worldName);

        if (this.world == null)
            throw new IllegalArgumentException("World \"" + worldName + "\" not found!");

        this.border = (Config.Border(worldName) == null)
            ? null
            : Config.Border(worldName).copy();

        if (this.border == null)
            throw new IllegalStateException("No border found for world \"" + worldName + "\"!");

        this.worldData = new WorldFileData(world, requester);

        this.border.setRadiusX(border.getRadiusX() + fillDistance);
        this.border.setRadiusZ(border.getRadiusZ() + fillDistance);
        this.x = CoordXZ.blockToChunk((int)border.getX());
        this.z = CoordXZ.blockToChunk((int)border.getZ());

        // We need to calculate the reportTarget with the bigger width, since the spiral
        // will only stop if it has a size of biggerWidth * biggerWidth
        int chunkWidthX = (int) Math.ceil((double)((border.getRadiusX() + 16) * 2) / 16);
        int chunkWidthZ = (int) Math.ceil((double)((border.getRadiusZ() + 16) * 2) / 16);
        int biggerWidth = (chunkWidthX > chunkWidthZ) ? chunkWidthX : chunkWidthZ;

        this.reportTarget = (biggerWidth * biggerWidth) + biggerWidth + 1;

        // Keep track of the chunks which are already loaded when the task starts, to not unload them
        this.provider = world.getChunkProvider();
        Collection<Chunk> originals = provider.getLoadedChunks();

        for (Chunk original : originals)
            originalChunks.add(new CoordXZ(original.xPosition, original.zPosition));

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

        if (continueNotice)
        {	// notify user that task has continued automatically
            continueNotice = false;
            sendMessage("World map generation task automatically continuing.");
            sendMessage("Reminder: you can cancel at any time with \"wb fill cancel\", or pause/unpause with \"wb fill pause\".");
        }

        if (memoryPause)
        {	// if available memory gets too low, we automatically pause, so handle that
            if ( Config.isAvailableMemoryTooLow() )
                return;

            memoryPause = false;
            readyToGo   = true;
            sendMessage("Available memory is sufficient, automatically continuing.");
        }

        if (!readyToGo || paused)
            return;

        // this is set so it only does one iteration at a time, no matter how frequently the timer fires
        readyToGo = false;
        // and this is tracked to keep one iteration from dragging on too long and possibly choking the system if the user specified a really high frequency
        long loopStartTime = Util.now();

        for (int loop = 0; loop < chunksPerRun; loop++)
        {
            // in case the task has been paused while we're repeating...
            if (paused || memoryPause)
                return;

            long now = Util.now();

            // every 5 seconds or so, give basic progress report to let user know how it's going
            if (now > lastReport + 5000)
                reportProgress();

            // if this iteration has been running for 45ms (almost 1 tick) or more, stop to take a breather
            if (now > loopStartTime + 45)
            {
                readyToGo = true;
                return;
            }

            // if we've made it at least partly outside the border, skip past any such chunks
            while (!border.insideBorder(CoordXZ.chunkToBlock(x) + 8, CoordXZ.chunkToBlock(z) + 8))
                if (!moveToNext())
                    return;

            inside = true;

            // skip past any chunks which are confirmed as fully generated using our super-special isChunkFullyGenerated routine
            if (!forceLoad)
                while (worldData.isChunkFullyGenerated(x, z))
                {
                    inside = true;
                    if (!moveToNext())
                        return;
                }

            // load the target chunk and generate it if necessary
            provider.loadChunk(x, z);
            worldData.chunkExistsNow(x, z);

            // There need to be enough nearby chunks loaded to make the server populate a chunk with trees, snow, etc.
            // So, we keep the last few chunks loaded, and need to also temporarily load an extra inside chunk (neighbor closest to center of map)
            int popX = !isZLeg ? x : (x + (isNeg ? -1 : 1));
            int popZ = isZLeg ? z : (z + (!isNeg ? -1 : 1));
            // RoyCurtis: this originally specified "false" for chunk generation; things
            // may break now that it is true
            provider.loadChunk(popX, popZ);

            // make sure the previous chunk in our spiral is loaded as well (might have already existed and been skipped over)
            if (!storedChunks.contains(lastChunk) && !originalChunks.contains(lastChunk))
            {
                provider.loadChunk(lastChunk.x, lastChunk.z);
                storedChunks.add(new CoordXZ(lastChunk.x, lastChunk.z));
            }

            // Store the coordinates of these latest 2 chunks we just loaded, so we can unload them after a bit...
            storedChunks.add(new CoordXZ(popX, popZ));
            storedChunks.add(new CoordXZ(x, z));

            // If enough stored chunks are buffered in, go ahead and unload the oldest to free up memory
            while (storedChunks.size() > 8)
            {
                CoordXZ coord = storedChunks.remove(0);

                if (!originalChunks.contains(coord))
                    ChunkUtil.unloadChunksIfNotNearSpawn(world, coord.x, coord.z);
            }

            // move on to next chunk
            if (!moveToNext())
                return;
        }

        // ready for the next iteration to run
        readyToGo = true;
    }

    // step through chunks in spiral pattern from center; returns false if we're done, otherwise returns true
    public boolean moveToNext()
    {
        if (paused || memoryPause)
            return false;

        reportNum++;

        // keep track of progress in case we need to save to config for restoring progress after server restart
        if (!isNeg && current == 0 && length > 3)
        {
            if (!isZLeg)
            {
                lastLegX     = x;
                lastLegZ     = z;
                lastLegTotal = reportTotal + reportNum;
            }
            else
            {
                refX      = lastLegX;
                refZ      = lastLegZ;
                refTotal  = lastLegTotal;
                refLength = length - 1;
            }
        }

        // make sure of the direction we're moving (X or Z? negative or positive?)
        if (current < length)
            current++;
        else
        {	// one leg/side of the spiral down...
            current  = 0;
            isZLeg  ^= true;
            if (isZLeg)
            {	// every second leg (between X and Z legs, negative or positive), length increases
                isNeg ^= true;
                length++;
            }
        }

        // keep track of the last chunk we were at
        lastChunk.x = x;
        lastChunk.z = z;

        // move one chunk further in the appropriate direction
        if (isZLeg)
            z += (isNeg) ? -1 : 1;
        else
            x += (isNeg) ? -1 : 1;

        // if we've been around one full loop (4 legs)...
        if (isZLeg && isNeg && current == 0)
        {	// see if we've been outside the border for the whole loop
            if (!inside)
            {	// and finish if so
                finish();
                return false;
            }	// otherwise, reset the "inside border" flag
            else
                inside = false;
        }
        return true;

    /* reference diagram used, should move in this pattern:
     *  8 [>][>][>][>][>] etc.
     * [^][6][>][>][>][>][>][6]
     * [^][^][4][>][>][>][4][v]
     * [^][^][^][2][>][2][v][v]
     * [^][^][^][^][0][v][v][v]
     * [^][^][^][1][1][v][v][v]
     * [^][^][3][<][<][3][v][v]
     * [^][5][<][<][<][<][5][v]
     * [7][<][<][<][<][<][<][7]
     */
    }

    private void finish()
    {
        this.paused = true;
        reportProgress();
        Worlds.saveWorld(world);
        sendMessage("Task successfully completed for world \"" + getWorld() + "\"!");
        this.stop();
    }

    // let the user know how things are coming along
    private void reportProgress()
    {
        lastReport = Util.now();
        double perc = ((double)(reportTotal + reportNum) / (double)reportTarget) * 100;
        if (perc > 100) perc = 100;
        sendMessage(reportNum + " more chunks processed (" + (reportTotal + reportNum) + " total, ~" + Config.COORD_FORMAT.format(perc) + "%%" + ")");
        reportTotal += reportNum;
        reportNum = 0;

        // go ahead and save world to disk every 30 seconds or so by default, just in case; can take a couple of seconds or more, so we don't want to run it too often
        if (Config.getFillAutosaveFrequency() > 0 && lastAutosave + (Config.getFillAutosaveFrequency() * 1000) < lastReport)
        {
            lastAutosave = lastReport;
            sendMessage("Saving the world to disk, just to be on the safe side.");
            Worlds.saveWorld(world);
        }
    }

    // send a message to the server console/log and possibly to an in-game player
    private void sendMessage(String text)
    {
        // Due to chunk generation eating up memory and Java being too slow about GC, we need to track memory availability
        long availMem = Config.getAvailableMemory();

        Log.info("[Fill] " + text + " (free mem: " + availMem + " MB)");
        if (requester instanceof EntityPlayerMP)
            Util.chat(requester, "[Fill] " + text + " (free mem: " + availMem + " MB)");

        if ( Config.isAvailableMemoryTooLow() )
        {	// running low on memory, auto-pause
            memoryPause = true;
            Config.storeFillTask();
            text = "Available memory is very low, task is pausing. A cleanup will be attempted now, " +
                "and the task will automatically continue if/when sufficient memory is freed up.\n " +
                "Alternatively, if you restart the server, this task will automatically continue once " +
                "the server is back up.";

            Log.info("[Fill] " + text);
            if (requester instanceof EntityPlayerMP)
                Util.chat(requester, "[Fill] " + text);

            // Forced garbage-collection works well to immediately recover memory
            System.gc();
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        Log.debug( "WorldFillTask cleaned up for %s", getWorld() );
    }
}
