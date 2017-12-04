package com.wimbli.WorldBorder.listener;

import com.wimbli.WorldBorder.BorderCheck;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.forge.Log;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class EnderPearlListener
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPearl(EnderTeleportEvent event)
    {
        if ( !(event.getEntityLiving() instanceof EntityPlayerMP) )
            return;

        if ( Config.getKnockBack() == 0.0 || !Config.getDenyEnderpearl() )
            return;

        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        Log.trace( "Caught pearl teleport event by %s", player.getDisplayName() );

        Location target = new Location(event, player);
        Location newLoc = BorderCheck.checkPlayer(player, target, true, true);

        if (newLoc != null)
        {
            event.setCanceled(true);
            event.setTargetX(newLoc.posX);
            event.setTargetY(newLoc.posY);
            event.setTargetZ(newLoc.posZ);
        }
    }
}
