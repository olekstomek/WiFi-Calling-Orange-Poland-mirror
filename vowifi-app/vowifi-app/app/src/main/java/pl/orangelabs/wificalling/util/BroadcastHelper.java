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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

/**
 * Created by Cookie on 2015-11-30.
 */
public class BroadcastHelper
{
    private final Context mContext;
    public final static String EXTRA = "olp_broadcasts_extra";

    public BroadcastHelper(final Context context)
    {
        mContext = context;
    }

    private Intent createIntent(String action)
    {
        return new Intent(action);
    }

    public void sendMessage(final String action, final String extra)
    {
        Intent intent = createIntent(action);
        intent.putExtra(EXTRA, extra);
        sendMessage(intent);
    }

    public void sendMessage(final String action)
    {
        Intent intent = createIntent(action);
        sendMessage(intent);
    }

    public void sendMessage(final String action, final int extra)
    {
        Intent intent = createIntent(action);
        intent.putExtra(EXTRA, extra);
        sendMessage(intent);
    }

    public void sendMessage(final String action, final long extra)
    {
        Intent intent = createIntent(action);
        intent.putExtra(EXTRA, extra);
        sendMessage(intent);
    }

    public void sendMessage(final String action, final Bundle extra)
    {
        Intent intent = createIntent(action);
        intent.putExtra(EXTRA, extra);
        sendMessage(intent);
    }

    public void sendMessage(final Intent intent)
    {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }


    public static void RegisterReceiver(final Context context, final BroadcastReceiver messageReceiver, final List<String> actions)
    {
        final IntentFilter intentFilter = new IntentFilter();
        for (final String action : actions)
        {
            intentFilter.addAction(action);
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(messageReceiver, intentFilter);
    }

    public static void UnRegisterReceiver(final Context context, final BroadcastReceiver messageReceiver)
    {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageReceiver);
    }


}
