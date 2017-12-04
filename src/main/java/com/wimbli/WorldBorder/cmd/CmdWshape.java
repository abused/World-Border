package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.forge.Worlds;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdWshape extends WBCmd
{
    public CmdWshape()
    {
        name = permission = "wshape";
        minParams = 1;
        maxParams = 2;

        addCmdExample(nameEmphasized() + "{world} <elliptic|rectangular|default> - shape override for a single world.");
        addCmdExample(nameEmphasized() + "{world} <round|square|default> - same as above.");
        helpText = "This will override the default border shape for a single world. The value \"default\" implies " +
            "a world is just using the default border shape. See the " + commandEmphasized("shape") + C_DESC +
            "command for more info and to set the default border shape.";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        if (player == null && params.size() == 1)
        {
            sendErrorAndHelp(sender, "When running this command from console, you must specify a world.");
            return;
        }

        String shapeName;

        // world and shape specified
        if (params.size() == 2)
        {
            worldName = params.get(0);
            shapeName = params.get(1).toLowerCase();
        }
        // no world specified, just shape
        else
        {
            worldName = Worlds.getWorldName(player.worldObj);
            shapeName = params.get(0).toLowerCase();
        }

        BorderData border = Config.Border(worldName);
        if (border == null)
        {
            sendErrorAndHelp(sender, "This world (\"" + worldName + "\") does not have a border set.");
            return;
        }

        Boolean shape = null;
        if (shapeName.equals("rectangular") || shapeName.equals("square"))
            shape = false;
        else if (shapeName.equals("elliptic") || shapeName.equals("round"))
            shape = true;

        border.setShape(shape);
        Config.setBorder(worldName, border, false);

        Util.chat(sender, "Border shape for world \"" + worldName + "\" is now set to \"" + Config.getShapeName(shape) + "\".");
    }
}
