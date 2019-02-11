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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;

import pl.orangelabs.log.Log;
import pl.orangelabs.log.LogConfig;
import pl.orangelabs.wificalling.model.CallLogsContentObserver;
import pl.orangelabs.wificalling.service.activation_service.ActivationComponent;
import pl.orangelabs.wificalling.service.activation_service.ActivationComponentDefault;
import pl.orangelabs.wificalling.service.connection_service.ConnectionCommand;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStatus;
import pl.orangelabs.wificalling.service.connection_service.WifiSignalLevel;
import pl.orangelabs.wificalling.service.receiver.CallReceiver;
import pl.orangelabs.wificalling.service.receiver.ConnectivityReceiver;
import pl.orangelabs.wificalling.service.receiver.RSSIStateReceiver;
import pl.orangelabs.wificalling.service.receiver.WifiStateReceiver;
import pl.orangelabs.wificalling.util.MobileServiceStateHelper;
import pl.orangelabs.wificalling.util.Settings;
import pl.orangelabs.wificalling.util.StringUtil;
import pl.orangelabs.wificalling.util.WifiUtils;

/**
 * Created by marcin on 08.11.16.
 */

public class App extends Application implements Application.ActivityLifecycleCallbacks
{
    private static App instance;

    public static App get()
    {
        return instance;
    }

    private static ActivationComponent activationComponent;
    private static MobileServiceStateHelper mobileServiceStateHelper;
    private RSSIStateReceiver mRSSIStateReceiver = null;
    private WifiStateReceiver wifiStateReceiver = null;
    private ConnectivityReceiver connectivityReceiver = null;
    private NetworkCallback networkCallback = null;
    public static int activityCounter = 0;
    public static boolean isAnyActivityActive()
    {
        return activityCounter != 0;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;
        CallLogsContentObserver.getInstance().observe();
        activationComponent = ActivationComponentDefault.getInstance(getApplicationContext());
        updateNetworkMonitor(ConnectionService.isServiceON(getApplicationContext()));
        new LogConfig.Builder().Enabled(Settings.LOGGING_ENABLED).Prefix("VoWiFi").AppVersion(StringUtil.AppVersion(this)).Build();
        ExceptionHandler.Setup(getApplicationContext());
        mobileServiceStateHelper = new MobileServiceStateHelper(getApplicationContext());
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
    }

    public static ActivationComponent getActivationComponent()
    {
        return activationComponent;
    }

    public static MobileServiceStateHelper getMobileServiceStateHelper()
    {
        return mobileServiceStateHelper;
    }

    public void registerReceivers()
    {
        if (mRSSIStateReceiver == null)
        {
            mRSSIStateReceiver = new RSSIStateReceiver();
            registerReceiver(mRSSIStateReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        }
    }

    public void unRegisterReceivers()
    {
        if (mRSSIStateReceiver != null)
        {
            unregisterReceiver(mRSSIStateReceiver);
            mRSSIStateReceiver = null;
        }
    }

    public static void setActivationComponent(ActivationComponent activationComponent)
    {
        App.activationComponent = activationComponent;
    }

    public void updateNetworkMonitor(boolean isServiceOn)
    {
        updateConnectivityNetworkMonitorForAPI21AndUp(isServiceOn);
    }

    private void updateBroadcastReceivers(boolean isServiceOn)
    {
        if (isServiceOn)
        {
            if (connectivityReceiver == null)
            {
                connectivityReceiver = new ConnectivityReceiver();
                wifiStateReceiver = new WifiStateReceiver();
                registerReceiver(connectivityReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
                registerReceiver(wifiStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                registerReceivers();
            }
        }
        else
        {
            if (connectivityReceiver != null)
            {
                unregisterReceiver(connectivityReceiver);
                unregisterReceiver(wifiStateReceiver);
                connectivityReceiver = null;
                wifiStateReceiver = null;
            }
            unRegisterReceivers();
        }
    }

    @SuppressLint("NewApi")
    private void updateConnectivityNetworkMonitorForAPI21AndUp(Boolean isServiceON)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (isServiceON)
        {
            networkCallback = new NetworkCallback();
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            //builder.addTransportType(NetworkCapabilities.TRANSPORT_VPN);
            connectivityManager.registerNetworkCallback(
                builder.build(), networkCallback);
        }
        else
        {
            if (networkCallback != null)
            {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            }
            unRegisterReceivers();
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState)
    {

    }

    @Override
    public void onActivityStarted(Activity activity)
    {

    }

    @Override
    public void onActivityResumed(Activity activity)
    {
        activityCounter++;
    }

    @Override
    public void onActivityPaused(Activity activity)
    {
        activityCounter--;
    }

    @Override
    public void onActivityStopped(Activity activity)
    {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState)
    {

    }

    @Override
    public void onActivityDestroyed(Activity activity)
    {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class NetworkCallback extends ConnectivityManager.NetworkCallback
    {
        /**
         * @param network
         */
        @Override
        public void onAvailable(Network network)
        {
            Log.d(this,"Network Available");
            int level = WifiUtils.getCurrentNetworkRSSI(getApplicationContext());
            if (!WifiUtils.isVPNActive() && level > WifiSignalLevel.CONNECTION_LEVEL.getSignalLevel()
                && ConnectionService.getConnectionState().getConnectionStatus() != ConnectionStatus.CONNECTED)
            {
                ConnectionCommand.makeConnectIntent(getApplicationContext(), false);
            }
            registerReceivers();
        }

        /**
         * @param network
         */
        @Override
        public void onLost(Network network)
        {
            Log.d(this,"Network lost");
            if (ConnectionService.getConnectionState().getConnectionStatus() != ConnectionStatus.DISCONNECTED)
            {
                ConnectionCommand.makeDisconnectIntent(getApplicationContext());
            }
            else
            {
                ConnectionCommand.makeGetStateIntent(getApplicationContext());
            }
            unRegisterReceivers();
        }
    }
}
