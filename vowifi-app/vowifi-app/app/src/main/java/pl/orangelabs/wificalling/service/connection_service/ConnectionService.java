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

package pl.orangelabs.wificalling.service.connection_service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import org.pjsip.pjsua2.pjsip_status_code;
import org.strongswan.android.logic.CharonVpnService;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.BuildConfig;
import pl.orangelabs.wificalling.LauncherActivity;
import pl.orangelabs.wificalling.VpnProfile;
import pl.orangelabs.wificalling.VpnStateService;
import pl.orangelabs.wificalling.model.VanStateMessenger;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.service.BackgroundService;
import pl.orangelabs.wificalling.service.activation_service.ActivationServerListener;
import pl.orangelabs.wificalling.service.activation_service.KeystoreHandlingDefaultAccounts;
import pl.orangelabs.wificalling.service.activation_service.RepeatStack;
import pl.orangelabs.wificalling.service.receiver.VpnStateReceiver;
import pl.orangelabs.wificalling.sip.BroadcastSipReceiver;
import pl.orangelabs.wificalling.sip.SipLib;
import pl.orangelabs.wificalling.sip.SipMessagesReceiver;
import pl.orangelabs.wificalling.sip.SipService;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.OLPNotificationBuilder;
import pl.orangelabs.wificalling.util.Settings;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.util.VpnInit;
import pl.orangelabs.wificalling.util.WifiUtils;
import pl.orangelabs.wificalling.view.ActivityClear;
import pl.orangelabs.wificalling.view.ActivityInit;

import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_FORBIDDEN;
import static pl.orangelabs.wificalling.SettingsApp.showRssiToast;
import static pl.orangelabs.wificalling.service.connection_service.ConnectionCommand.PARAM_RESET;
import static pl.orangelabs.wificalling.service.connection_service.GCMConnectionRepeatService.ALIAS;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.CancelNotification;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.NOTIFICATION_MAIN;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.showAllNotifications;


/**
 * Created by marcin on 21.11.16.
 */

public class ConnectionService extends BackgroundService implements ActivationServerListener
{
    private BroadcastConnectionSender broadcastConnectionSender;

    private BroadcastSipReceiver mBroadcastSipReceiver;
    private VpnStateReceiver mVpnStateReceiver;
    private boolean isForeground = false;
    private static int repeatSipConnectionCounter = 0;

    private static ConnectionState connectionState = new ConnectionState();
    private final static int MAX_REPEAT_COUNT = 3;
    SharedPrefs sharedPrefs = null;
    RepeatStack repeatStack;


