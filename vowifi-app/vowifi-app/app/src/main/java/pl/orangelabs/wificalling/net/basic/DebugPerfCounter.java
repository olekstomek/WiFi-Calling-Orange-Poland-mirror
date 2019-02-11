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

package pl.orangelabs.wificalling.net.basic;

import android.util.SparseArray;

import pl.orangelabs.wificalling.util.Settings;

/**
 * @author F
 */
public final class DebugPerfCounter
{
    private static final SparseArray<Long> DATA = new SparseArray<>();

    private DebugPerfCounter()
    {
    }

    public static void Start(final Object o)
    {
        if (!Settings.LOGGING_ENABLED)
        {
            return;
        }
        synchronized (DATA)
        {
            DATA.put(o.hashCode(), System.currentTimeMillis());
        }
    }

    public static void End(final Object o, final String what)
    {
        if (!Settings.LOGGING_ENABLED)
        {
            return;
        }
        final int hash = o.hashCode();
        synchronized (DATA)
        {
            final Long startTime = DATA.get(hash);
            if (startTime != null)
            {
                DATA.remove(hash);
                android.util.Log.v(o.getClass().getSimpleName(), "[perf] " + what + " took " + (System.currentTimeMillis() - startTime) + "ms");
            }
        }
    }
}
