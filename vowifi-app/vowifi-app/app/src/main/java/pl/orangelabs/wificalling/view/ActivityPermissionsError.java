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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import pl.orangelabs.wificalling.LauncherActivity;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.SettingsApp;

import static pl.orangelabs.wificalling.SettingsApp.MANDATORY_PERMISSIONS;

/**
 * @author F
 */
public class ActivityPermissionsError extends ActivityBase
{
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_error);
        initToolbar(true);

        final View retryBtn = findViewById(R.id.page_permissions_error_button);
        retryBtn.setOnClickListener(this::retryPermissions);
    }

    private void retryPermissions(final View view)
    {
        if (checkShouldShowRequestPermission() || SettingsApp.isPermissionsGranted(getApplicationContext()))
        {
            showInitActivity();
        }
        else
        {
            showApplicationSetting();
        }
    }

    private void showInitActivity()
    {
        LauncherActivity.showInitActivity(getApplicationContext());
        finish();
    }

    private void showApplicationSetting()
    {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean checkShouldShowRequestPermission()
    {
        for (String permission : MANDATORY_PERMISSIONS)
        {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
            {
                return false;
            }
        }
        return true;
    }
}
