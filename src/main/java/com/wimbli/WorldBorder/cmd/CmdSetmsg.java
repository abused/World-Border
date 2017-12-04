package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdSetmsg extends WBCmd
{
    public CmdSetmsg()
    {
        name = permission = "setmsg";
        minParams = 1;

        addCmdExample(nameEmphasized() + "<text> - set border message.");
        helpText = "Default value: \"&cYou have reached the edge of this world.\". This command lets you set the message shown to players who are knocked back from the border.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Border message is set to:");
        Util.chat(sender, Config.getMessageRaw());
        Util.chat(sender, C_HEAD + "Formatted border message:");
        Util.chat(sender, Config.getMessage());
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        StringBuilder message = new StringBuilder();
        boolean first = true;
        for (String param : params)
        {
            if (!first)
                message.append(" ");
            message.append(param);
            first = false;
        }

        Config.setMessage(message.toString());

        cmdStatus(sender);
    }
}
