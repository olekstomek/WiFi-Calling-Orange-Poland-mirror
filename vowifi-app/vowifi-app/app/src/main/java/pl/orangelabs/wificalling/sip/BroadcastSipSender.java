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

import android.content.Context;
import android.content.Intent;

import pl.orangelabs.wificalling.util.BroadcastHelper;

/**
 * Created by Cookie on 2016-09-29.
 */

public class BroadcastSipSender
{
    public class BroadcastParameters
    {
        public static final String ACCOUNT_ID = "account_id";
        public static final String CALL_ID = "call_id";
        public static final String CODE = "code";
        public static final String DE_INIT_FROM = "de_init_from";
        public static final String REMOTE_URI = "remote_uri";
        public static final String DISPLAY_NAME = "display_name";
        public static final String CALL_STATE = "call_state";
        public static final String CALL_STATUS = "call_status";
        public static final String NUMBER = "number";
        public static final String STACK_STARTED = "stack_started";
        public static final String LOCAL_HOLD = "local_hold";
        public static final String LOCAL_MUTE = "local_mute";
        public static final String CALL_DURATION = "call_duration";
        public static final String SPEAKER_ON = "speaker_on";
        public static final String REGISTRATION = "Registration";
    }

    private BroadcastHelper mBroadcastHelper;

    public BroadcastSipSender(final Context context)
    {
        mBroadcastHelper = new BroadcastHelper(context);
    }

    public void registrationState(final int registrationStateCode, final boolean isRegistered)
    {
        final Intent intent = new Intent();

        intent.setAction(SipMsg.REG_STATE);
        intent.putExtra(BroadcastParameters.CODE, registrationStateCode);
        intent.putExtra(BroadcastParameters.REGISTRATION, isRegistered);

        mBroadcastHelper.sendMessage(intent);
    }

    public void callState(int callID, int callStateCode, int callStatusCode, boolean isLocalHold, boolean isLocalMute, long callDuration, boolean isSpeakerOn)
    {
        final Intent intent = new Intent();

        intent.setAction(SipMsg.CALL_STATE);
        intent.putExtra(BroadcastParameters.CALL_ID, callID);
        intent.putExtra(BroadcastParameters.CALL_STATE, callStateCode);
        intent.putExtra(BroadcastParameters.CALL_STATUS, callStatusCode);
        intent.putExtra(BroadcastParameters.LOCAL_HOLD, isLocalHold);
        intent.putExtra(BroadcastParameters.LOCAL_MUTE, isLocalMute);
        intent.putExtra(BroadcastParameters.CALL_DURATION, callDuration);
        intent.putExtra(BroadcastParameters.SPEAKER_ON, isSpeakerOn);

        mBroadcastHelper.sendMessage(intent);
    }

    public void sipStackState(final int stackCode, final SipService.DE_INIT_PLACES deinitPlaces)
    {
        final Intent intent = new Intent();

        intent.setAction(SipMsg.STACK_STATE);
        intent.putExtra(BroadcastParameters.CODE, stackCode);
        intent.putExtra(BroadcastParameters.DE_INIT_FROM, deinitPlaces.ordinal());
        mBroadcastHelper.sendMessage(intent);
    }

}
