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

package pl.orangelabs.wificalling.model.db;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;

import pl.orangelabs.log.Log;

public class AsyncMarkAllCallsAsRead extends AsyncTask<Void, Void, Void>
{
    private Context mCtx;

    public AsyncMarkAllCallsAsRead(final Context ctx)
    {
        mCtx = ctx;
    }

    @Override
    protected final Void doInBackground(final Void... params)
    {
        markAllCallsAsRead(mCtx);
        return null;
    }

    private void markAllCallsAsRead(final Context ctx)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CallLog.Calls.NEW,0);
        int result =0 ;
        try
        {
            if (ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            result = ctx.getContentResolver().update(CallLog.Calls.CONTENT_URI, contentValues, CallLog.Calls.NEW + " = '1'", null);
        }
        catch (Exception e)
        {
            Log.w(this, "error during update calls", e);
        }
        Log.i(this, "update row :" + result);
    }
}