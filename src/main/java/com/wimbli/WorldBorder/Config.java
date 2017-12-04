package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Configuration;
import com.wimbli.WorldBorder.forge.Log;
import com.wimbli.WorldBorder.forge.Util;
import com.wimbli.WorldBorder.task.BorderCheckTask;
import com.wimbli.WorldBorder.task.WorldFillTask;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * Static class for holding, loading and saving global and per-border data
 */
public class Config
{
    public static final DecimalFormat COORD_FORMAT = new DecimalFormat("0.0");

    private static final String MAIN_CAT = "general";
    private static final String FILL_CAT = "fillTask";

    // TODO: move this elsewhere?
    public static BorderCheckTask borderTask = null;

    private static File          configDir;
    private static Configuration cfgMain;
    private static Configuration cfgBorders;

    // actual configuration values which can be changed
    private static Map<String, BorderData> borders = new LinkedHashMap<>();

    /** Knockback message without formatting */
    private static String    message;
    /** Knockback message with formatting */
    private static String    messageFmt;
    private static String    dynmapMessage;
    private static Set<UUID> bypassPlayers     = new LinkedHashSet<>();
    private static boolean   shapeRound        = true;
    private static float     knockBack         = 3.0F;
    private static int       timerTicks        = 20;
    private static boolean   whooshEffect      = true;
    private static boolean   dynmapEnable      = true;
    private static boolean   remount           = true;
    private static boolean   killPlayer        = false;
    private static boolean   denyEnderpearl    = false;
    private static boolean   preventBlockPlace = false;
    private static boolean   preventMobSpawn   = false;

    private static int fillAutosaveFrequency = 30;
    private static int fillMemoryTolerance   = 200;

    public static void setupConfigDir(File globalDir)
    {
        configDir = new File(globalDir, WorldBorder.MODID);

        if ( !configDir.exists() && configDir.mkdirs() )
            Log.info("Created config directory for the first time");
    }

    public static void setBorder(String world, BorderData border, boolean logIt)
    {
        borders.put(world, border);
        if (logIt)
            Log.info("Border set. " + BorderDescription(world));
        save();
        DynMapFeatures.showBorder(world, border);
    }

