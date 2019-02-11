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

package pl.orangelabs.wificalling.service.receiver;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

import java.util.Date;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStatus;
import pl.orangelabs.wificalling.sip.SipService;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;

public class CallReceiver extends PhoneCallReceiver
{
    private final Object mServiceLock = new Object();

    /***
     * hotfix for wrong state CALL_STATE_OFF_HOOK for Android 5 - Sony before onMakeCall
     */
    private static boolean wrongCallState = false;
    boolean goodMethodSequence = false;
    
    @Override
    protected void onMakeCall(final Context ctx, final String number, final Date start)
    {
        Log.d(this, "onMakeCall");
        if (!isVowifiCallPossible(ctx))
        {
            return;
        }
        TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (OLPPhoneNumberUtils.shouldCallEmergencyNativeFromOutsideApp(number) || OLPPhoneNumberUtils.isUSSDNumber(number) || handleNativeCallRequest(number))
        {
            Log.d(this, "onMakeCall call state native");
        }
        else if (manager.getCallState() != TelephonyManager.CALL_STATE_IDLE && !wrongCallState)
        {
            Log.d(this,"onMakeCall call state not idle");
        }
        else
        {
            Log.i(this, "VoWifi can handle this request, let's ask user what he wants");
            setResultData(null);
            SipServiceCommand.makeCall(ctx, number, wrongCallState);
        }
        if (!wrongCallState)
        {
            goodMethodSequence = true;
        }
        wrongCallState = false;
        // else ignore (system default action will be executed)
    }

    private boolean handleNativeCallRequest(final String number)
    {
        final String nativeReqNumber = SipService.getNumberToCallNativeDuringNextReceive();
        if (nativeReqNumber != null && nativeReqNumber.equals(number))
        {
            Log.i(this, "Found native request match for " + number);
            SipService.setNumberToCallNativeDuringNextReceive(null);
            return true;
        }
        Log.i(this, "No native request match for " + number);
        return false;
    }

    private boolean isVowifiCallPossible(final Context ctx)
    {
        return ConnectionService.getConnectionState().getConnectionStatus() == ConnectionStatus.CONNECTED;//&& checkSipReg(ctx);
    }

    @Override
    protected void onIncomingCallStarted(final Context ctx, final String number, final Date start)
    {
        Log.d(this, "onIncomingCallStarted");
        SipServiceCommand.holdAllCalls(ctx);
    }

    @Override
    protected void onAnswerCall(final Context ctx, final String number, final Date start)
    {
        Log.d(this, "onAnswerCall");
        SipServiceCommand.holdAllCalls(ctx);
    }

    @Override
    protected void onOutgoingCallStarted(final Context ctx, final String number, final Date start)
    {
        Log.d(this, "onOutgoingCallStarted");
        if (!goodMethodSequence)
        {
            wrongCallState = true;
        }
        SipServiceCommand.holdAllCalls(ctx);
    }

    @Override
    protected void onIncomingCallEnded(final Context ctx, final String number, final Date start, final Date end)
    {
        Log.d(this, "onIncomingCallEnded");
        SipServiceCommand.unHoldAllCalls(ctx);
    }

    @Override
    protected void onOutgoingCallEnded(final Context ctx, final String number, final Date start, final Date end)
    {
        Log.d(this, "onOutgoingCallEnded");
        SipServiceCommand.unHoldAllCalls(ctx);
    }

    @Override
    protected void onMissedCall(final Context ctx, final String number, final Date start)
    {
        Log.d(this, "onMissedCall");
    }

    private boolean checkWifi(final Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null)
        {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null)
            {
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected())
                {
                    return true;
                }
            }
        }
        return false;
    }

// TODO: 2016-11-29 remove it
//    private boolean checkSipReg(final Context contexts)
//    {
//        try
//        {
//            final SipAccount sipAccount = SipLib.getInstance().getSipAccount();
//            return sipAccount != null && sipAccount.getInfo().getRegStatus() == pjsip_status_code.PJSIP_SC_OK;
//        }
//        catch (Exception e)
//        {
//            Log.d(this, "App error", e);
//            return false;
//        }
//
//    }
}