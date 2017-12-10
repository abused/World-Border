package com.wimbli.WorldBorder.task;

import com.wimbli.WorldBorder.BorderCheck;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Tick handler that regularly checks for players that have wandered beyond any
 * configured borders. Handles knocking back of outside players.
 */
public class BorderCheckTask
{
    private boolean running = false;

    public boolean isRunning()
    {
        return running;
    }

    /** Sets whether this task is running by (un)registering it as a tick handler */
    public void setRunning(boolean state)
    {
        if (state)
            FMLCommonHandler.instance().bus().register(this);
        else
            FMLCommonHandler.instance().bus().unregister(this);

        running = state;
    }

    /** Uses lowest event priority to run after everything else has */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        // Only run at end of tick to catch players that just moved past border
        if (event.phase == TickEvent.Phase.START)
            return;

        if ( WorldBorder.SERVER.getTickCounter() % Config.getTimerTicks() != 0 )
            return;

        for (Object o : WorldBorder.SERVER.getPlayerList().getPlayers())
            BorderCheck.checkPlayer( (EntityPlayerMP) o, null, false, true );
    }
}
