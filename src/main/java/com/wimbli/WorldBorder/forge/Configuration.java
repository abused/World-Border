package com.wimbli.WorldBorder.forge;

import java.io.File;
import java.util.Set;

/**
 * More convenient and compatible version of the Forge configuration class
 */
public class Configuration extends net.minecraftforge.common.config.Configuration
{
    /** Creates convenient config with case-sensitive categories */
    public Configuration(File file)
    {
        super(file, true);
    }

    /** Shortcut for getting a string from a key and category */
    public String getString(String category, String key, String defValue)
    {
        return getString(key, category, defValue, "");
    }

    /** Shortcut for getting a boolean from a key and category */
    public boolean getBoolean(String category, String key, boolean defValue)
    {
        return getBoolean(key, category, defValue, "");
    }

    /** Shortcut for getting a float from a key and category */
    public float getFloat(String category, String key, float defValue)
    {
        return getFloat(key, category, defValue, 0, Float.MAX_VALUE, "");
    }

    /** Shortcut for getting an int from a key and category */
    public int getInt(String category, String key, int defValue)
    {
        return getInt(key, category, defValue, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
    }

    /** Shortcut for getting a string array from a key and category */
    public String[] getStringList(String category, String key)
    {
        return getStringList(key, category, new String[0], "");
    }

    /** Shortcut for setting a boolean to a key and category */
    public void set(String category, String key, boolean value)
    {
        get(category, key, value).set(value);
    }

    /** Shortcut for setting a string to a key and category */
    public void set(String category, String key, String value)
    {
        get(category, key, value).set(value);
    }

    /** Shortcut for setting an integer to a key and category */
    public void set(String category, String key, int value)
    {
        get(category, key, value).set(value);
    }

    /** Shortcut for setting a double to a key and category */
    public void set(String category, String key, double value)
    {
        get(category, key, value).set(value);
    }

    /** Shortcut for setting a float to a key and category */
    public void set(String category, String key, float value)
    {
        get(category, key, value).set(value);
    }

    /** Shortcut for setting a string array to a key and category */
    public void set(String category, String key, String[] values)
    {
        get(category, key, values).set(values);
    }

    /** Removes a category by given name */
    public void removeCategory(String category)
    {
        removeCategory(getCategory(category));
    }

    /** Clears this configuration of all its data */
    public void clear()
    {
        Set<String> categories = getCategoryNames();

        for(String category : categories)
            removeCategory( getCategory(category) );
    }
}
