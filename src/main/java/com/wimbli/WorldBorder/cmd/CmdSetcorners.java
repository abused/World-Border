package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.List;


public class CmdSetcorners extends WBCmd
{
    public CmdSetcorners()
    {
        name = "setcorners";
        permission = "set";
        hasWorldNameInput = true;
        minParams = maxParams = 4;

        addCmdExample(nameEmphasizedW() + "<x1> <z1> <x2> <z2> - corner coords.");
        helpText = "This is an alternate way to set a border, by specifying the X and Z coordinates of two opposite " +
            "corners of the border area ((x1, z1) to (x2, z2)). [world] is optional for players and defaults to the " +
            "world the player is in.";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        if (worldName == null)
            worldName = Worlds.getWorldName(player.worldObj);
        else
        {
            WorldServer worldTest = Worlds.getWorld(worldName);
            if (worldTest == null)
                Util.chat(sender, "The world you specified (\"" + worldName + "\") could not be found on the server, but data for it will be stored anyway.");
        }

        try
        {
            double x1 = Double.parseDouble(params.get(0));
            double z1 = Double.parseDouble(params.get(1));
            double x2 = Double.parseDouble(params.get(2));
            double z2 = Double.parseDouble(params.get(3));
            Config.setBorderCorners(worldName, x1, z1, x2, z2);
        }
        catch(NumberFormatException ex)
        {
            sendErrorAndHelp(sender, "The x1, z1, x2, and z2 coordinate values must be numerical.");
            return;
        }

        if(player != null)
            Util.chat(sender, "Border has been set. " + Config.BorderDescription(worldName));
    }
}
