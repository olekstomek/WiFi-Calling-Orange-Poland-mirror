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
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.model.VWCallLog;

public class AsyncGetCallLogByPhoneNumbers extends AsyncTask<List<PhoneNumber>, Void, List<VWCallLog>>
{
    private Context mCtx;
    private OnCallLogByContactResultCallback mResultCallback;

    public AsyncGetCallLogByPhoneNumbers(final Context ctx, final OnCallLogByContactResultCallback resultCallback)
    {
        mCtx = ctx;
        mResultCallback = resultCallback;
    }

    @SafeVarargs
    @Override
    protected final List<VWCallLog> doInBackground(final List<PhoneNumber>... params)
    {
        List<PhoneNumber> phoneNumbers = params[0];
        return GetCallLog(mCtx, phoneNumbers);
    }

    @Override
    protected void onPostExecute(final List<VWCallLog> callLogList)
    {
        mResultCallback.OnLoadCompleteListener(callLogList);
    }

    private List<VWCallLog> GetCallLog(final Context ctx, final List<PhoneNumber> phoneNumberList)
    {
        final String[] projection = new String[] {
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_NUMBER_LABEL,
            CallLog.Calls._ID};

        final StringBuilder selectionBuilder = new StringBuilder();
        final List<String> selectionArg = new ArrayList<>();

        if (phoneNumberList.isEmpty())
        {
            return null;
        }

        for (final PhoneNumber phoneNumber : phoneNumberList)
        {
            if (phoneNumber.GetNumberForSearch() == null)
            {
                continue;
            }
            if (selectionBuilder.length() != 0)
            {
                selectionBuilder.append(" OR ");
            }
            selectionBuilder.append(CallLog.Calls.NUMBER + " LIKE ?");
            selectionArg.add("%" + phoneNumber.GetNumberForSearch() + "%");
        }

        final String selection = selectionBuilder.toString();
        final String sortBy = CallLog.Calls.DATE + " DESC";
        final String[] selectionArgs = selectionArg.toArray(new String[selectionArg.size()]);

        Cursor callLogCursor = null;
        List<VWCallLog> callLogList = new ArrayList<>();
        try
        {
            if (ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                return null;
            }
            callLogCursor = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, sortBy);
            if (callLogCursor != null && callLogCursor.getCount() > 0)
            {
                final HashMap<String, Integer> indexMap = ContactDbHelper.GetColumnIndexByProjection(projection, callLogCursor);
                callLogCursor.moveToFirst();
                do
                {
                    VWCallLog callLog = new VWCallLog();
                    callLog.mDate = callLogCursor.getLong(indexMap.get(CallLog.Calls.DATE));
                    callLog.mPhoneNumber = callLogCursor.getString(indexMap.get(CallLog.Calls.NUMBER));
                    callLog.mType = callLogCursor.getInt(indexMap.get(CallLog.Calls.TYPE));
                    callLog.mDuration = callLogCursor.getLong(indexMap.get(CallLog.Calls.DURATION));
                    callLog.mDisplayName = callLogCursor.getString(indexMap.get(CallLog.Calls.CACHED_NAME));
                    callLog.mPhoneKind = callLogCursor.getString(indexMap.get(CallLog.Calls.CACHED_NUMBER_LABEL));
                    callLogList.add(callLog);

                } while (callLogCursor.moveToNext());
            }
        }
        catch (Exception e)
        {
            Log.w(this, "error during gets phone numbers", e);
        }
        finally
        {
            if (callLogCursor != null)
            {
                callLogCursor.close();
            }
        }

        return callLogList;

    }

    public interface OnCallLogByContactResultCallback
    {
        void OnLoadCompleteListener(final List<VWCallLog> callLogList);
    }
}