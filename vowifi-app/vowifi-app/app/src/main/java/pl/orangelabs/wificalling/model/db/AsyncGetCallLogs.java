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
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.util.PermissionUtils;

public class AsyncGetCallLogs extends AsyncTask<Void, Void, List<VWCallLog>>
{
    private Context mCtx;
    private OnCallLogByContactResultCallback mResultCallback;
    private boolean isContactPermissionGranted = false;

    public AsyncGetCallLogs(final Context ctx, final OnCallLogByContactResultCallback resultCallback)
    {
        mCtx = ctx;
        mResultCallback = resultCallback;
        isContactPermissionGranted = PermissionUtils.isPermissionGranted(Manifest.permission.READ_CONTACTS, ctx);
    }

    @Override
    protected final List<VWCallLog> doInBackground(final Void... params)
    {
        return GetCallLog(mCtx);
    }

    @Override
    protected void onPostExecute(final List<VWCallLog> callLogList)
    {
        Log.d(this, "onPostExecute");
        mResultCallback.OnLoadCompleteListener(callLogList);
    }

    private List<VWCallLog> GetCallLog(final Context ctx)
    {
        final String[] projection = new String[] {
                CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.CACHED_NUMBER_TYPE,
            CallLog.Calls.CACHED_NUMBER_LABEL,
            CallLog.Calls.DATE,
            CallLog.Calls._ID,
            CallLog.Calls.NEW,
            CallLog.Calls.CACHED_LOOKUP_URI
        };


        final String sortBy = CallLog.Calls.DATE + " DESC";

        Cursor callLogCursor = null;
        List<VWCallLog> callLogList = new ArrayList<>();
        try
        {
            if (ActivityCompat.checkSelfPermission(mCtx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                return null;
            }

            callLogCursor = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, null, null, sortBy);
            if (callLogCursor != null && callLogCursor.getCount() > 0)
            {
                final HashMap<String, Integer> indexMap = ContactDbHelper.GetColumnIndexByProjection(projection, callLogCursor);
                callLogCursor.moveToFirst();
                do
                {
                    VWCallLog callLog = new VWCallLog();
                    callLog.id = callLogCursor.getInt(indexMap.get(CallLog.Calls._ID));
                    callLog.mPhoneNumber = callLogCursor.getString(indexMap.get(CallLog.Calls.NUMBER));
                    callLog.mType = callLogCursor.getInt(indexMap.get(CallLog.Calls.TYPE));
                    callLog.mDuration = callLogCursor.getLong(indexMap.get(CallLog.Calls.DURATION));
                    callLog.mDate = callLogCursor.getLong(indexMap.get(CallLog.Calls.DATE));
                    callLog.mDisplayName = callLogCursor.getString(indexMap.get(CallLog.Calls.CACHED_NAME));
                    callLog.mPhoneKind = callLogCursor.getString(indexMap.get(CallLog.Calls.CACHED_NUMBER_LABEL));
                    callLog.mNew = callLogCursor.getInt(indexMap.get(CallLog.Calls.NEW)) == 1;
                    callLog.mContactUri = callLogCursor.getString(indexMap.get(CallLog.Calls.CACHED_LOOKUP_URI));
                    if (callLog.mPhoneKind == null)
                    {
                        final int numberType = callLogCursor.getInt(indexMap.get(CallLog.Calls.CACHED_NUMBER_TYPE));
                        callLog.mPhoneKind = ctx.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(numberType));
                    }

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
        fillCallLogsContactsPhoto(callLogList, ctx);
        return callLogList;
    }
    private void fillCallLogsContactsPhoto(List<VWCallLog> callLogList, Context context)
    {
        if (isContactPermissionGranted)
        {
            for (final VWCallLog callLogEntry : callLogList)
            {
                if (callLogEntry.mContactUri != null)
                {
                    callLogEntry.mThumbnailUri = Uri.withAppendedPath(Uri.parse(callLogEntry.mContactUri), ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                }
            }
        }
    }
    public interface OnCallLogByContactResultCallback
    {
        void OnLoadCompleteListener(final List<VWCallLog> callLogList);
    }
}