package com.wimbli.WorldBorder.forge;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketParticles;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;

/** Handles creation and sending of particle effect packets for server-side emission */
public class Particles
{
    public static void showWhooshEffect(EntityPlayerMP player)
    {
        WorldServer world = player.getServerWorld();
        Particles.emitEnder(world, player.posX, player.posY, player.posZ);
        Particles.emitSmoke(world, player.posX, player.posY, player.posZ);
        world.playSound(player, player.getPosition(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F, 1.0f);
    }

    private static void emitSmoke(WorldServer world, double x, double y, double z)
    {
        SPacketParticles packet = new SPacketParticles(EnumParticleTypes.SMOKE_LARGE, false, (float) x, (float) y, (float) z, 0f, 0.5f, 0f, 0.0f, 10);

        dispatch(world, packet);
    }

    private static void emitEnder(WorldServer world, double x, double y, double z)
    {
        SPacketParticles packet = new SPacketParticles(EnumParticleTypes.PORTAL, false, (float) x, (float) y, (float) z, 0.5f, 0.5f, 0.5f, 1.0f, 50);
        dispatch(world, packet);
    }

    private static void dispatch(WorldServer world, SPacketParticles packet)
    {
        //fix later, not needed right now
        //for (Object o : world.playerEntities)
        //    ((EntityPlayerMP) o).mcServer.getEntityWorld().sendPacketToServer(packet);
    }
}
