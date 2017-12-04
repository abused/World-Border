package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.WorldBorder;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;

public class CmdCommands extends WBCmd
{
    private static int pageSize = 8;  // examples to list per page; 10 lines available, 1 for header, 1 for footer

    public CmdCommands()
    {
        name = "commands";
        permission = "help";
        hasWorldNameInput = false;
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        // determine which page we're viewing
        int page = (player == null) ? 0 : 1;
        if (!params.isEmpty())
            try
            {
                page = Integer.parseInt(params.get(0));
            }
            catch(NumberFormatException ignored) {}

        // see whether we're showing examples to player or to console, and determine number of pages available
        List<String> examples = (player == null) ? cmdExamplesConsole : cmdExamplesPlayer;
        int pageCount = (int) Math.ceil(examples.size() / (double) pageSize);

        // if specified page number is negative or higher than we have available, default back to first page
        if (page < 0 || page > pageCount)
            page = (player == null) ? 0 : 1;

        // send command example header
        Util.chat(sender, C_HEAD + WorldBorder.MODID + "  -  key: " +
                commandEmphasized("command") + C_REQ + "<required> " + C_OPT + "[optional]");

        if (page > 0)
        {
            // send examples for this page
            int first = (page - 1) * pageSize;
            int count = Math.min(pageSize, examples.size() - first);

            for(int i = first; i < first + count; i++)
                Util.chat(sender, examples.get(i));

            // send page footer, if relevant; manual spacing to get right side lined up near edge is crude, but sufficient
            String footer = C_HEAD + " (Page " + page + "/" + pageCount + ")              " + cmd(sender);
            if (page < pageCount)
                Util.chat(sender, footer + Integer.toString(page + 1) + C_DESC + " - view next page of commands.");
            else if (page > 1)
                Util.chat(sender, footer + C_DESC + "- view first page of commands.");
        }
        else
            // if page "0" is specified, send all examples; done by default for console but can be specified by player
            for (String example : examples)
                Util.chat(sender, example);
    }
}
