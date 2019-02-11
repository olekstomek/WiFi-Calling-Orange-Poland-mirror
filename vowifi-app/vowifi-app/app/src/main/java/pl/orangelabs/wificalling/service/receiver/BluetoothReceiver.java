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

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.sip.SipServiceCommand;

/**
 * Created by kozlovsky on 11/4/2016.
 */

public class BluetoothReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        switch (action)
        {
            case BluetoothDevice.ACTION_FOUND:
                Log.d(this, "Device found!");
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.d(this, "Device connected!");
                SipServiceCommand.setBluetoothHeadset(context, true);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.d(this, "Device disconnected!");
                SipServiceCommand.setBluetoothHeadset(context, false);
                break;
            default:
                Log.d(this, "Device found!");
                break;
        }

    }
}
