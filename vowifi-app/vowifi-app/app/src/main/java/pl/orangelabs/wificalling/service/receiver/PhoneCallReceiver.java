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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import java.util.Date;


/**
 * @author Cookie
 */
public abstract class PhoneCallReceiver extends BroadcastReceiver
{

    private static final String INTENT_OUTGOING_CALL_ACTION = "android.intent.action.NEW_OUTGOING_CALL";
    private static final String INTENT_EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent)
    {

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
        {
            savedNumber = intent.getExtras().getString(INTENT_EXTRA_PHONE_NUMBER);
            onMakeCall(context, savedNumber, new Date());
        }
        else
        {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr != null)
            {
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE))
                {
                    state = TelephonyManager.CALL_STATE_IDLE;
                }
                else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
                {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                }
                else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING))
                {
                    state = TelephonyManager.CALL_STATE_RINGING;
                }
            }
            onCallStateChanged(context, state, number);
        }
    }

    protected abstract void onMakeCall(Context ctx, String number, Date start);

    protected abstract void onIncomingCallStarted(Context ctx, String number, Date start);

    protected abstract void onAnswerCall(Context ctx, String number, Date start);

    protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);

    protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

    protected abstract void onMissedCall(Context ctx, String number, Date start);

    void onCallStateChanged(Context context, int state, String number)
    {
        if (lastState == state)
        {
            return;
        }
        switch (state)
        {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number, callStartTime);

                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (lastState != TelephonyManager.CALL_STATE_RINGING)
                {
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                }
                else
                {
                    onAnswerCall(context, savedNumber, new Date());
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (lastState == TelephonyManager.CALL_STATE_RINGING)
                {
                    onMissedCall(context, savedNumber, callStartTime);
                }
                else if (isIncoming)
                {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                else
                {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }
}
