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

package pl.orangelabs.wificalling.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.VpnProfile;
import pl.orangelabs.wificalling.service.connection_service.ConnectionCommand;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.VpnInit;

/**
 * @author F
 */
public class ActivityClear extends ActivityBase
{

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(this, "onCreate");
        setContentView(R.layout.activity_clear);
        mPrefs.Save(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
        tryStartingCharon();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VpnInit.PREPARE_VPN_SERVICE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                ConnectionCommand.makeConnectIntent(getApplicationContext(), true);
            }
            else
            {
                ConnectionCommand.makeGetStateIntent(getApplicationContext());
            }
            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void tryStartingCharon()
    {
        int counter = mPrefs.Load(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
        mPrefs.Save(SharedPrefs.KEY_VPN_TRY_COUNTER, ++counter);

        VpnInit mVpnInit = new VpnInit();
        mVpnInit.onVpnProfileSelected(new VpnInitCallback(), new VpnProfile());
    }


    private class VpnInitCallback implements VpnInit.IVpnInitCallback
    {
        @Override
        public void startActivity(final Intent intent, final int requestCode)
        {
            ActivityClear.this.startActivityForResult(intent, requestCode);
        }

        @Override
        public void startService(final Intent intent)
        {
            ActivityClear.this.startService(intent);
        }

        @Override
        public Context ctx()
        {
            return ActivityClear.this;
        }
    }
}
