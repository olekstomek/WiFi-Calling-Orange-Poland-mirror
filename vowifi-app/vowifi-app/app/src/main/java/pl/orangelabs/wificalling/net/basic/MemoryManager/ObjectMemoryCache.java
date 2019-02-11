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

package pl.orangelabs.wificalling.net.basic.MemoryManager;

import java.util.HashMap;
import java.util.Map;

import pl.orangelabs.log.Log;

/**
 * keeps downloaded data in memory for faster access than db
 *
 * @author F
 */
public class ObjectMemoryCache
{
    /**
     * map-like object to keep our data
     */
    private final Map<String, Object> mObjectCache;

    private ObjectMemoryCache()
    {
        mObjectCache = new HashMap<>();
    }

    /**
     * singleton
     */
    public static ObjectMemoryCache Me()
    {
        return MemoryCacheHolder.INSTANCE;
    }

    /**
     * saves given object to memory cache (it will replace another object of the same type if it exists)
     *
     * @param object data that we want to save
     */
    public synchronized <T> void Save(final String key, final T object)
    {
        Log.v(this, "Saving memory cache for key:" + key + "): " + object);
        mObjectCache.put(key, object);
    }

    /**
     * loads object of given type; this unchecked cast is totally safe, because we only save objects based on saved object class
     *
     * @param key identifier of saved data
     * @return requested data object or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T Load(final String key)
    {
        final T data = (T) mObjectCache.get(key);
        Log.v(this, "Loading memory cache for class " + key + ": " + data);
        return data;
    }

    public synchronized void Clear(final String key)
    {
        Log.v(this, "Deleting cache for key " + key);
        mObjectCache.remove(key);
    }

    public boolean Contains(final String key)
    {
        return mObjectCache.containsKey(key);
    }

    public synchronized void Clear()
    {
        Log.v(this, "Cleared memory cache");
        mObjectCache.clear();
    }

    private static final class MemoryCacheHolder
    {
        private static final ObjectMemoryCache INSTANCE = new ObjectMemoryCache();
    }
}
