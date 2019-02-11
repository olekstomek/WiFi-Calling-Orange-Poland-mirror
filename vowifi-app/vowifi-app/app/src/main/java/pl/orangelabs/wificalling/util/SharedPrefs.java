/*
 * Copyright (C) 2017 Orange Polska SA
 *
 * This file is part of WiFi Calling.
 *
 * WiFi Calling is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  WiFi Calling is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty o
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package pl.orangelabs.wificalling.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import pl.orangelabs.log.Log;

public class SharedPrefs
{
    public static final String KEY_CERT_ALIAS = "KEY_CERT_ALIAS";
    public static final String KEY_CERT_ACCEPTED = "KEY_CERT_ACCEPTED";
    public static final String KEY_SETTINGS_DIVIDER_MODE = "KEY_SETTINGS_DIVIDER_MODE";
    public static final String KEY_SETTINGS_CONTACT_SORTING_MODE = "KEY_SETTINGS_CONTACT_SORTING_MODE";
    public static final String KEY_SETTINGS_CONTACT_DISPLAY_MODE = "KEY_SETTINGS_CONTACT_DISPLAY_MODE";
    private static final String PREFS = "vowifi_prefs";
    public static final String KEY_SERVICE_ON = "KEY_SERVICE_ON";
    public static final String KEY_VPN_TRY_COUNTER = "KEY_VPN_TRY_COUNTER";
    public static final String KEY_SKIP_PROTECTED_APPS_MESSAGE = "KEY_SKIP_PROTECTED_APPS_MESSAGE";
    public static ContactDisplayMode DEFAULT_CONTACT_SORTING_MODE = ContactDisplayMode.FAMILY_NAME;
    private final SharedPreferences mPrefs;

    public SharedPrefs(final Context ctx)
    {
        mPrefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private boolean PrefsValid()
    {
        if (mPrefs == null)
        {
            Log.e(this, "Invalid preferences!");
        }

        return mPrefs != null;
    }

    public String Load(final String key, final String defaultValue)
    {
        if (PrefsValid())
        {
            final String value = mPrefs.getString(key, defaultValue);
            Log.v(this, "[Prefs loading] " + key + " => " + value);
            return value;
        }
        return defaultValue;
    }

    public boolean Load(final String key, final boolean defaultValue)
    {
        if (PrefsValid())
        {
            final boolean value = mPrefs.getBoolean(key, defaultValue);
            Log.v(this, "[Prefs loading] " + key + " => " + value);
            return value;
        }
        return defaultValue;
    }

    public int Load(final String key, final int defaultValue)
    {
        if (PrefsValid())
        {
            final int value = mPrefs.getInt(key, defaultValue);
            Log.v(this, "[Prefs loading] " + key + " => " + value);
            return value;
        }
        return defaultValue;
    }

    public long Load(final String key, final long defaultValue)
    {
        if (PrefsValid())
        {
            final long value = mPrefs.getLong(key, defaultValue);
            Log.v(this, "[Prefs loading] " + key + " => " + value);
            return value;
        }
        return defaultValue;
    }

    public <T extends Enum<T>> T LoadEnum(final String key, @NonNull final T defaultValue)
    {
        if (PrefsValid())
        {
            final int value = mPrefs.getInt(key, -1);
            if (value < 0)
            {
                return defaultValue;
            }

            try
            {
                return (T) defaultValue.getClass().getEnumConstants()[value];
            }
            catch (final Exception ex)
            {
                Log.w(this, "Could not load enum from key: " + key, ex);
            }
        }
        return defaultValue;
    }

    /**
     * tries to convert boolean stored in prefs to given enum (indexing false as 0, true as 1);
     * <p>
     * convenience method to be able to use named settings instead of arbitrary true/false
     *
     * @param key
     * @param defaultValue
     * @param <T>
     * @return
     */
    public <T extends Enum<T>> T LoadEnumFromBool(final String key, @NonNull final T defaultValue)
    {
        if (PrefsValid())
        {
            final boolean value = mPrefs.getBoolean(key, defaultValue.ordinal() > 0);

            try
            {
                return (T) defaultValue.getClass().getEnumConstants()[value ? 1 : 0];
            }
            catch (final Exception ex)
            {
                Log.w(this, "Could not load enum from key: " + key, ex);
            }
        }
        return defaultValue;
    }

    public void Save(final String key, final String value)
    {
        if (PrefsValid())
        {
            Log.v(this, "[Prefs saving] " + key + " => " + value);
            final SharedPreferences.Editor spe = mPrefs.edit();
            spe.putString(key, value);
            spe.apply();
        }
    }

    public void Save(final String key, final boolean value)
    {
        if (PrefsValid())
        {
            Log.v(this, "[Prefs saving] " + key + " => " + value);
            final SharedPreferences.Editor spe = mPrefs.edit();
            spe.putBoolean(key, value);
            spe.apply();
        }
    }

    public void Save(final String key, final int value)
    {
        if (PrefsValid())
        {
            Log.v(this, "[Prefs saving] " + key + " => " + value);
            final SharedPreferences.Editor spe = mPrefs.edit();
            spe.putInt(key, value);
            spe.apply();
        }
    }

    public void Save(final String key, final long value)
    {
        if (PrefsValid())
        {
            Log.v(this, "[Prefs saving] " + key + " => " + value);
            final SharedPreferences.Editor spe = mPrefs.edit();
            spe.putLong(key, value);
            spe.apply();
        }
    }

    public <T extends Enum<T>> void SaveEnum(final String key, @NonNull final T value)
    {
        if (PrefsValid())
        {
            Log.v(this, "[Prefs saving] " + key + " => " + value);
            final SharedPreferences.Editor spe = mPrefs.edit();
            spe.putInt(key, value.ordinal());
            spe.apply();
        }
    }

    public void Clear()
    {
        if (PrefsValid())
        {
            mPrefs.edit().clear().apply();
            Log.v(this, "[Prefs cleared]");
        }
    }

    public void Clear(final String key)
    {
        if (PrefsValid())
        {
            mPrefs.edit().remove(key).apply();
            Log.v(this, "[Prefs clearing] " + key);
        }
    }

    public enum ConnectionsDividerMode
    {
        APPROXIMATE,
        DETAILED
    }

    public enum ContactDisplayMode
    {
        FAMILY_NAME,
        GIVEN_NAME
    }

    public static class Defaults
    {
        public static final ConnectionsDividerMode DIVIDER_MODE = ConnectionsDividerMode.APPROXIMATE;
        public static final ContactDisplayMode CONTACT_DISPLAY_MODE = ContactDisplayMode.GIVEN_NAME;
        public static final ContactDisplayMode CONTACT_SORTING_MODE = ContactDisplayMode.GIVEN_NAME;
        public static final boolean DEFAULT_SERVICE_ON = false;
        public static final boolean SKIP_PROTECTED_APPS_MESSAGE = false;
        public static final int DEFAULT_VPN_COUNTER = 0;
    }
}
