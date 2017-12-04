package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.WorldBorder;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;
import java.util.List;


public class CmdHelp extends WBCmd
{
    public CmdHelp()
    {
        name = permission = "help";
        minParams = 0;
        maxParams = 10;

        addCmdExample(nameEmphasized() + "[command] - get help on command usage.");
//		helpText = "If [command] is specified, info for that particular command will be provided.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        String commands = WorldBorder.COMMAND.getCommandNames().toString().replace(", ", C_DESC + ", " + C_CMD);
        Util.chat(sender, C_HEAD + "Commands: " + C_CMD + commands.substring(1, commands.length() - 1));
        Util.chat(sender, "Example, for info on \"set\" command: " + cmd(sender) + nameEmphasized() + C_CMD + "set");
        Util.chat(sender, C_HEAD + "For a full command example list, simply run the root " + cmd(sender) + C_HEAD + "command by itself with nothing specified.");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        if (params.isEmpty())
        {
            sendCmdHelp(sender);
            return;
        }

        ArrayList<String> commands = WorldBorder.COMMAND.getCommandNames();
        for (String param : params)
            if (commands.contains(param.toLowerCase()))
            {
                WorldBorder.COMMAND.subCommands.get(param.toLowerCase()).sendCmdHelp(sender);
                return;
            }

        sendErrorAndHelp(sender, "No command recognized.");
    }
}
