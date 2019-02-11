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

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;


/**
 * Created by marcin on 22.05.17.
 */

public class MobileServiceStateHelper
{
    public MobileServiceStateHelper(Context context)
    {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener ,PhoneStateListener.LISTEN_SERVICE_STATE);
    }
    private ServiceState mServiceState = null;

    public boolean isMobileOutOfService()
    {
        if (mServiceState == null)
        {
            return false;
        }
        return mServiceState.getState() == ServiceState.STATE_OUT_OF_SERVICE;
    }
    public boolean isMobileServiceOn()
    {
        if (mServiceState == null)
        {
            return false;
        }
        return mServiceState.getState() == ServiceState.STATE_IN_SERVICE || mServiceState.getState() == ServiceState.STATE_EMERGENCY_ONLY;
    }
    PhoneStateListener phoneStateListener = new PhoneStateListener()
    {
        @Override
        public void onServiceStateChanged(ServiceState serviceState)
        {
            super.onServiceStateChanged(serviceState);
            mServiceState = serviceState;
        }
    };
    public boolean is112onMobileOn(String number)
    {
        return isMobileServiceOn() && number.equals("112");
    }
}
