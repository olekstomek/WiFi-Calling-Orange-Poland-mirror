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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Stream;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.MainActivityTest;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.SettingsApp;
import pl.orangelabs.wificalling.VpnStateService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionCommand;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionState;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStateMessenger;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStateReceiver;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;
import pl.orangelabs.wificalling.view.helper.TermsAgreementDialog;

import static pl.orangelabs.wificalling.SettingsApp.MANDATORY_PERMISSIONS;
import static pl.orangelabs.wificalling.SettingsApp.REQUEST_CODE_PERMISSIONS;

/**
 * @author F
 */
public class ActivityInit extends ActivityBaseInit
{
    int errorsCounter = 0;
    private ProgressDialog progressDialogConnecting;
    private AlertDialog wifiDialog;
    private AlertDialog wifiDialogNoInternet;
    private AlertDialog cannotConnectDialog;
    private AlertDialog cannotConnectResetDialog;
    private AlertDialog simHasChangedDialog;
    private ConnectionStateReceiver connectionStateReceiver;


    public static void startInitActivity(Context context)
    {
        Intent intent = new Intent(context, ActivityInit.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_init);
        super.onCreate(savedInstanceState);

        final View continueBtn = findViewById(R.id.activity_init_btn_start);

        continueBtn.setOnClickListener(v -> TurnOnService());

        //Utils.BatterySavingFeature(this);// TODO: 2017-03-22

        if (SettingsApp.debugAdditionalInfo)
        {
            ((LinearLayout) findViewById(R.id.sip_state).getParent()).setVisibility(View.VISIBLE);
            findViewById(R.id.activity_init_btn_tm_pre_start_anim).setVisibility(View.VISIBLE);
            findViewById(R.id.activity_init_btn_tmp_test_activity).setVisibility(View.VISIBLE);
            findViewById(R.id.activity_init_btn_tm_pre_start_anim) // tmp, goes to app without starting VPN+SIP
                .setOnClickListener(v -> startMainActivity());
            findViewById(R.id.activity_init_btn_tmp_test_activity) // tmp, navigatess to test activity
                .setOnClickListener(v -> startActivity(new Intent(this, MainActivityTest.class)));
        }
        connectionStateReceiver = new ConnectionStateReceiver(new InternalConnectionStateMessenger());

        final long notGrantedPermissions =
            Stream.of(MANDATORY_PERMISSIONS).filter(v -> ContextCompat.checkSelfPermission(this, v) != PackageManager.PERMISSION_GRANTED).count();
        if (notGrantedPermissions > 0L)
        {
            ActivityCompat.requestPermissions(this, MANDATORY_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        if (!ConnectionService.isServiceON(getApplicationContext()))
        {
            transitionUserControlsDelayed();
        }
    }

    private void TurnOnService()
    {
        boolean isAgreementAccepted = mPrefs.Load(TermsAgreementDialog.AGREEMENT_ACCEPTED, false);
        if (isAgreementAccepted)
        {
            turnOnServiceWifiCheck();
        }
        else
        {
            new TermsAgreementDialog().showTermsAndAgreementsDialog(this, this::turnOnServiceWifiCheck, false);
        }
    }

    private void turnOnServiceWifiCheck()
    {
        ConnectivityManager conMan = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = conMan.getActiveNetworkInfo();
        ConnectionService.serServiceON(true, getApplicationContext());
        if (wifiInfo != null && wifiInfo.getType() == ConnectivityManager.TYPE_WIFI && wifiInfo.isConnected())
        {
            showConnectingProgressDialog(R.string.connecting, false);
            tryStartingCharon();
        }
        else
        {
            showWifiDialog();
        }
    }

    private void showWifiDialog()
    {
        if (wifiDialog == null)
        {
            wifiDialog = getWifiDialog(false);
        }
        hideConnectingProgressDialog();
        if (!wifiDialog.isShowing())
        {
            wifiDialog.show();
        }
    }
    private void showWifiDialogNoInternet()
    {
        if (wifiDialogNoInternet == null)
        {
            wifiDialogNoInternet = getWifiDialog(true);
        }
        hideConnectingProgressDialog();
        if (!wifiDialogNoInternet.isShowing())
        {
            wifiDialogNoInternet.show();
        }
    }
    private AlertDialog getWifiDialog(boolean noInternetConnection)
    {
        AlertDialog alertDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(noInternetConnection ? R.layout.dialog_wifi_no_internet: R.layout.dialog_wifi);
        builder.setCancelable(false);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!noInternetConnection)
        {
            builder.setPositiveButton(R.string.turn_on_wifi, (dialog, which) ->
            {
                ConnectionCommand.makeGetStateAfterTimeout(getApplicationContext());
                wifiManager.setWifiEnabled(true);
                showConnectingProgressDialog(R.string.connecting, false);
            });
        }
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton(R.string.settings, (dialog, which) ->
        {
            Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
            startActivity(intent);
        });
        alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog ->
        {
            Button button = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            button.setTextColor(ResourcesCompat.getColor(getResources(), R.color.textInvertedAccented, null));
            FontHandler.setFont(alertDialog);
        });
        return alertDialog;
    }
    private void hideWifiDialog()
    {
        if (wifiDialog != null)
        {
            wifiDialog.dismiss();
        }
        if (wifiDialogNoInternet != null)
        {
            wifiDialogNoInternet.dismiss();
        }
    }

