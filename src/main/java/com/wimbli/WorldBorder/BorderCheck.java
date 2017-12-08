package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

/**
 * Static utility class that holds logic for border and player checking
 */
public class BorderCheck
{
    // set targetLoc only if not current player location
    // set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
    public static Location checkPlayer(EntityPlayerMP player, Location targetLoc, boolean returnLocationOnly, boolean notify)
    {
        if (player == null) return null;

        Location loc = (targetLoc == null) ? new Location(player) : targetLoc;

        WorldServer world = loc.world;
        if (world == null) return null;
        BorderData border = Config.Border(Worlds.getWorldName(world));
        if (border == null) return null;

        if (border.insideBorder(loc.posX, loc.posZ, Config.getShapeRound())) return null;

        // if player is in bypass list (from bypass command), allow them beyond border; also ignore players currently being handled already
        if ( Config.isPlayerBypassing( player.getUniqueID() ) )
            return null;

        Location newLoc = newLocation(player, loc, border, notify);

        /*
         * since we need to forcibly eject players who are inside vehicles, that fires a teleport event (go figure) and
         * so would effectively double trigger for us, so we need to handle it here to prevent sending two messages and
         * two log entries etc.
         * after players are ejected we can wait a few ticks (long enough for their client to receive new entity location)
         * and then set them as passenger of the vehicle again
         */
        if (player.isRiding())
        {
            Entity ride = player.getRidingEntity();
            player.dismountRidingEntity();
            if (ride != null)
            {    // vehicles need to be offset vertically and have velocity stopped
                double vertOffset = (ride instanceof EntityLiving) ? 0 : ride.posY - loc.posY;
                Location rideLoc = new Location(newLoc);
                rideLoc.posY = newLoc.posY + vertOffset;

                Log.trace("Player was riding a \"" + ride.toString() + "\".");

                if (ride instanceof EntityBoat)
                {    // boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
                    ride.setDead();
                    ride = new EntityBoat(world, rideLoc.posX, rideLoc.posY, rideLoc.posZ);
                    world.spawnEntityInWorld(ride);
                }
                else
                    ride.setPositionAndRotation(rideLoc.posX, rideLoc.posY, rideLoc.posZ, rideLoc.pitch, rideLoc.yaw);

                if ( Config.getRemount() )
                    player.addPassenger(ride);
            }
        }

        // check if player has something (a pet, maybe?) riding them; only possible through odd plugins.
        // it can prevent all teleportation of the player completely, so it's very much not good and needs handling
        if (player.getPassengers() != null)
        {
            player.removePassengers();
            for(Entity rider : player.getPassengers()) {
                //spam
                //Util.chat(player, "Your passenger has been ejected.");
                rider.setPositionAndRotation(newLoc.posX, newLoc.posY, newLoc.posZ, newLoc.pitch, newLoc.yaw);

                Log.trace(
                    "%s had %s riding on them",
                    player.getDisplayName(),
                    rider.getCommandSenderEntity().getName()
                );
            }
        }

        // give some particle and sound effects where the player was beyond the border, if "whoosh effect" is enabled
        if (Config.doWhooshEffect()) Particles.showWhooshEffect(player);

        if (!returnLocationOnly) player.setPositionAndUpdate(newLoc.posX, newLoc.posY, newLoc.posZ);

        if (returnLocationOnly) return newLoc;

        return null;
    }

    private static Location newLocation(EntityPlayerMP player, Location loc, BorderData border, boolean notify)
    {
        Log.trace(
            "%s @ world '%s'. Border: %s",
            (notify ? "Border crossing" : "Check was run"),
            Worlds.getWorldName(loc.world), border
        );

        Log.trace("Player @ X: %.2f Y: %.2f Z: %.2f", loc.posX, loc.posY, loc.posZ);

        Location newLoc = border.correctedPosition(loc, Config.getShapeRound(), player.capabilities.isFlying);

        // it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
        if (newLoc == null)
        {
            Log.debug("Target new location unviable, trying again with border center.");

            double safeY = border.getSafeY(
                loc.world, (int) border.getX(), 64, (int) border.getZ(),
                player.capabilities.isFlying
            );

            if (safeY != 1)
            {
                newLoc = new Location(loc);
                newLoc.posX = Math.floor( border.getX() ) + 0.5;
                newLoc.posY = safeY;
                newLoc.posZ = Math.floor( border.getZ() ) + 0.5;
            }
        }

        if (newLoc == null)
        {
            Log.debug("Target new location still unviable, using spawn or killing player.");
            if ( Config.doPlayerKill() )
            {
                player.setHealth(0.0F);
                return null;
            }

            newLoc = new Location( (WorldServer) player.worldObj );
        }

        Log.trace(
            "New position @ X: %.2f Y: %.2f Z: %.2f",
            newLoc.posX, newLoc.posY, newLoc.posZ
        );

        if (notify)
            Util.chat( player, Config.getMessage() );

        return newLoc;
    }
}
