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

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;

import java.util.Locale;
import android.os.Handler;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.model.ContactCall;

/**
 * Created by marcin on 01.06.17.
 */

public class CallLogUtils
{
    public static void registerCallToCallLog(String number, final long durationInSec, int callType, Context context)
    {

        new Handler(Looper.getMainLooper()).post(() -> registerCallToCallLog(number,durationInSec, ContactCall.loadDetails(context, number),callType,context));
    }
    public static void registerCallToCallLog(String phoneNumber, final long durationInSec, ContactCall contactCall, int callType, Context context)
    {
        if (callType == -1)
        {
            return;
        }

        ContentValues values = new ContentValues();

        values.put(CallLog.Calls.NEW, 1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
        {
            values.put(CallLog.Calls.FEATURES, CallLog.Calls.FEATURES_PULLED_EXTERNALLY);
        }
        else
        {
            values.put(CallLog.Calls.FEATURES, 0x2);
        }

        values.put(CallLog.Calls.NUMBER, phoneNumber);
        values.put(CallLog.Calls.DATE, System.currentTimeMillis());
        values.put(CallLog.Calls.DURATION, durationInSec);
        values.put(CallLog.Calls.TYPE, callType);

//        if (mCallType == CallLog.Calls.MISSED_TYPE)
//        {
//            values.put(CallLog.Calls.IS_READ, 0);
//        }

        values.put(CallLog.Calls.CACHED_NAME, "");
        values.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
        values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");

        if (!TextUtils.isEmpty(phoneNumber))
        {
            if (phoneNumber.startsWith("-"))
            {
                values.put(CallLog.Calls.CACHED_NAME, context.getString(R.string.call_log_entry_anonymouse_name));
            }
            if (contactCall != null)
            {
                values.put(CallLog.Calls.CACHED_NAME, contactCall.mDisplayName);
                if (contactCall.mPhoneKind != null && !contactCall.mPhoneKind.equals("null"))
                {
                    values.put(CallLog.Calls.CACHED_NUMBER_TYPE, contactCall.mPhoneKind);
                    values.put(CallLog.Calls.CACHED_NUMBER_LABEL,
                            context.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(Integer.parseInt(contactCall.mPhoneKind))));
                }
                else
                {
                    putDefaultPhoneType(values, phoneNumber,context);
                }
                Uri myPhoneUri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_LOOKUP_URI, contactCall.LookupKey);
                values.put(CallLog.Calls.CACHED_LOOKUP_URI, myPhoneUri.toString());
            }
            else
            {
                putDefaultPhoneType(values, phoneNumber,context);
            }

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            Log.e(CallLogUtils.class, "values for callLog " + values.toString());
            context.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);
        }
    }

    private static void putDefaultPhoneType(ContentValues values, String phoneNumber, Context context)
    {
        int phoneType = OLPPhoneNumberUtils.GetPhoneType(phoneNumber, Locale.getDefault().getCountry());
        values.put(CallLog.Calls.CACHED_NUMBER_TYPE, phoneType);
        values.put(CallLog.Calls.CACHED_NUMBER_LABEL,
                context.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(phoneType)));
    }
}
