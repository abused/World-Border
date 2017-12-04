package com.wimbli.WorldBorder;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.wimbli.WorldBorder.cmd.*;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class WBCommand implements ICommand
{
    static final String NAME    = "wborder";
    static final List   ALIASES = Arrays.asList(NAME, "wb", "worldborder");

    // map of all sub-commands with the command name (string) for quick reference
    public Map<String, WBCmd> subCommands = new LinkedHashMap<>();
    // ref. list of the commands which can have a world name in front of the command itself (ex. /wb _world_ radius 100)
    private Set<String> subCommandsWithWorldNames = new LinkedHashSet<>();

    private ArrayList<String> subCommandNames = null;

    /**
     * Checks the server's registered commands in case any other commands override
     * WorldBorder's. Will print errors to the log if conflicts found.
     */
    public static void checkRegistrations(MinecraftServer server)
    {
        List<String> valid    = new ArrayList<>( ALIASES.size() );
        List<String> conflict = new ArrayList<>( ALIASES.size() );

        Map commands = server.getCommandManager().getCommands();

        for (Object o : ALIASES)
        {
            String name  = (String) o;
            Object value = commands.get(name);

            if (value == null)
                Log.error("Null handler for '/%s'! Please report this", name);
            else if (value instanceof WBCommand)
                valid.add("/" + name);
            else
                conflict.add(
                    String.format( "/%s (from %s)", name, value.getClass().getName() )
                );
        }

        if (valid.size() == 0)
        {
            Log.error("All WorldBorder commands are being handled elsewhere:");
            for (String c : conflict) Log.error("* %s", c);
            Log.error("It may be that another mod is attempting to provide world " +
                "border functionality. Consider removing or disabling that mod to " +
                "allow WorldBorder-Forge to work properly.");
        }
        else if (conflict.size() > 0)
        {
            Log.warn("The following WorldBorder commands are being handled elsewhere:");
            for (String c : conflict) Log.warn("* %s", c);
            Log.warn("It may be that another mod is attempting to provide world " +
                "border functionality. Consider removing or disabling that mod to " +
                "allow WorldBorder-Forge to work. Alternatively, try these commands:");
            for (String v : valid) Log.warn("* %s", v);
        }
    }

    // constructor
    public WBCommand ()
    {
        addCmd(new CmdHelp());			// 1 example
        addCmd(new CmdSet());			// 4 examples for player, 3 for console
        addCmd(new CmdSetcorners());	// 1
        addCmd(new CmdRadius());		// 1
        addCmd(new CmdList());			// 1
        //----- 8 per page of examples
        addCmd(new CmdShape());			// 2
        addCmd(new CmdClear());			// 2
        addCmd(new CmdFill());			// 1
        addCmd(new CmdTrim());			// 1
        addCmd(new CmdBypass());		// 1
        addCmd(new CmdBypasslist());	// 1
        //-----
        addCmd(new CmdKnockback());		// 1
        addCmd(new CmdWrap());			// 1
        addCmd(new CmdWhoosh());		// 1
        addCmd(new CmdGetmsg());		// 1
        addCmd(new CmdSetmsg());		// 1
        addCmd(new CmdWshape());		// 3
        //-----
        addCmd(new CmdPreventPlace());	// 1
        addCmd(new CmdPreventSpawn());	// 1
        addCmd(new CmdDelay());			// 1
        addCmd(new CmdDynmap());		// 1
        addCmd(new CmdDynmapmsg());		// 1
        addCmd(new CmdRemount());		// 1
        addCmd(new CmdFillautosave());	// 1
        addCmd(new CmdDenypearl());		// 1
        //-----
        addCmd(new CmdReload());		// 1

        // this is the default command, which shows command example pages; should be last just in case
        addCmd(new CmdCommands());
    }

    private void addCmd(WBCmd cmd)
    {
        subCommands.put(cmd.name, cmd);
        if (cmd.hasWorldNameInput)
            subCommandsWithWorldNames.add(cmd.name);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] split) throws CommandException {
        EntityPlayerMP player = sender instanceof EntityPlayerMP
            ? (EntityPlayerMP) sender
            : null;

        ArrayList<String> params = Lists.newArrayList(split);

        String worldName = null;
        // is second parameter the command and first parameter a world name?
        if (params.size() > 1 && !subCommands.containsKey(params.get(0)) && subCommandsWithWorldNames.contains(params.get(1)))
            worldName = params.get(0);

        // no command specified? show command examples / help
        if (params.isEmpty())
            params.add(0, "commands");

        // determined the command name
        String cmdName = (worldName == null) ? params.get(0).toLowerCase() : params.get(1).toLowerCase();

        // remove command name and (if there) world name from front of param array
        params.remove(0);
        if (worldName != null)
            params.remove(0);

        // make sure command is recognized, default to showing command examples / help if not; also check for specified page number
        if (!subCommands.containsKey(cmdName))
        {
            int page = (player == null) ? 0 : 1;
            try
            {
                page = Integer.parseInt(cmdName);
            }
            catch(NumberFormatException ignored)
            {
                Util.chat(sender, WBCmd.C_ERR + "Command not recognized. Showing command list.");
            }
            cmdName = "commands";
            params.add(0, Integer.toString(page));
        }

        WBCmd subCommand = subCommands.get(cmdName);

        // if command requires world name when run by console, make sure that's in place
        if (player == null && subCommand.hasWorldNameInput && subCommand.consoleRequiresWorldName && worldName == null)
        {
            Util.chat(sender, WBCmd.C_ERR + "This command requires a world to be specified if run by the console.");
            subCommand.sendCmdHelp(sender);
            return;
        }

        // make sure valid number of parameters has been provided
        if (params.size() < subCommand.minParams || params.size() > subCommand.maxParams)
        {
            if (subCommand.maxParams == 0)
                Util.chat(sender, WBCmd.C_ERR + "This command does not accept any parameters.");
            else
                Util.chat(sender, WBCmd.C_ERR + "You have not provided a valid number of parameters.");
            subCommand.sendCmdHelp(sender);
            return;
        }

        // execute command
        subCommand.execute(sender, player, params, worldName);
    }

    public ArrayList<String> getCommandNames()
    {
        if (subCommandNames != null)
            return subCommandNames;

        subCommandNames = new ArrayList<>( subCommands.keySet() );
        // Remove "commands" as it's not normally shown or run like other commands
        subCommandNames.remove("commands");
        Collections.sort(subCommandNames);

        return subCommandNames;
    }

    @Override
    public String getCommandName()
    {
        return NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/wborder help [n]";
    }

    @Override
    public List getCommandAliases()
    {
        return ALIASES;
    }

    @Override
    public boolean isUsernameIndex(String[] args, int idx)
    {
        return false;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        if (sender instanceof DedicatedServer)
            return true;

        EntityPlayerMP   player  = (EntityPlayerMP) sender;
        GameProfile      profile = player.getGameProfile();
        UserListOpsEntry opEntry = (UserListOpsEntry) WorldBorder.SERVER
            .getPlayerList()
            .getOppedPlayers()
            .getEntry(profile);

        // Level 2 (out of 4) have general access to game-changing commands
        // TODO: Make this a configuration option
        return opEntry != null && opEntry.getPermissionLevel() > 2;
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        if (args.length <= 1)
            return CommandBase.getListOfStringsMatchingLastWord(args, getCommandNames());

        String[] players = WorldBorder.SERVER.getAllUsernames();
        return CommandBase.getListOfStringsMatchingLastWord(args, players);
    }

    @Override
    public int compareTo(ICommand o) {
        return o.getCommandName().compareTo( getCommandName() );
    }
}
