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

import android.content.Context;
import android.content.Intent;

/**
 * Created by marcin on 21.11.16.
 */

public class ConnectionCommand
{
    public static final String ACTION_CONNECT_TO_SERVICE = "action_make_connection";
    public static final String ACTION_DISCONNECT_SERVICE = "action_make_disconnect";
    public static final String ACTION_CONNECT_TO_SIP = "action_make_connection";
    public static final String UPDATE_NOTIFICATION ="action_update_notification";
    public static final String GET_STATE = "action_get_state";
    public static final String UPDATE_STATE = "action_update_state";
    public static final String REGISTER_GET_STATE_AFTER_TIMEOUT = "REGISTER_GET_STATE_AFTER_TIMEOUT";
    public static final String RSSI_CHANGED = "RSSI_CHANGED";

    public static final String PARAM_CONNECTION_STATE = "connection_state";
    public static final String PARAM_CONNECTION_ERROR = "connection_error";
    public static final String PARAM_RESET = "connection_error";

    public static void makeConnectIntent(final Context context, boolean reset)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_CONNECT_TO_SERVICE);
        intent.putExtra(PARAM_RESET, reset);
        context.startService(intent);
    }
    public static void makeDisconnectIntent(final Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_DISCONNECT_SERVICE);
        context.startService(intent);
    }
    public static void makeConnectToSIPIntent(final Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(ACTION_CONNECT_TO_SIP);
        context.startService(intent);
    }
    public static void makeUpdateNotificationIntent(final Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(UPDATE_NOTIFICATION);
        context.startService(intent);
    }
    public static void makeGetStateIntent(final Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(GET_STATE);
        context.startService(intent);
    }
    public static void makeGetStateAfterTimeout(final Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(REGISTER_GET_STATE_AFTER_TIMEOUT);
        context.startService(intent);
    }
    public static void makeUpdateStateIntent(Context context, ConnectionStatus connectionStatus, ConnectionError connectionError)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(UPDATE_STATE);
        intent.putExtra(PARAM_CONNECTION_ERROR, connectionError.ordinal());
        intent.putExtra(PARAM_CONNECTION_STATE, connectionStatus.ordinal());
        context.startService(intent);
    }
    public static void makeRSSIChanged(Context context)
    {
        Intent intent = new Intent(context, ConnectionService.class);
        intent.setAction(RSSI_CHANGED);
        context.startService(intent);
    }
}
