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

package pl.orangelabs.wificalling.view.activation_client;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;

import com.annimon.stream.Stream;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.WifiUtils;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;
import pl.orangelabs.wificalling.view.ActivityBaseInit;
import pl.orangelabs.wificalling.view.ActivityPermissionsError;

import static pl.orangelabs.wificalling.SettingsApp.MANDATORY_PERMISSIONS;
import static pl.orangelabs.wificalling.SettingsApp.REQUEST_CODE_PERMISSIONS;
import static pl.orangelabs.wificalling.util.Utils.isCorrectOperator;

public class ActivityActivationInit extends ActivityBaseInit
{
    private AlertDialog wifiDialog = null;
    private AlertDialog dialogWrongOperator = null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_activiation_init);
        super.onCreate(savedInstanceState);
        findViewById(R.id.activity_activation_btn_start).setOnClickListener(v -> activateService());
        final long notGrantedPermissions =
                Stream.of(MANDATORY_PERMISSIONS).filter(v -> ContextCompat.checkSelfPermission(this, v) != PackageManager.PERMISSION_GRANTED).count();
        if (notGrantedPermissions > 0L)
        {
            ActivityCompat.requestPermissions(this, MANDATORY_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        else
        {
            transitionUserControlsDelayed();
        }
    }
    public void activateService()
    {
        if (isCorrectOperator(getApplicationContext()))
        {
            findViewById(R.id.activity_activation_btn_start).setEnabled(false);
            WifiUtils.isInternetConnectionAvailable(isOnline -> {
                if (isOnline)
                {
                    App.getActivationComponent().activateAccount(true);
                    Intent intent = new Intent(ActivityActivationInit.this, ActivityActivationTutorial.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else
                {
                    showWifiDialog();
                    findViewById(R.id.activity_activation_btn_start).setEnabled(true);
                }
            }, getApplicationContext());
        }
        else
        {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_title_wrong_operator)
                    .setCancelable(false)
                    .setMessage(R.string.dialog_msg_wrong_operator)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }
    }
    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults)
    {
        if (requestCode == REQUEST_CODE_PERMISSIONS)
        {
            boolean allGranted = true;
            for (final int grantResult : grantResults)
            {
                if (grantResult != PackageManager.PERMISSION_GRANTED)
                {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted)
            {
                transitionUserControlsDelayed();
            }
            else
            {
                startActivity(new Intent(this, ActivityPermissionsError.class));
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    private void showWifiDialog()
    {
        if (wifiDialog == null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(R.layout.dialog_wifi);
            builder.setCancelable(false);
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            builder.setPositiveButton(R.string.turn_on_wifi, (dialog, which) -> {
                wifiManager.setWifiEnabled(true);
            });
            builder.setNegativeButton(R.string.skip, (dialog, which) -> {
                dialog.dismiss();
            });
            builder.setNeutralButton(R.string.settings, (dialog, which) -> {
                Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
                startActivity(intent);
            });
            wifiDialog = builder.create();
        }
        if (!wifiDialog.isShowing())
        {
            wifiDialog.show();
        }
        FontHandler.setFont(wifiDialog);
        Button button = wifiDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        button.setTextColor(ResourcesCompat.getColor(getResources(), R.color.textInvertedAccented, null));
    }

}