    public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
    {
        BorderData old = Border(world);
        Boolean oldShape = (old == null) ? null : old.getShape();
        boolean oldWrap = (old != null) && old.getWrapping();
        setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, oldWrap), true);
    }

    // set border based on corner coordinates
    public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound, boolean wrap)
    {
        double radiusX = Math.abs(x1 - x2) / 2;
        double radiusZ = Math.abs(z1 - z2) / 2;
        double x = ((x1 < x2) ? x1 : x2) + radiusX;
        double z = ((z1 < z2) ? z1 : z2) + radiusZ;
        setBorder(world, new BorderData(x, z, (int) Math.round(radiusX), (int) Math.round(radiusZ), shapeRound, wrap), true);
    }

    public static void setBorderCorners(String world, double x1, double z1, double x2, double z2)
    {
        BorderData old = Border(world);
        Boolean oldShape = (old == null) ? null : old.getShape();
        boolean oldWrap = (old != null) && old.getWrapping();
        setBorderCorners(world, x1, z1, x2, z2, oldShape, oldWrap);
    }

    public static void removeBorder(String world)
    {
        borders.remove(world);
        Log.info("Removed border for world \"" + world + "\".");
        save();
        DynMapFeatures.removeBorder(world);
    }

    public static void removeAllBorders()
    {
        borders.clear();
        Log.info("Removed all borders for all worlds.");
        save();
        DynMapFeatures.removeAllBorders();
    }

    public static String BorderDescription(String world)
    {
        BorderData border = borders.get(world);

        return border == null
            ? "No border was found for the world \"" + world + "\"."
            : "World \"" + world + "\" has border " + border.toString();
    }

    public static Set<String> BorderDescriptions()
    {
        Set<String> output = new HashSet<String>();

        for (String worldName : borders.keySet())
            output.add(BorderDescription(worldName));

        return output;
    }

    public static BorderData Border(String world)
    {
        return borders.get(world);
    }

    public static Map<String, BorderData> getBorders()
    {
        return new LinkedHashMap<String, BorderData>(borders);
    }

    public static void setMessage(String msg)
    {
        updateMessage(msg);
        save();
    }

    public static void updateMessage(String msg)
    {
        message = msg;
        messageFmt = Util.replaceAmpColors(msg);
    }

    public static String getMessage()
    {
        return messageFmt;
    }

    public static String getMessageRaw()
    {
        return message;
    }

    public static void setShape(boolean round)
    {
        shapeRound = round;
        Log.info("Set default border shape to " + (getShapeName()) + ".");
        save();
        DynMapFeatures.showAllBorders();
    }

    public static boolean getShapeRound()
    {
        return shapeRound;
    }

    public static String getShapeName()
    {
        return getShapeName(shapeRound);
    }

    public static String getShapeName(Boolean round)
    {
        if (round == null)
            return "default";
        return round ? "elliptic/round" : "rectangular/square";
    }

    public static void setWhooshEffect(boolean enable)
    {
        whooshEffect = enable;
        Log.info("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + ".");
        save();
    }

    public static boolean doWhooshEffect()
    {
        return whooshEffect;
    }

    public static void setPreventBlockPlace(boolean enable)
    {
        if (preventBlockPlace != enable)
            WorldBorder.INSTANCE.enableBlockPlaceListener(enable);

        preventBlockPlace = enable;
        Log.info("Prevent block place " + (enable ? "enabled" : "disabled") + ".");
        save();
    }

    public static void setPreventMobSpawn(boolean enable)
    {
        if (preventMobSpawn != enable)
            WorldBorder.INSTANCE.enableMobSpawnListener(enable);

        preventMobSpawn = enable;
        Log.info("Prevent mob spawn " + (enable ? "enabled" : "disabled") + ".");
        save();
    }

    public static boolean preventBlockPlace()
    {
        return preventBlockPlace;
    }

    public static boolean preventMobSpawn()
    {
        return preventMobSpawn;
    }

    public static boolean doPlayerKill()
    {
        return killPlayer;
    }

    public static boolean getDenyEnderpearl()
    {
        return denyEnderpearl;
    }

    public static void setDenyEnderpearl(boolean enable)
    {
        if (denyEnderpearl != enable)
            WorldBorder.INSTANCE.enableEnderPearlListener(enable);

        denyEnderpearl = enable;
        Log.info("Direct cancellation of ender pearls thrown past the border " + (enable ? "enabled" : "disabled") + ".");
        save();
    }

    public static void setKnockBack(float numBlocks)
    {
        knockBack = numBlocks;
        Log.info("Knockback set to " + knockBack + " blocks inside the border.");

        if (knockBack == 0.0)
            stopBorderTimer();
        else
            startBorderTimer();

        save();
    }

    public static double getKnockBack()
    {
        return knockBack;
    }

    public static void setTimerTicks(int ticks)
    {
        timerTicks = ticks;
        Log.info("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
        startBorderTimer();
        save();
    }

    public static int getTimerTicks()
    {
        return timerTicks;
    }

    public static void setRemount(boolean enable)
    {
        remount = enable;
        if (remount)
            Log.info("Remount is now enabled. Players will be remounted on their vehicle when knocked back");
        else
            Log.info("Remount is now disabled. Players will be left dismounted when knocked back from the border while on a vehicle.");

        save();
    }

    public static boolean getRemount()
    {
        return remount;
    }

    public static void setFillAutosaveFrequency(int seconds)
    {
        fillAutosaveFrequency = seconds;
        if (fillAutosaveFrequency == 0)
            Log.info("World autosave frequency during Fill process set to 0, disabling it. Note that much progress can be lost this way if there is a bug or crash in the world generation process from Bukkit or any world generation plugin you use.");
        else
            Log.info("World autosave frequency during Fill process set to " + fillAutosaveFrequency + " seconds (rounded to a multiple of 5). New chunks generated by the Fill process will be forcibly saved to disk this often to prevent loss of progress due to bugs or crashes in the world generation process.");
        save();
    }

    public static int getFillAutosaveFrequency()
    {
        return fillAutosaveFrequency;
    }


    public static void setDynmapBorderEnabled(boolean enable)
    {
        dynmapEnable = enable;
        Log.info("DynMap border display is now " + (enable ? "enabled" : "disabled") + ".");
        save();
        DynMapFeatures.showAllBorders();
    }

    public static boolean isDynmapBorderEnabled()
    {
        return dynmapEnable;
    }

    public static void setDynmapMessage(String msg)
    {
        dynmapMessage = msg;
        Log.info("DynMap border label is now set to: " + msg);
        save();
        DynMapFeatures.showAllBorders();
    }

    public static String getDynmapMessage()
    {
        return dynmapMessage;
    }

    public static void setPlayerBypass(UUID player, boolean bypass)
    {
        if (bypass)
            bypassPlayers.add(player);
        else
            bypassPlayers.remove(player);
        save();
    }

    public static boolean isPlayerBypassing(UUID player)
    {
        return bypassPlayers.contains(player);
    }

    public static UUID[] getPlayerBypassList()
    {
        return bypassPlayers.toArray( new UUID[ bypassPlayers.size() ] );
    }

    private static void importBypassStringList(String[] strings)
    {
        bypassPlayers.clear();
        for (String string : strings)
            bypassPlayers.add( UUID.fromString(string) );
    }

    private static String[] exportBypassStringList()
    {
        ArrayList<String> strings = new ArrayList<>();

        for (UUID uuid : bypassPlayers)
            strings.add( uuid.toString() );

        return strings.toArray(new String[strings.size()]);
    }

    public static void startBorderTimer()
    {
        if (borderTask == null)
            borderTask = new BorderCheckTask();

        if ( borderTask.isRunning() )
            return;

        borderTask.setRunning(true);
        Log.info("Border-checking timed task started.");
    }

    public static void stopBorderTimer()
    {
        if ( borderTask == null || !borderTask.isRunning() )
            return;

        borderTask.setRunning(false);
        Log.info("Border-checking timed task stopped.");
    }

    public static void storeFillTask()
    {
        save(true);
    }

    public static void deleteFillTask()
    {
        save();
    }

    public static void restoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
    {
        if (WorldFillTask.getInstance() != null)
            return;

        try
        {
            WorldFillTask task = WorldFillTask.create(WorldBorder.SERVER, world, forceLoad, fillDistance, chunksPerRun, tickFrequency);
            task.startFrom(x, z, length, total);
        }
        catch (Exception e)
        {
            Log.warn("Could not resume fill task: " + e.getMessage());
        }
    }

    public static long getAvailableMemory()
    {
        Runtime rt = Runtime.getRuntime();
        // 1024*1024 = 1048576 (bytes in 1 MB)
        return (rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576;
    }

    public static boolean isAvailableMemoryTooLow()
    {
        return getAvailableMemory() < fillMemoryTolerance;
    }

    private static final int currentCfgVersion = 11;

    public static void load(boolean logIt)
    {
        if (cfgMain == null)
            cfgMain = new Configuration( new File(configDir, "main.cfg") );
        else cfgMain.load();

        if (cfgBorders == null)
            cfgBorders = new Configuration( new File(configDir, "borders.cfg") );
        else cfgBorders.load();

        int cfgVersion = cfgMain.getInt(MAIN_CAT, "cfg-version", currentCfgVersion);

        // TODO: make all these call setters
        // WARNING: NPE because msg == null here
        String msg = cfgMain.getString(MAIN_CAT, "message", "");
        importBypassStringList(cfgMain.getStringList(MAIN_CAT, "bypass-list-uuids"));
        shapeRound        = cfgMain.getBoolean(MAIN_CAT, "round-border", true);
        whooshEffect      = cfgMain.getBoolean(MAIN_CAT, "whoosh-effect", true);
        knockBack         = cfgMain.getFloat(MAIN_CAT, "knock-back-dist", 3.0F);
        timerTicks        = cfgMain.getInt(MAIN_CAT, "timer-delay-ticks", 20);
        remount           = cfgMain.getBoolean(MAIN_CAT, "remount-on-knockback", true);
        dynmapEnable      = cfgMain.getBoolean(MAIN_CAT, "dynmap-border-enabled", true);
        dynmapMessage     = cfgMain.getString(MAIN_CAT, "dynmap-border-message", "The border of the world.");
        killPlayer        = cfgMain.getBoolean(MAIN_CAT, "player-killed-bad-spawn", false);
        denyEnderpearl    = cfgMain.getBoolean(MAIN_CAT, "deny-enderpearl", true);
        preventBlockPlace = cfgMain.getBoolean(MAIN_CAT, "prevent-block-place", true);
        preventMobSpawn   = cfgMain.getBoolean(MAIN_CAT, "prevent-mob-spawn", true);

        fillAutosaveFrequency = cfgMain.getInt(MAIN_CAT, "fill-autosave-frequency", fillAutosaveFrequency);
        fillMemoryTolerance   = cfgMain.getInt(MAIN_CAT, "fill-memory-tolerance", fillMemoryTolerance);

        Log.info("Using " + (getShapeName()) + " border, knockback of " + knockBack + " blocks, and timer delay of " + timerTicks + ".");

        // TODO: Move to server setup?
        if (knockBack == 0.0)
        {
            Log.warn("Knockback is set to 0; the border check task will be disabled.");
            stopBorderTimer();
        }
        else
            startBorderTimer();

        borders.clear();

        // if empty border message, assume no config
        if (msg == null || msg.isEmpty())
        {	// store defaults
            Log.info("Configuration not present, creating new file.");
            msg = "&cYou have reached the edge of this world.";
            updateMessage(msg);
            save();
            return;
        }
        // otherwise just set border message
        else
            updateMessage(msg);

        Set<String> worldNames = cfgBorders.getCategoryNames();

        for(String worldName : worldNames)
        {
            // backwards compatibility for config from before elliptical/rectangular borders were supported
            if (cfgBorders.hasKey(worldName, "radius") && !cfgBorders.hasKey(worldName, "radiusX"))
            {
                int radius = cfgBorders.get(worldName, "radius", 0).getInt();
                cfgBorders.set(worldName, "radiusX", radius);
                cfgBorders.set(worldName, "radiusZ", radius);
            }

            Boolean overrideShape = cfgBorders.hasKey(worldName, "shape-round")
                ? cfgBorders.getBoolean(worldName, "shape-round", true)
                : null;

            boolean wrap = cfgBorders.get(worldName, "wrapping", false).getBoolean();
            BorderData border = new BorderData(
                cfgBorders.get(worldName, "x", 0.0D).getDouble(), cfgBorders.get(worldName, "z", 0.0D).getDouble(),
                cfgBorders.get(worldName, "radiusX", 0).getInt(), cfgBorders.get(worldName, "radiusZ", 0).getInt(),
                overrideShape, wrap
            );
            borders.put(worldName, border);
            Log.debug(BorderDescription(worldName));
        }

        // if we have an unfinished fill task stored from a previous run, load it up
        if ( cfgMain.hasCategory(FILL_CAT) )
        {
            // TODO: make these get from category object to provoke NPE
            String  worldName = cfgMain.get(FILL_CAT, "world", "").getString();
            boolean forceLoad = cfgMain.get(FILL_CAT, "forceLoad", false).getBoolean();

            int fillDistance  = cfgMain.get(FILL_CAT, "fillDistance", 176).getInt();
            int chunksPerRun  = cfgMain.get(FILL_CAT, "chunksPerRun", 5).getInt();
            int tickFrequency = cfgMain.get(FILL_CAT, "tickFrequency", 20).getInt();
            int fillX         = cfgMain.get(FILL_CAT, "x", 0).getInt();
            int fillZ         = cfgMain.get(FILL_CAT, "z", 0).getInt();
            int fillLength    = cfgMain.get(FILL_CAT, "length", 0).getInt();
            int fillTotal     = cfgMain.get(FILL_CAT, "total", 0).getInt();

            restoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal, forceLoad);
            save();
        }

        if (logIt)
            Log.info("Configuration loaded.");

        if (cfgVersion < currentCfgVersion) save();
    }

    public static void save()
    {
        save(false);
    }
    public static void save(boolean storeFillTask)
    {	// save config to file
        if (cfgMain == null) return;

        cfgMain.set(MAIN_CAT, "cfg-version", currentCfgVersion);
        cfgMain.set(MAIN_CAT, "message", message);
        cfgMain.set(MAIN_CAT, "round-border", shapeRound);
        cfgMain.set(MAIN_CAT, "whoosh-effect", whooshEffect);
        cfgMain.set(MAIN_CAT, "knock-back-dist", knockBack);
        cfgMain.set(MAIN_CAT, "timer-delay-ticks", timerTicks);
        cfgMain.set(MAIN_CAT, "remount-on-knockback", remount);
        cfgMain.set(MAIN_CAT, "dynmap-border-enabled", dynmapEnable);
        cfgMain.set(MAIN_CAT, "dynmap-border-message", dynmapMessage);
        cfgMain.set(MAIN_CAT, "player-killed-bad-spawn", killPlayer);
        cfgMain.set(MAIN_CAT, "deny-enderpearl", denyEnderpearl);
        cfgMain.set(MAIN_CAT, "fill-autosave-frequency", fillAutosaveFrequency);
        cfgMain.set(MAIN_CAT, "bypass-list-uuids", exportBypassStringList());
        cfgMain.set(MAIN_CAT, "fill-memory-tolerance", fillMemoryTolerance);
        cfgMain.set(MAIN_CAT, "prevent-block-place", preventBlockPlace);
        cfgMain.set(MAIN_CAT, "prevent-mob-spawn", preventMobSpawn);

        cfgBorders.clear();
        for ( Entry<String, BorderData> stringBorderDataEntry : borders.entrySet() )
        {
            String     name = stringBorderDataEntry.getKey();
            BorderData bord = stringBorderDataEntry.getValue();

            cfgBorders.set(name, "x", bord.getX());
            cfgBorders.set(name, "z", bord.getZ());
            cfgBorders.set(name, "radiusX", bord.getRadiusX());
            cfgBorders.set(name, "radiusZ", bord.getRadiusZ());
            cfgBorders.set(name, "wrapping", bord.getWrapping());

            // No need to remove shape-round since cfgBorders is cleared
            if (bord.getShape() != null)
                cfgBorders.set(name, "shape-round", bord.getShape());
        }

        WorldFillTask fillTask = WorldFillTask.getInstance();
        if (storeFillTask && fillTask != null)
        {
            cfgMain.set(FILL_CAT, "world", fillTask.getWorld());
            cfgMain.set(FILL_CAT, "fillDistance", fillTask.getFillDistance());
            cfgMain.set(FILL_CAT, "chunksPerRun", fillTask.getChunksPerRun());
            cfgMain.set(FILL_CAT, "tickFrequency", fillTask.getTickFrequency());
            cfgMain.set(FILL_CAT, "x", fillTask.getRefX());
            cfgMain.set(FILL_CAT, "z", fillTask.getRefZ());
            cfgMain.set(FILL_CAT, "length", fillTask.getRefLength());
            cfgMain.set(FILL_CAT, "total", fillTask.getRefTotal());
            cfgMain.set(FILL_CAT, "forceLoad", fillTask.getForceLoad());
        }
        else
            cfgMain.removeCategory("fillTask");

        cfgMain.save();
        cfgBorders.save();
        Log.debug("Configuration saved");
    }


}
