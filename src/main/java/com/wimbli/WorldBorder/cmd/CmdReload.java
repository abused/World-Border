package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdReload extends WBCmd
{
    public CmdReload()
    {
        name = permission = "reload";
        minParams = maxParams = 0;

        addCmdExample(nameEmphasized() + "- re-load data from config.yml.");
        helpText = "If you make manual changes to config.yml while the server is running, you can use this command " +
            "to make WorldBorder load the changes without needing to restart the server.";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        if (player != null)
            Log.info("Reloading config file at the command of player \"" + player.getDisplayName() + "\".");

        Config.load(true);

        if (player != null)
            Util.chat(sender, "WorldBorder configuration reloaded.");
    }
}
