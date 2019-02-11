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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import pl.orangelabs.log.Log;

/**
 * keeps downloaded data in memory for faster access than db; allows one instance per class to be saved (this might be limiting in some cases, but
 * generally for our data it fits quite well -- we usually need only one instance of each data type)
 *
 * @author F
 */
public final class MemoryCache
{
    /**
     * map-like object to keep our data
     */
    private final SparseArray<Object> mObjectCache;

    private MemoryCache()
    {
        mObjectCache = new SparseArray<>();
    }

    /**
     * singleton
     */
    public static MemoryCache Me()
    {
        return MemoryCacheHolder.INSTANCE;
    }

    /**
     * saves given object to memory cache (it will replace another object of the same type if it exists)
     *
     * @param object
     *     data that we want to save
     */
    public synchronized <T> void Save(@NonNull final T object)
    {
        Log.v(this, "Saving memory cache for class " + object.getClass() + " (hash:" + object.getClass().hashCode() + ")");
        mObjectCache.put(object.getClass().hashCode(), object);
    }

    /**
     * loads object of given type; this unchecked cast is totally safe, because we only save objects based on saved object class
     *
     * @param type
     *     class of the object that we want to load
     * @return requested data object or null if it doesn't exist
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public synchronized <T> T Load(@NonNull final Class<T> type)
    {
        Log.v(this, "Loading memory cache for class " + type.getSimpleName() + " (hash:" + type.hashCode() + ")");
        return (T) mObjectCache.get(type.hashCode());
    }

    public synchronized <T> void Clear(@NonNull final Class<T> type)
    {
        Log.v(this, "Deleting cache for class " + type.getSimpleName() + " (hash:" + type.hashCode() + ")");
        mObjectCache.delete(type.hashCode());
    }

    public synchronized <T> boolean Contains(@NonNull final Class<T> type)
    {
        return mObjectCache.get(type.hashCode()) != null;
    }

    public synchronized void Clear()
    {
        Log.v(this, "Cleared memory cache");
        mObjectCache.clear();
    }

    private static final class MemoryCacheHolder
    {
        private static final MemoryCache INSTANCE = new MemoryCache();
    }

}
