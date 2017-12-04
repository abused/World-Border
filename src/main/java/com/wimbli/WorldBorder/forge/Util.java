package com.wimbli.WorldBorder.forge;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;

/** Static utility class for shortcut methods to help transition from Bukkit to Forge */
public class Util
{
    /**
     * Attempts to a translate a given string/key using the local language, and then
     * using the fallback language
     * @param msg String or language key to translate
     * @return Translated or same string
     */
    public static String translate(String msg)
    {
        return I18n.canTranslate(msg)
            ? I18n.translateToLocal(msg)
            : I18n.translateToFallback(msg);
    }

    /**
     * Sends an automatically translated and formatted message to a command sender
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     */
    public static void chat(ICommandSender sender, String msg, Object... parts)
    {
        String translated = translate(msg);

        // Consoles require ANSI coloring for formatting
        if (sender instanceof DedicatedServer)
            Log.info( removeFormatting(translated), parts );
        else
        {
            translated = String.format(translated, parts);
            sender.addChatMessage( new TextComponentString(translated) );
        }
    }

    /** Replaces Bukkit-convention amp format tokens with vanilla ones */
    public static String replaceAmpColors(String message)
    {
        return message.replaceAll("(?i)&([a-fk-or0-9])", "§$1");
    }

    /** Strips vanilla formatting from a string */
    public static String removeFormatting(String message)
    {
        return message.replaceAll("(?i)§[a-fk-or0-9]", "");
    }

    /** Shortcut for java.lang.System.currentTimeMillis */
    public static long now()
    {
        return System.currentTimeMillis();
    }
}