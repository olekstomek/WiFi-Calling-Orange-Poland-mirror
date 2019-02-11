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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.pjsip.pjsua2.pjsip_status_code;

import java.io.Serializable;
import java.util.Date;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.SettingsApp;
import pl.orangelabs.wificalling.VpnStateService;
import pl.orangelabs.wificalling.service.activation_service.ActivationState;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.util.WifiUtils;

import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_OK;
import static pl.orangelabs.wificalling.sip.SipService.isSIPRegisterError;

/**
 * Created by marcin on 23.11.16.
 */

public class ConnectionState implements Serializable
{
    private  VpnStateService.State mVpnState = VpnStateService.State.DISABLED;
    private  VpnStateService.ErrorState mVpnErrorState = VpnStateService.ErrorState.NO_ERROR;
    private  pjsip_status_code sipRegStatus;
    private  ConnectionStatus connectionStatus = ConnectionStatus.NON;
    private  ConnectionError connectionError = ConnectionError.NON;
    private int rssiWifi = 0;
    private boolean isRegistered  = false;
    private boolean timeout = false;
    private boolean isDisconnecting = false;

    public VpnStateService.State getVpnState()
    {
        return mVpnState;
    }

    public void setVpnState(VpnStateService.State mVpnState)
    {
        this.mVpnState = mVpnState;
    }

    public VpnStateService.ErrorState getVpnErrorState()
    {
        return mVpnErrorState;
    }

    public void setmVpnErrorState(VpnStateService.ErrorState mVpnErrorState)
    {
        this.mVpnErrorState = mVpnErrorState;
    }

    public pjsip_status_code getSipRegStatus()
    {
        return sipRegStatus;
    }

    public void setSipRegStatus(pjsip_status_code sipRegStatus)
    {
        this.sipRegStatus = sipRegStatus;
    }

    public ConnectionStatus getConnectionStatus()
    {
        return connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus)
    {
        this.connectionStatus = connectionStatus;
    }

    public ConnectionError getConnectionError()
    {
        return connectionError;
    }

    public void setConnectionError(ConnectionError connectionError)
    {
        this.connectionError = connectionError;
    }

    public int getRssiWifi()
    {
        return rssiWifi;
    }

    public void setRssiWifi(int rssiWifi)
    {
        this.rssiWifi = rssiWifi;
    }

    public boolean isRegistered()
    {
        return isRegistered;
    }

    public void setRegistered(boolean registered)
    {
        isRegistered = registered;
    }

    public ConnectionState updateState(Context context)
    {
        if (isDisconnecting())
        {
            setConnectionStatus(ConnectionStatus.DISCONNECTING);
            setConnectionError(ConnectionError.NON);
        }
        else if (VpnService.prepare(context) != null)
        {
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            setConnectionError(ConnectionError.VPN_PERMISSION_CANCELED);
        }
        else if (!ConnectionService.isServiceON(context))
        {
            setConnectionStatus(ConnectionStatus.INACTIVE);
            setConnectionError(ConnectionError.NON);
        }
        else if (getVpnState() == VpnStateService.State.DISABLED || getVpnState() == VpnStateService.State.ERROR)
        {
            return getConnectionStateOnDisabledVPN(context);
        }
        else if(getVpnState() == VpnStateService.State.CONNECTING)
        {
            setConnectionStatus(ConnectionStatus.CONNECTING);
            setConnectionError(ConnectionError.NON);
        }
        else if (getVpnState() == VpnStateService.State.DISCONNECTING)
        {
            setConnectionStatus(ConnectionStatus.DISCONNECTING);
            setConnectionError(ConnectionError.NON);
        }
        else
        {
            return getConnectionStateOnConnectedVPN();
        }
        return this;
    }

    private ConnectionState getConnectionStateOnDisabledVPN(Context context)
    {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo networkInfo =   wifi.getConnectionInfo();
        if (!wifi.isWifiEnabled()) //no wifi
        {
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            setConnectionError(ConnectionError.NO_WIFI);
        }
        else if (wifi.isWifiEnabled() && !isWifiNetworkConnected(context))
        {
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            setConnectionError(ConnectionError.NO_NETWORK);
        }
        else if (networkInfo.getRssi() < WifiSignalLevel.DISCONNECTION_LEVEL.getSignalLevel())
        {
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            setConnectionError(ConnectionError.WEAK_WIFI);
        }
        else if (!WifiUtils.isInternetConnectionAvailable(context))
        {
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            setConnectionError(ConnectionError.NO_INTERNET_CONNECTION);
        }
        else if (isCertificateOutOfDate())
        {
            if (App.getActivationComponent().getActivationState() != ActivationState.CERT_CHANGING)
            {
                setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
            }
            else
            {
                setConnectionStatus(ConnectionStatus.CONNECTING);
            }
            setConnectionError(ConnectionError.VPN_CERT_OUT_OF_DATE);
        }
        else if (SettingsApp.isProd && !App.getActivationComponent().getActivationDataKeeper().loadImsi().equals(Utils.getUserImsi(context)))
        {
            setConnectionError(ConnectionError.SIM_CHANGED);
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
        }
        else if (getVpnErrorState() != VpnStateService.ErrorState.NO_ERROR)
        {

            setConnectionError(ConnectionError.VPN_ERROR);
            setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);

        }
        else
        {
            if (getConnectionStatus() != ConnectionStatus.CONNECTING || isTimeout())
            {
                setConnectionStatus(ConnectionStatus.DISCONNECTED);
                setConnectionError(ConnectionError.NON);
            }
        }
        return this;
    }
    public boolean isWifiNetworkConnected(Context context)
    {
        ConnectivityManager conMan = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        for (Network network: conMan.getAllNetworks())
        {
            NetworkInfo networkInfo = conMan.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected())
            {
                return true;
            }
        }
        return false;
    }

    private ConnectionState getConnectionStateOnConnectedVPN()
    {

        if (getSipRegStatus() != null)
        {
            if (getSipRegStatus() == PJSIP_SC_OK && isRegistered())
            {
                setConnectionStatus(ConnectionStatus.CONNECTED);
                if (rssiWifi < WifiSignalLevel.WEAK_CALL.getSignalLevel())
                {
                    setConnectionError(ConnectionError.WEAK_WIFI);
                }
                else
                {
                    setConnectionError(ConnectionError.NON);
                }
            }
            else if (isSIPRegisterError(getSipRegStatus(), isRegistered()))
            {
                setConnectionStatus(ConnectionStatus.CONNECTION_ERROR);
                setConnectionError(ConnectionError.SIP_ERROR);
            }
            else
            {
                setConnectionStatus(ConnectionStatus.CONNECTING);
                setConnectionError(ConnectionError.NON);
            }
        }
        else
        {
            setConnectionStatus(ConnectionStatus.CONNECTING);
            setConnectionError(ConnectionError.NON);
        }
        return this;
    }
    public boolean isCertificateOutOfDate()
    {
            return new Date().getTime() > App.getActivationComponent().getActivationDataKeeper().loadCertificateExpirationDate();
    }
    @Override
    public String toString()
    {
        return "Connection status: " + getConnectionStatus() + " Connection Error: "
                + getConnectionError() + " VpnState: " + getVpnState()
                + " VpnStateError: " + getVpnErrorState() + " SipStatus: " + getSipRegStatus();
    }

    public boolean isTimeout()
    {
        return timeout;
    }

    public void setTimeout(boolean timeout)
    {
        this.timeout = timeout;
    }

    public boolean isDisconnecting()
    {
        return isDisconnecting;
    }

    public void setDisconnecting(boolean disconnecting)
    {
        isDisconnecting = disconnecting;
    }
}