    private void showConnectingProgressDialog(int messageStringId, boolean showCancelButton)
    {
        if (progressDialogConnecting == null)
        {
            progressDialogConnecting = Utils.getProgressDialog(this, messageStringId);

        }
        if (showCancelButton)
        {
            progressDialogConnecting.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), (dialog, which) ->
            {
            });
        }
        Button button = progressDialogConnecting.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (button != null)
        {
            button.setVisibility(showCancelButton ? View.VISIBLE : View.GONE);
        }
        progressDialogConnecting.setMessage(getString(messageStringId));
        // TODO needs to use custom title/message font for nju flavour
        hideWifiDialog();
        hideCannotConnectDialog();
        progressDialogConnecting.show();
    }

    private void hideConnectingProgressDialog()
    {
        if (progressDialogConnecting != null)
        {
            progressDialogConnecting.dismiss();
        }
    }

    private void showCannotConnectDialog()
    {
        if (cannotConnectResetDialog == null)
        {
            cannotConnectResetDialog = Utils.createCannotConnectResetDialog(this);
        }
        cannotConnectResetDialog.show();
    }

    private void showCannotConnectDialog(int textMessage)
    {
        if (cannotConnectDialog == null)
        {
            cannotConnectDialog = Utils.showDialogWithText(textMessage, ActivityInit.this);
        }
        cannotConnectDialog.setMessage(getString(textMessage));
        cannotConnectDialog.show();
    }

    private void hideCannotConnectDialog()
    {
        if (cannotConnectDialog != null)
        {
            cannotConnectDialog.dismiss();
        }
    }
    private void showChangeSimDialog()
    {
        if (simHasChangedDialog == null)
        {
            simHasChangedDialog = Utils.createSimHasChangedDialog(this);
        }
        simHasChangedDialog.show();
    }

    private void hideChangeSimDialog()
    {
        if (simHasChangedDialog != null)
        {
            simHasChangedDialog.dismiss();
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
            if (!allGranted)
            {
                startActivity(new Intent(this, ActivityPermissionsError.class));
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void tryStartingCharon()
    {
        ConnectionCommand.makeConnectIntent(getApplicationContext(), true);
    }

    @Override
    protected void onResume()
    {
        Log.d(this, "onResume");
        connectionStateReceiver.registerConnectionStateReceiver(getApplicationContext());
        ConnectionCommand.makeGetStateIntent(getApplicationContext());
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        //UnRegisterReceiver();
        //mInitState = null;
        connectionStateReceiver.unRegisterConnectionStateReceiver(getApplicationContext());
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        hideConnectingProgressDialog();
        hideCannotConnectDialog();
        hideWifiDialog();
        hideChangeSimDialog();
        super.onDestroy();
    }


    /**
     * DEBUG
     *
     * @param text
     */
    private void dbgUpdateVpnState(String text)
    {
        ((TextView) findViewById(R.id.vpn_state)).setText(text);
    }

    /**
     * DEBUG
     *
     * @param text
     */
    private void dbgUpdateSipState(String text)
    {
        ((TextView) findViewById(R.id.sip_state)).setText(text);
    }

    private void startMainActivity()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    mWaitingThread.join();
                }
                catch (InterruptedException ignored)
                {
                }
                startActivity(new Intent(ActivityInit.this, ActivityMain.class));
                finish();
            }
        }.start();
    }

    private void handleFinalState()
    {
        hideConnectingProgressDialog();
        startMainActivity();
    }

    private void handleError()
    {
        hideConnectingProgressDialog();
        errorsCounter++;
        if (errorsCounter != 3)
        {
            showErrorSnackBar();
        }
        else
        {
            errorsCounter = 0;
            showCannotConnectDialog();
        }
    }

    private void showErrorSnackBar()
    {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_init_bottom_frame), R.string.connection_error, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.misc_btn_try_again, v ->
        {
            TurnOnService();
        });
        snackbar.show();
    }

    /**
     * thread that waits a specified amount of time to ensure the splash screen is visible for at least MINIMAL_SPLASH_TIME (this thread should be
     * joined before changing activity etc)
     */
    private class InternalConnectionStateMessenger extends ConnectionStateMessenger
    {
        @Override
        public void stateChange(ConnectionState connectionState)
        {
            super.stateChange(connectionState);
            switch (connectionState.getConnectionStatus())
            {
                case CONNECTED:
                    handleFinalState();
                    break;
                case CONNECTING:
                    showConnectingProgressDialog(R.string.connecting, false);
                    break;
                case DISCONNECTED:
                case INACTIVE:
                    transitionUserControlsDelayed();
                    hideConnectingProgressDialog();
                    break;
                case DISCONNECTING:
                    showConnectingProgressDialog(R.string.disconnecting,false);
                    break;
                case CONNECTION_ERROR:
                    switch (connectionState.getConnectionError())
                    {
                        case NO_WIFI:
                            showWifiDialog();
                            break;
                        case NO_INTERNET_CONNECTION:
                        case WEAK_WIFI:
                            showWifiDialogNoInternet();
                            break;
                        case NO_NETWORK:
                            showConnectingProgressDialog(R.string.wifi_waiting, true);
                            break;
                        case VPN_PERMISSION_CANCELED:
                            hideConnectingProgressDialog();
                            break;
                        case SIM_CHANGED:
                            handleSimChangedError();
                            break;
                        case VPN_ERROR:
                            if (connectionState.getVpnErrorState() == VpnStateService.ErrorState.GENERIC_ERROR)
                            {
                                hideConnectingProgressDialog();
                                showCannotConnectDialog(R.string.dialog_msg_restart_phone);
                            }
                            else
                            {
                                handleError();
                            }
                        default:
                            handleError();
                    }
                    transitionUserControlsDelayed();
                    break;
            }

            if (SettingsApp.debugAdditionalInfo)
            {
                if (connectionState.getSipRegStatus() != null)
                {
                    dbgUpdateSipState("SIP: " + connectionState.getSipRegStatus() + " [" + connectionState.getSipRegStatus().swigValue() + "]");
                }
                dbgUpdateVpnState("VPN: " + connectionState.getVpnState() + " [" + connectionState.getVpnErrorState() + "]");
            }
        }
    }

    private void handleSimChangedError()
    {
        hideConnectingProgressDialog();
        if (!Utils.isCorrectOperator(getApplicationContext()))
        {
            showCannotConnectDialog(R.string.dialog_msg_wrong_operator);
        }
        else
        {
            showChangeSimDialog();
        }
    }
}
