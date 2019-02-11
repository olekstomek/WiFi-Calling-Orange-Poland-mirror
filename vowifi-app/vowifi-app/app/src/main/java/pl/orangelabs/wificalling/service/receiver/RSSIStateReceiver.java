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

package pl.orangelabs.wificalling.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.service.connection_service.ConnectionCommand;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStatus;
import pl.orangelabs.wificalling.service.connection_service.WifiSignalLevel;
import pl.orangelabs.wificalling.util.WifiUtils;

/**
 * Created by marcin on 08.02.17.
 */

public class RSSIStateReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (ConnectionService.isServiceON(context))
        {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (intent != null && ConnectionService.isServiceON(context) &&
                    netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI && netInfo.isConnected())
            {
                Log.d(this,"RSSI level :" + WifiUtils.getCurrentNetworkRSSI(context));
                if (ConnectionService.getConnectionState().getConnectionStatus() != ConnectionStatus.CONNECTED
                        || WifiUtils.getCurrentNetworkRSSI(context) < WifiSignalLevel.DISCONNECTION_LEVEL.getSignalLevel())
                {
                    ConnectionCommand.makeRSSIChanged(context);
                }
            }
        }
    }
}
