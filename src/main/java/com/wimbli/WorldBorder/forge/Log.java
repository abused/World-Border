package com.wimbli.WorldBorder.forge;

import com.wimbli.WorldBorder.WorldBorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static utility class for WorldBorder logging.
 *
 * To enable debug logging for WorldBorder:
 * 1. Save https://gist.github.com/RoyCurtis/517dd9d6a0619c44e970 to debug directory
 * 2. Add `-Dlog4j.configurationFile=log4j.xml` to VM options when running server
 */
public class Log
{
    private static final Logger LOG = LogManager.getFormatterLogger(WorldBorder.MODID);

    // <editor-fold desc="Emitters">
    public static void trace(String msg, Object... parts)
    {
        LOG.trace(msg, parts);
    }

    public static void debug(String msg, Object... parts)
    {
        LOG.debug(msg, parts);
    }

    public static void info(String msg, Object... parts)
    {
        LOG.info(msg, parts);
    }

    public static void warn(String msg, Object... parts)
    {
        LOG.warn(msg, parts);
    }

    public static void error(String msg, Object... parts)
    {
        LOG.error(msg, parts);
    }
    // </editor-fold>
}
