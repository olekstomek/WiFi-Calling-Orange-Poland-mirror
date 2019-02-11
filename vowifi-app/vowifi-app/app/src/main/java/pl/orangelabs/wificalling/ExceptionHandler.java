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

package pl.orangelabs.wificalling;

import android.content.Context;
import android.os.Build;

import java.lang.ref.WeakReference;

import pl.orangelabs.log.Log;
import pl.orangelabs.log.LogOperations;
import pl.orangelabs.log.LogType;

/**
 * @author F
 */
public class ExceptionHandler implements Thread.UncaughtExceptionHandler
{
    private final Thread.UncaughtExceptionHandler mPrevHandler;
    private final WeakReference<Context> mApplicationContext;

    private ExceptionHandler(final Thread.UncaughtExceptionHandler prevHandler, final Context applicationContext)
    {
        mPrevHandler = prevHandler;
        mApplicationContext = new WeakReference<>(applicationContext);
    }

    public static void Setup(final Context context)
    {
        final Thread.UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(exceptionHandler instanceof ExceptionHandler)) // ignore if our handler is already hooked
        {
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(exceptionHandler, context));
        }
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex)
    {
        Log.i(this, "crash\n + DEVICE: " + Build.BRAND
                    + "; Device: " + Build.DEVICE
                    + "; Model: " + Build.MODEL
                    + "; Prod: " + Build.PRODUCT);

        Log.i(this, "crash\n" +
                    "SDK: " + Build.VERSION.SDK_INT
                    + "; Release: " + Build.VERSION.RELEASE
                    + "; Inc: " + Build.VERSION.INCREMENTAL);

        Log.e(this, "crash", ex);

        LogOperations.SaveToFile(LogType.CRASH);
        LogOperations.SendToServer(LogType.CRASH);

        mPrevHandler.uncaughtException(thread, ex); // (this should be comscore-overridden handler, so we don't want to System.exit directly here)
    }
}
