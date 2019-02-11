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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;

import org.strongswan.android.logic.CharonVpnService;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.VpnProfile;

/**
 * @author F
 */

public class VpnInit
{
    public static final int PREPARE_VPN_SERVICE = 101;

    public void onVpnProfileSelected(final IVpnInitCallback callback, VpnProfile profile)
    {
        Bundle profileInfo = new Bundle();
        prepareVpnService(callback, profileInfo);
    }

    protected void prepareVpnService(final IVpnInitCallback callback, Bundle profileInfo)
    {
        Intent intent;
        try
        {
            intent = VpnService.prepare(callback.ctx());
        }
        catch (IllegalStateException ex)
        {
            /* this happens if the always-on VPN feature (Android 4.2+) is activated */
//            VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported_during_lockdown);
            return;
        }
        /* store profile info until the user grants us permission */
//        mProfileInfo = profileInfo;
        if (intent != null)
        {
            try
            {
                callback.startActivity(intent, PREPARE_VPN_SERVICE);
            }
            catch (ActivityNotFoundException ex)
            {
                Log.i(this, "xx# broken");
                /* it seems some devices, even though they come with Android 4,
                 * don't have the VPN components built into the system image.
				 * com.android.vpndialogs/com.android.vpndialogs.ConfirmDialog
				 * will not be found then */
//                VpnNotSupportedError.showWithMessage(this, R.string.vpn_not_supported);
            }
        }
        else
        {	/* user already granted permission to use VpnService */
            startCharon(callback);
        }
    }

    private void startCharon(final IVpnInitCallback callback)
    {
        Intent intent = new Intent(callback.ctx(), CharonVpnService.class);
        intent.putExtras(new Bundle());
        callback.startService(intent);
    }

    public void stopCharon(final Context context)
    {
        CharonVpnService.stopVPN(context);
    }


    public void onResult(final IVpnInitCallback callback, final int requestCode, final int resultCode, final Intent data)
    {
        Log.i(this, "onResult");
        if (requestCode == PREPARE_VPN_SERVICE && resultCode == Activity.RESULT_OK)
        {
            Log.i(this, "STARTING SERVICE");
            Intent intent = new Intent(callback.ctx(), CharonVpnService.class);
            intent.putExtras(new Bundle());
            callback.startService(intent);
        }
    }

    public interface IVpnInitCallback
    {
        void startActivity(final Intent intent, final int requestCode);

        void startService(final Intent intent);

        Context ctx();
    }
}
