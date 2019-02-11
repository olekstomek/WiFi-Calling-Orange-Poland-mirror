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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import pl.orangelabs.log.Log;

/**
 * @author F
 */
public class StringUtil
{
    private static String sVersion;
    public static String Captitalize(final String in)
    {
        if (in == null)
        {
            return null;
        }
        switch (in.length())
        {
            case 0:
                return in;
            case 1:
                return in.toUpperCase(Locale.US);
            default:
                return in.substring(0, 1).toUpperCase(Locale.US) + in.substring(1);
        }
    }

    public static String StreamToString(final java.io.InputStream is)
    {
        try (final java.util.Scanner s = new java.util.Scanner(is, Utf8String()).useDelimiter("\\A"))
        {
            return s.hasNext()
                   ? s.next()
                   : "";
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Charset Utf8()
    {
        return StandardCharsets.UTF_8;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String Utf8String()
    {
        return StandardCharsets.UTF_8.name();
    }

    public static String AppVersion(final Context ctx)
    {
        try
        {
            if (sVersion == null || sVersion.isEmpty())
            {
                if (ctx != null)
                {
                    final PackageManager pm = ctx.getPackageManager();
                    if (pm != null)
                    {
                        final PackageInfo packageInfo = pm.getPackageInfo(ctx.getPackageName(), 0);
                        sVersion = packageInfo.versionName;
                        return sVersion;
                    }
                }
            }
            else
            {
                return sVersion;
            }
        }
        catch (final android.content.pm.PackageManager.NameNotFoundException e)
        {
            Log.w(ctx, "Could not retrieve app version name", e);
        }
        return "";
    }
}
