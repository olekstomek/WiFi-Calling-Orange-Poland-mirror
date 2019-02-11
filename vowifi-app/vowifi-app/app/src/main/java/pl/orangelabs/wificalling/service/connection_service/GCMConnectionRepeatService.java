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

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import pl.orangelabs.log.Log;

/**
 * Created by marcin on 19.05.17.
 */

public class GCMConnectionRepeatService extends GcmTaskService
{
    public static String ALIAS = "PERIODIC_CONNECTION_REQUEST";
    @Override
    public int onRunTask(TaskParams taskParams)
    {
        Log.d(this,"Connection Periodic task");
        if (ConnectionService.isServiceON(getApplicationContext()))
        {
            ConnectionCommand.makeConnectIntent(getApplicationContext(), false);
        }
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
