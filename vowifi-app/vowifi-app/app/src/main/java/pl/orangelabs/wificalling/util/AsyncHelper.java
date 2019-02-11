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

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author F
 *         <p/>
 *         small helper class to simplify executing given async tasks simultaneously (on shared thread pool executor) instead of queuing them (default
 *         behavior on newer apis)
 */
public final class AsyncHelper
{
    private static ExecutorService sExecutorService;

    static
    {
        final int threads = Math.max(Runtime.getRuntime().availableProcessors() * 2, 1);
        sExecutorService = Executors.newFixedThreadPool(threads, new CustomExecutorThreadFactory());
    }

    @SafeVarargs
    public static <T, U, V> void execute(final AsyncTask<T, U, V> task, T... params)
    {
        task.executeOnExecutor(sExecutorService, params);
    }

    private static class CustomExecutorThreadFactory implements ThreadFactory
    {
        @Override
        public Thread newThread(@NonNull final Runnable r)
        {
            return new Thread(r, "OW async downloader");
        }
    }
}
