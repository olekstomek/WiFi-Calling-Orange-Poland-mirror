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
import android.os.Bundle;

import pl.orangelabs.wificalling.service.connection_service.ConnectionService;

/**
 * Created by marcin on 16.11.16.
 */

public class SIMCardReceiver extends BroadcastReceiver
{
    private static String EXTRAS_SIM_STATE = "ss";
    private static String SIM_STATE_ABSENT = "ABSENT";
    @Override
    public void onReceive(Context context, Intent intent)
    {
        String simState = getSimState(intent);
        if (ConnectionService.isServiceON(context) && simState != null && simState.equals(SIM_STATE_ABSENT))
        {
            ConnectionService.turnServiceOFF(context);
        }
    }
    private String getSimState(Intent intent){
        Bundle bundle = intent.getExtras();
        if (bundle != null)
        {
            return bundle.getString(EXTRAS_SIM_STATE);
        }
        return null;
    }
}
