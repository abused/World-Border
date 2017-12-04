package com.wimbli.WorldBorder.listener;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockPlaceListener
{
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.PlaceEvent event)
    {
        if ( isInsideBorder(event.getWorld(), event.getPos().getX(), event.getPos().getZ()) )
            return;

        event.setResult(BlockEvent.Result.DENY);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMultiBlockPlace(BlockEvent.MultiPlaceEvent event)
    {
        if ( isInsideBorder(event.getWorld(), event.getPos().getX(), event.getPos().getZ()) )
            return;

        event.setResult(BlockEvent.Result.DENY);
        event.setCanceled(true);
    }

    private boolean isInsideBorder(World world, int x, int z)
    {
        BorderData border = Config.Border(Worlds.getWorldName(world));

        return border == null
            || border.insideBorder( x, z, Config.getShapeRound() );
    }
}
