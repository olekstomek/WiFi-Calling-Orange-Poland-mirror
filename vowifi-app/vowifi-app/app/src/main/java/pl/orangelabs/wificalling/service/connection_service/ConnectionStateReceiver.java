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

package pl.orangelabs.wificalling.service.connection_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pl.orangelabs.wificalling.util.BroadcastHelper;


/**
 * Created by marcin on 21.11.16.
 */

public class ConnectionStateReceiver extends BroadcastReceiver
{
    private ConnectionStateMessenger mConnectionStateMessenger;

    public ConnectionStateReceiver(ConnectionStateMessenger connectionStateMessenger)
    {
        mConnectionStateMessenger = connectionStateMessenger;
    }

    public void registerConnectionStateReceiver(final Context context)
    {
        BroadcastHelper.RegisterReceiver(context, ConnectionStateReceiver.this, ConnectionMsg.ALL_ACTIONS);
    }

    public void unRegisterConnectionStateReceiver(final Context context)
    {
        BroadcastHelper.UnRegisterReceiver(context, ConnectionStateReceiver.this);
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null || intent.getAction() == null)
        {
            return;
        }

        if (intent.getAction().equals(ConnectionMsg.CONNECTION_STATE))
        {
            onConnectionIntent(intent);
        }
    }
    private void onConnectionIntent(Intent intent)
    {
        ConnectionState connectionState = (ConnectionState) intent.getSerializableExtra(BroadcastConnectionSender.BroadcastParameters.CONNECTION_STATE);
        mConnectionStateMessenger.stateChange(connectionState);
    }
}