    @Override
    public void onCreate()
    {
        super.onCreate();
        mBroadcastSipReceiver = new BroadcastSipReceiver(new SIPStateReceiver());
        mVpnStateReceiver = new VpnStateReceiver(new VanStateReceiver());
        RegisterReceiver();
        broadcastConnectionSender = new BroadcastConnectionSender(getApplicationContext());
        sharedPrefs = new SharedPrefs(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        enqueueJob(() -> {
            if (intent != null && intent.getAction() != null)
            {
                String action = intent.getAction();

                if(ConnectionCommand.RSSI_CHANGED.equals(action))
                {
                    onRSSIChanged();
                }
                if (ConnectionCommand.UPDATE_NOTIFICATION.equals(action))
                {
                    broadcastConnectionState();
                }
                else if (ConnectionCommand.GET_STATE.equals(action))
                {
                    broadcastConnectionState();
                }
                else if(ConnectionCommand.ACTION_CONNECT_TO_SERVICE.equals(action))
                {
                    connectToService(intent.getBooleanExtra(PARAM_RESET, false));
                }
                else if(ConnectionCommand.ACTION_CONNECT_TO_SIP.equals(action))
                {
                    connectToSIP();
                }
                else if(ConnectionCommand.UPDATE_STATE.equals(action))
                {
                    setState(intent);
                }
                else if(ConnectionCommand.REGISTER_GET_STATE_AFTER_TIMEOUT.equals(action))
                {
                    registerTimeout();
                }
                else if (ConnectionCommand.ACTION_DISCONNECT_SERVICE.equals(action))
                {
                    disconnectService();
                }

            }
        });
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        UnRegisterReceiver();
        App.getActivationComponent().unRegisterListener(this);
    }
    private void setState(Intent intent)
    {
        connectionState.setConnectionStatus(ConnectionStatus.values()[
                intent.getIntExtra(ConnectionCommand.PARAM_CONNECTION_STATE,0)]);
        connectionState.setConnectionError(ConnectionError.values()[
                intent.getIntExtra(ConnectionCommand.PARAM_CONNECTION_ERROR,0)]);
    }
    private void broadcastConnectionState()
    {
        connectionState.updateState(getApplicationContext());
        Log.d(this, "connection status: " + connectionState.toString());
        broadcastConnectionSender.broadcastConnectionState(connectionState);
        if (!SipLib.getInstance().isActiveCall())
        {
            OLPNotificationBuilder mNotificationBuilder = new OLPNotificationBuilder(getApplicationContext());
            StartStopService(mNotificationBuilder);
            mNotificationBuilder.UpdateMainNotification(connectionState);
        }
    }

    public static ConnectionState getConnectionState()
    {
        return connectionState;
    }
    private void StartStopService(OLPNotificationBuilder mNotificationBuilder)
    {
        if (connectionState.getConnectionStatus() == ConnectionStatus.CONNECTED)
        {
            sharedPrefs.Save(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
            if (!isForeground)
            {
                Utils.showRoutingTable();
                startForeground(NOTIFICATION_MAIN, mNotificationBuilder.GetMainNotification(connectionState));
                isForeground = true;
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            }
        }
        else
        {
            if (!showAllNotifications || connectionState.getConnectionStatus() == ConnectionStatus.INACTIVE)
            {
                stopForeground(true);
                CancelNotification(getApplicationContext(),OLPNotificationBuilder.NOTIFICATION_MAIN);
                isForeground = false;
            }
        }
    }

    protected void RegisterReceiver()
    {
        if (mBroadcastSipReceiver != null)
        {
            mBroadcastSipReceiver.RegisterSipReceiver(this);
        }
        if (mVpnStateReceiver != null)
        {
            mVpnStateReceiver.RegisterVpnStateReceiver(this);
        }
    }

    protected void UnRegisterReceiver()
    {
        if (mBroadcastSipReceiver != null)
        {
            mBroadcastSipReceiver.UnRegisterSipReceiver(this);
        }
        if (mVpnStateReceiver != null)
        {
            mVpnStateReceiver.UnRegisterVpnStateReceiver(this);
        }
    }

    private void connectToService(boolean reset)
    {
        if (!BuildConfig.CONFIG_PROD)
        {
            loadCertAndPrivateKey();
        }
        repeatSipConnectionCounter = 0;
        if (connectionState.getConnectionStatus() != ConnectionStatus.CONNECTED)
        {
            if(connectionState.getVpnState() != null && connectionState.getVpnState() == VpnStateService.State.CONNECTED)
            {
                connectToSIP();
            }
            else
            {
                if (connectionState.getConnectionStatus() != ConnectionStatus.CONNECTING || reset)
                {
                    resetConnectionService();
                }
                if (connectionState.getConnectionStatus() == ConnectionStatus.DISCONNECTED
                        || connectionState.getConnectionError() == ConnectionError.VPN_PERMISSION_CANCELED)
                {
                    connectionState.setConnectionStatus(ConnectionStatus.CONNECTING);
                    connectionState.setConnectionError(ConnectionError.NON);
                    cancelRepeat();
                    Utils.showRoutingTable();
                    tryStartingCharon(getApplicationContext());
                }
                else
                {
                    if (connectionState.getConnectionError() == ConnectionError.VPN_CERT_OUT_OF_DATE)
                    {
                        handleExpiredCertificate();
                    }
                    else if (connectionState.getConnectionError() == ConnectionError.NO_INTERNET_CONNECTION)
                    {
                        repeatConnect();
                    }
                    else
                    {
                        cancelRepeat();
                    }
                    broadcastConnectionState();
                }
            }
        }
        else
        {
            broadcastConnectionState();
        }
    }


    private void repeatConnect()
    {
        if (repeatStack == null)
        {
            Log.d(this, "Repeat Periodic task()");
            repeatStack = new RepeatStack(true);
        }
        if (repeatStack.hasNext())
        {
            GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(getApplicationContext());
            PeriodicTask task = new PeriodicTask.Builder()
                    .setService(GCMConnectionRepeatService.class)
                    .setTag(ALIAS)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setPeriod(repeatStack.getTime())
                    .setFlex(repeatStack.getTime()/4L)
                    .setRequiresCharging(false)
                    .setUpdateCurrent(true)
                    .build();
            mGcmNetworkManager.schedule(task);
        }
        else
        {
            cancelRepeat();
        }
    }
    private void cancelRepeat()
    {
        Log.d(this, "Cancel Periodic task");
        repeatStack = null;
        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(getApplicationContext());
        mGcmNetworkManager.cancelAllTasks(GCMConnectionRepeatService.class);
    }

    private void disconnectService()
    {
        if (connectionState.getConnectionStatus() != ConnectionStatus.DISCONNECTED
                && (connectionState.getConnectionStatus() != ConnectionStatus.CONNECTION_ERROR &&
                connectionState.getConnectionError() != ConnectionError.SIP_ERROR))
        {
            connectionState.setDisconnecting(true);
            SipService.unregisterSipService(getApplicationContext());
            if (!isServiceON(getApplicationContext()))
            {
                Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getApplicationContext().startActivity(intent);
            }
        }
        broadcastConnectionState();

    }
    private void resetConnectionService()
    {
        connectionState = new ConnectionState();
        connectionState.updateState(getApplicationContext());
        sharedPrefs.Save(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
    }


    private void loadCertAndPrivateKey()
    {
        KeystoreHandlingDefaultAccounts.InitCertFromResources(getApplicationContext());
    }
    private void onRSSIChanged()
    {
        int level = WifiUtils.getCurrentNetworkRSSI(getApplicationContext());
        connectionState.setRssiWifi(level);
        if (showRssiToast)
        {
            Toast.makeText(getApplicationContext(), "Level " + level, Toast.LENGTH_SHORT).show();
        }
        if (level > WifiSignalLevel.CONNECTION_LEVEL.getSignalLevel())
        {
            if (connectionState.getConnectionStatus() != ConnectionStatus.CONNECTED)
            {
                connectToService(false);
            }
            else if (connectionState.getConnectionError() == ConnectionError.WEAK_WIFI)
            {
                broadcastConnectionState();
            }
        }
        else if (level < WifiSignalLevel.DISCONNECTION_LEVEL.getSignalLevel() && connectionState.getConnectionStatus() == ConnectionStatus.CONNECTED)
        {
            if (!SipLib.getInstance().isActiveCall())
            {
                disconnectService();
            }
            else if (level < WifiSignalLevel.WEAK_CALL.getSignalLevel())
            {
                handleWeakCall();
            }
        }
    }
    private boolean preventMakeTone = false;
    private void handleWeakCall()
    {
        if (!preventMakeTone || connectionState.getConnectionError() != ConnectionError.WEAK_WIFI)
        {
            preventMakeTone = true;
            broadcastConnectionState();
            SipServiceCommand.makeTone(getApplicationContext());
            new Handler().postDelayed(() -> preventMakeTone = false, 4000);
        }
    }

    Handler handler = null;
    public void registerTimeout()
    {
        if (handler != null)
        {
            handler.removeCallbacks(null);
        }
        else
        {
            handler = new Handler();
        }
        handler.postDelayed(() ->
        {
            if (connectionState.getConnectionStatus() != ConnectionStatus.CONNECTED)
            {
                connectionState.setTimeout(true);
                broadcastConnectionState();
            }
        },5000);
    }

    public void connectToSIP()
    {
        SipServiceCommand.regAccount(getApplicationContext());
    }

    private static Boolean isServiceON = null;
    public static boolean isServiceON(Context context)
    {
        if (isServiceON == null)
        {
            SharedPrefs sharedPrefs = new SharedPrefs(context);
            isServiceON =  sharedPrefs.Load(SharedPrefs.KEY_SERVICE_ON, false);
        }
        return isServiceON;
    }
    public static void serServiceON(boolean on, Context context)
    {
        SharedPrefs sharedPrefs = new SharedPrefs(context);
        sharedPrefs.Save(SharedPrefs.KEY_SERVICE_ON, on);
        isServiceON = on;
        ((App)context.getApplicationContext()).updateNetworkMonitor(on);
    }
    public static void turnServiceOFF(Context context)
    {
        ConnectionService.serServiceON(false, context);
        ConnectionCommand.makeDisconnectIntent(context);
    }

    public void tryStartingCharon(final Context ctx)
    {
        int counter = sharedPrefs.Load(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
        sharedPrefs.Save(SharedPrefs.KEY_VPN_TRY_COUNTER, ++counter);
        VpnInit vpnInit = new VpnInit();
        VpnInit.IVpnInitCallback vpnInitCallback = new VpnInit.IVpnInitCallback()
        {
            @Override
            public void startActivity(final Intent intent, final int requestCode)
            {
                Log.d(this, "startActivity");
                Intent intentClear = new Intent(ctx, ActivityClear.class);
                intentClear.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intentClear);
            }

            @Override
            public void startService(final Intent intent)
            {
                ctx.startService(intent);
            }

            @Override
            public Context ctx()
            {
                return ctx;
            }
        };
        vpnInit.onVpnProfileSelected(vpnInitCallback, new VpnProfile());
    }


    private class VanStateReceiver extends VanStateMessenger
    {
        @Override
        public void stateChange(VpnStateService.State state, VpnStateService.ErrorState errorState)
        {
            super.stateChange(state, errorState);
            enqueueJob(() -> {
                if ( state == VpnStateService.State.ERROR && (errorState == VpnStateService.ErrorState.GENERIC_ERROR
                        || errorState == VpnStateService.ErrorState.AUTH_FAILED) && handleVPNError())
                {
                    return;
                }
                connectionState.setVpnState(state);
                connectionState.setmVpnErrorState(errorState);
                if (connectionState.getVpnState() != VpnStateService.State.CONNECTED){
                    if(connectionState.getVpnState() == VpnStateService.State.DISABLED && isServiceON(getApplicationContext()) )
                    {
                        SipServiceCommand.unRegAccount(getApplicationContext(), SipService.DE_INIT_PLACES.OTHER.ordinal());
                    }
                }
                else
                {
                    connectToSIP();
                }
                broadcastConnectionState();
            });
        }
    }
    public void handleExpiredCertificate()
    {
        App.getActivationComponent().registerListener(ConnectionService.this);
        App.getActivationComponent().generateNewCertificate(true);
    }
    public boolean handleVPNError()
    {
        SharedPrefs sharedPrefs = new SharedPrefs(getApplicationContext());
        int counter = sharedPrefs.Load(SharedPrefs.KEY_VPN_TRY_COUNTER, SharedPrefs.Defaults.DEFAULT_VPN_COUNTER);
        if(counter < Settings.VPN_AUTH_FAILD_COUNTER)
        {
            SipServiceCommand.deInitialize(getApplicationContext(), SipService.DE_INIT_PLACES.OTHER.ordinal());
            tryStartingCharon(getApplicationContext());
            return true;
        }
        return false;
    }

    private class SIPStateReceiver extends SipMessagesReceiver
    {
        @Override
        protected void OnRegState(int code, boolean isRegistered)
        {
            super.OnRegState(code, isRegistered);
            enqueueJob(() -> {
                if (connectionState.isDisconnecting())
                {
                    onSipDisconnectingState(code, isRegistered);
                }
                else
                {
                    onSipConnectionState(code, isRegistered);
                }
            });
        }

        public void onSipDisconnectingState(int code, boolean isRegistered)
        {
            if (!isRegistered && (code == pjsip_status_code.PJSIP_SC_OK.swigValue()
            || code == pjsip_status_code.PJSIP_SC_REQUEST_TIMEOUT.swigValue()
            || code == pjsip_status_code.PJSIP_SC_BAD_REQUEST.swigValue())
                    || code == pjsip_status_code.PJSIP_SC_FORBIDDEN.swigValue())
            {
                connectionState.setDisconnecting(false);
                SipServiceCommand.deInitialize(getApplicationContext(), SipService.DE_INIT_PLACES.OTHER.ordinal());
                CharonVpnService.stopVPN(getApplicationContext());
                if (!isServiceON(getApplicationContext())
                        && App.getActivationComponent().isActive())
                {
                    getApplicationContext().stopService(new Intent(getApplicationContext(),CharonVpnService.class));
                    getApplicationContext().stopService(new Intent(getApplicationContext(),SipService.class));
                    //ActivityInit.startInitActivity(getApplicationContext());
                }
            }
        }

        private void onSipConnectionState(int code, boolean isRegistered)
        {
            if (code ==  PJSIP_SC_FORBIDDEN.swigValue() && repeatSipConnectionCounter != MAX_REPEAT_COUNT)
            {
                //Toast.makeText(getApplicationContext(),"state " + PJSIP_SC_FORBIDDEN, Toast.LENGTH_SHORT).show();
                connectToSIP();
                repeatSipConnectionCounter++;
                return;
            }
            connectionState.setSipRegStatus(pjsip_status_code.swigToEnum(code));
            connectionState.setRegistered(isRegistered);
            broadcastConnectionState();
        }

    }
    @Override
    public void onNewCertificateSuccess(ApiResponse mDefaultResponse)
    {
        enqueueJob(() ->
        {
            connectToService(true);
            App.getActivationComponent().unRegisterListener(this);
        });
    }

    @Override
    public void onNewCertificateFailed(ApiResponse mDefaultResponse)
    {
        enqueueJob(this::broadcastConnectionState);
    }

    @Override
    public void onActivatedAccount()
    {

    }

    @Override
    public void onActivationAwaiting(ApiResponse mDefaultResponse)
    {

    }

    @Override
    public void onActivationFailed(ApiResponse mDefaultResponse)
    {

    }

    @Override
    public void onNewPasswordSuccess()
    {

    }

    @Override
    public void onNewPasswordAwaiting(ApiResponse mDefaultResponse)
    {

    }

    @Override
    public void onNewPasswordFailed(ApiResponse mDefaultResponse)
    {

    }
}
