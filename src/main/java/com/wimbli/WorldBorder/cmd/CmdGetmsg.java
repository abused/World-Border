package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdGetmsg extends WBCmd
{
    public CmdGetmsg()
    {
        name = permission = "getmsg";
        minParams = maxParams = 0;

        addCmdExample(nameEmphasized() + "- display border message.");
        helpText = "This command simply displays the message shown to players knocked back from the border.";
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Util.chat(sender, "Border message is currently set to:");
        Util.chat(sender, Config.getMessageRaw());
        Util.chat(sender, "Formatted border message:");
        Util.chat(sender, Config.getMessage());
    }
}
