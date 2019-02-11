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

package pl.orangelabs.wificalling.model;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.model.db.AsyncGetCallLogs;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.OLPNotificationBuilder;

/**
 * Created by marcin on 07.11.16.
 */

public class CallLogsContentObserver extends ContentObserver
{
    private  AsyncGetCallLogs.OnCallLogByContactResultCallback onCallLogByContactResultCallback;
    private static CallLogsContentObserver callLogsContentObserver;
    private boolean isObserve = false;

    public static CallLogsContentObserver getInstance(){
        if (callLogsContentObserver == null){
            callLogsContentObserver = new CallLogsContentObserver(new Handler());
        }
        return callLogsContentObserver;
    }

    private CallLogsContentObserver(Handler handler)
    {
        super(handler);
    }

    public void observe(){
        if (!isObserve)
        {
            App.get().getContentResolver()
                    .registerContentObserver(CallLog.CONTENT_URI, true, this);
            isObserve = true;
        }
    }

    @Override
    public boolean deliverSelfNotifications()
    {
        return true;
    }

    @Override
    public void onChange(boolean selfChange)
    {
        handleOnchange();
    }
    public void handleOnchange()
    {
        AsyncHelper.execute(new AsyncGetCallLogs(App.get(), callLogList -> {
            if (callLogList != null)
            {
                OLPNotificationBuilder.updateMissedCallNotifications(callLogList, App.get().getApplicationContext());
            }
            if (getInstance().onCallLogByContactResultCallback != null)
            {
                getInstance().onCallLogByContactResultCallback.OnLoadCompleteListener(callLogList);
            }
        }));
    }

    @Override
    public void onChange(boolean selfChange, Uri uri)
    {
        handleOnchange();
    }

    public void registerCallback(AsyncGetCallLogs.OnCallLogByContactResultCallback onCallLogByContactResultCallback){
        this.onCallLogByContactResultCallback = onCallLogByContactResultCallback;
    }
    public void unregisterCallback()
    {
        onCallLogByContactResultCallback = null;
    }

}
