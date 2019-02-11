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

import org.strongswan.android.logic.IpsecpMsg;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.VpnStateService;
import pl.orangelabs.wificalling.model.VanStateMessenger;
import pl.orangelabs.wificalling.util.BroadcastHelper;

/**
 * @author Cookie
 */

public class VpnStateReceiver extends BroadcastReceiver
{
    private VanStateMessenger mMessenger;

    public VpnStateReceiver(final VanStateMessenger messenger)
    {
        mMessenger = messenger;
    }

    public void RegisterVpnStateReceiver(final Context context)
    {
        BroadcastHelper.RegisterReceiver(context, VpnStateReceiver.this, IpsecpMsg.ALL_ACTIONS);
    }

    public void UnRegisterVpnStateReceiver(final Context context)
    {
        BroadcastHelper.UnRegisterReceiver(context, VpnStateReceiver.this);
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        final String action = intent.getAction();

        Log.d(this, "onReceive " + action);

        if (action.equals(IpsecpMsg.STATE_CHANGE))
        {
            stateChange(intent);
        }

    }

    public void stateChange(final Intent intent)
    {
        Bundle extra = intent.getBundleExtra(BroadcastHelper.EXTRA);
        VpnStateService.State state = VpnStateService.State.values()[extra.getInt(VpnStateService.VPN_STATE)];
        VpnStateService.ErrorState errorState = VpnStateService.ErrorState.values()[extra.getInt(VpnStateService.VPN_ERROR_STATE)];

        mMessenger.stateChange(state, errorState);

//        mInitState.setVpnState(state, errorState);
//        Log.d(this, "IPSEC STATE CHANGE " + state + ", error" + errorState);
//
    }


}
