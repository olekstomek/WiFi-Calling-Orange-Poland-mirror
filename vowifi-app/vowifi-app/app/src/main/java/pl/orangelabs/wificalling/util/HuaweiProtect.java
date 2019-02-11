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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import pl.orangelabs.wificalling.R;

/**
 * @author Cookie
 */

public class HuaweiProtect
{
    private Context mContext;

    public HuaweiProtect(final Context context)
    {
        mContext = context;
    }

    public void ifHuaweiAlert()
    {
        final SharedPrefs prefs = new SharedPrefs(mContext);
        boolean skipMessage = prefs.Load(SharedPrefs.KEY_SKIP_PROTECTED_APPS_MESSAGE, SharedPrefs.Defaults.SKIP_PROTECTED_APPS_MESSAGE);
        if (!skipMessage)
        {
            Intent intent = new Intent();
            intent.setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
            if (isCallable(intent))
            {
                final AppCompatCheckBox dontShowAgain = new AppCompatCheckBox(mContext);
                //dontShowAgain.setText("Do not show again");
                dontShowAgain.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> prefs.Save(SharedPrefs.KEY_SKIP_PROTECTED_APPS_MESSAGE, isChecked));

                new AlertDialog.Builder(mContext)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Huawei Protected Apps")
                    .setMessage(
                        String.format("%s requires to be enabled in 'Protected Apps' to function properly.%n", mContext.getString(R.string.app_name)))
                    .setView(dontShowAgain)
                    .setPositiveButton("Protected Apps", (dialog, which) -> huaweiProtectedApps())
                    .setNegativeButton(android.R.string.cancel, null)
                    .create().show();
            }
            else
            {
                prefs.Save(SharedPrefs.KEY_SKIP_PROTECTED_APPS_MESSAGE, true);
            }
        }
    }

    private boolean isCallable(Intent intent)
    {
        List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void huaweiProtectedApps()
    {
        try
        {
            String cmd = "am start -n com.huawei.systemmanager/.optimize.process.ProtectActivity";
            cmd += " --user " + getUserSerial();
            Runtime.getRuntime().exec(cmd);
        }
        catch (IOException ignored)
        {
        }
    }

    private String getUserSerial()
    {
        //noinspection ResourceType
        Object userManager = mContext.getSystemService("user");
        if (null == userManager)
        {
            return "";
        }
        try
        {
            Method myUserHandleMethod = android.os.Process.class.getMethod("myUserHandle", (Class<?>[]) null);
            Object myUserHandle = myUserHandleMethod.invoke(android.os.Process.class, (Object[]) null);
            Method getSerialNumberForUser = userManager.getClass().getMethod("getSerialNumberForUser", myUserHandle.getClass());
            Long userSerial = (Long) getSerialNumberForUser.invoke(userManager, myUserHandle);
            if (userSerial != null)
            {
                return String.valueOf(userSerial);
            }
            else
            {
                return "";
            }
        }
        catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored)
        {
        }
        return "";
    }
}
