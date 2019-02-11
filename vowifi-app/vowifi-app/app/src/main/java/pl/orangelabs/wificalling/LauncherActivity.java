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

package pl.orangelabs.wificalling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import pl.orangelabs.wificalling.model.PermissionList;
import pl.orangelabs.wificalling.service.activation_service.ActivationState;
import pl.orangelabs.wificalling.service.activation_service.RepeatModeState;
import pl.orangelabs.wificalling.view.ActivityPermissions;
import pl.orangelabs.wificalling.view.activation_client.ActivityActivationInit;
import pl.orangelabs.wificalling.view.ActivityInit;
import pl.orangelabs.wificalling.view.activation_client.ActivityActivationSmsWaiter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class LauncherActivity extends Activity
{
    private static boolean showActivityPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        showInitActivity(this);
        finish();
    }
    public static void showInitActivity(Context context)
    {
        Intent newActivity = new Intent(context, getActivityToShow(context));
        newActivity.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(newActivity);
    }
    private static Class<? extends Activity> getActivityToShow(Context context)
    {
        Class<? extends Activity> activityStart;
        if (showActivityPermission && PermissionList.isMissingPermission(context))
        {
            showActivityPermission = false;
            activityStart = ActivityPermissions.class;
        }
        else if (App.getActivationComponent().isActive() || !BuildConfig.CONFIG_PROD)
        {
            activityStart = ActivityInit.class;
        }
        else if(App.getActivationComponent().getActivationState() != ActivationState.NON
                || App.getActivationComponent().getRepeatModeState() != RepeatModeState.OFF)
        {
            activityStart = ActivityActivationSmsWaiter.class;
        }
        else
        {
            activityStart = ActivityActivationInit.class;
        }
        return activityStart;
    }
}
