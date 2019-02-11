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

package pl.orangelabs.wificalling.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_state;

import pl.orangelabs.wificalling.util.BroadcastHelper;

/**
 * Created by Cookie on 2016-09-29.
 */

public class BroadcastSipReceiver extends BroadcastReceiver
{

    final SipMessagesReceiver mSipMessagesReceiver;

    public BroadcastSipReceiver(final SipMessagesReceiver sipMessagesReceiver)
    {
        mSipMessagesReceiver = sipMessagesReceiver;
    }

    public void RegisterSipReceiver(final Context context)
    {
        BroadcastHelper.RegisterReceiver(context, BroadcastSipReceiver.this, SipMsg.ALL_ACTIONS);
    }

    public void UnRegisterSipReceiver(final Context context)
    {
        BroadcastHelper.UnRegisterReceiver(context, BroadcastSipReceiver.this);
    }


    public void registrationState(final Intent intent)
    {
        int regStatus = intent.getIntExtra(BroadcastSipSender.BroadcastParameters.CODE, -1);
        boolean isRegistered = intent.getBooleanExtra(BroadcastSipSender.BroadcastParameters.REGISTRATION, false);
        mSipMessagesReceiver.OnRegState(regStatus, isRegistered);
    }

    public void callState(final Context context, final Intent intent)
    {
        int callID = intent.getIntExtra(BroadcastSipSender.BroadcastParameters.CALL_ID, -1);
        pjsip_inv_state
            callState =
            pjsip_inv_state.swigToEnum(
                intent.getIntExtra(BroadcastSipSender.BroadcastParameters.CALL_STATE, pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.swigValue()));
        pjsip_status_code
            callStatus =
            pjsip_status_code.swigToEnum(
                intent.getIntExtra(BroadcastSipSender.BroadcastParameters.CALL_STATUS, pjsip_status_code.PJSIP_SC_DECLINE.swigValue()));
        boolean isLocalHold = intent.getBooleanExtra(BroadcastSipSender.BroadcastParameters.LOCAL_HOLD, false);
        boolean isLocalMute = intent.getBooleanExtra(BroadcastSipSender.BroadcastParameters.LOCAL_MUTE, false);
        long callDuration = intent.getLongExtra(BroadcastSipSender.BroadcastParameters.CALL_DURATION, 0L);
        boolean isSpeakerOn = intent.getBooleanExtra(BroadcastSipSender.BroadcastParameters.SPEAKER_ON, false);

        mSipMessagesReceiver.OnCallState(callID, callState, callStatus, isLocalHold, isLocalMute, callDuration, isSpeakerOn);

    }

    public void stackState(final Intent intent)
    {
        final pjsua_state stackState = pjsua_state.swigToEnum(intent.getIntExtra(BroadcastSipSender.BroadcastParameters.CODE, -1));
        final SipService.DE_INIT_PLACES
            from =
            SipService.DE_INIT_PLACES.values()[(intent.getIntExtra(BroadcastSipSender.BroadcastParameters.DE_INIT_FROM,
                SipService.DE_INIT_PLACES.OTHER.ordinal()))];
        mSipMessagesReceiver.OnStackState(stackState, from);
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        if (intent == null)
        {
            return;
        }
        String action = intent.getAction();

        if (action.equals(SipMsg.REG_STATE))
        {
            registrationState(intent);
        }
        if (action.equals(SipMsg.CALL_STATE))
        {
            callState(context, intent);
        }
        if (action.equals(SipMsg.STACK_STATE))
        {
            stackState(intent);
        }
    }
}
