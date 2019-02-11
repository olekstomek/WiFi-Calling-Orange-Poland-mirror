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

import pl.orangelabs.wificalling.util.OLPNotificationBuilder;

/**
 * Created by Cookie on 2016-09-29.
 */

public class SipServiceCommand
{
    protected static final String ACTION_MAKE_CALL = "sip_makeCall";
    protected static final String ACTION_GET_CALL_STATE = "sip_callState";
    protected static final String ACTION_HANG_UP_CALL = "sip_hangUpCall";
    protected static final String ACTION_MUTE_CALL = "sip_muteCall";
    protected static final String ACTION_SEND_DTMF = "sip_sendDTMF";
    protected static final String ACTION_ANSWER_CALL = "sip_answerCall";
    protected static final String ACTION_GET_REG_STATE = "sip_regState";
    protected static final String ACTION_REG_ACCOUNT = "sip_regAccount";
    protected static final String ACTION_UNREG_ACCOUNT = "sip_unRegAccount";
    protected static final String ACTION_REJECT_CALL = "sip_rejectCall";
    protected static final String ACTION_SIP_DEINIT = "sip_deinitialize";
    protected static final String ACTION_HOLD_CALL = "sip_holdCall";
    protected static final String ACTION_UNHOLD_CALL = "sip_unHoldCall";
    protected static final String ACTION_MAKE_ALERT_TONE = "sip_alert_tone";

    protected static final String ACTION_TOGGLE_SPEAKER = "toggle_speaker";
    protected static final String ACTION_SET_BLUETOOTH_HEADSET = "set_bluetooth";
    protected static final String ACTION_SET_SPEAKER = "set_speaker";
    protected static final String ACTION_MUTE_RING = "mute_ring";

    public static final String PARAM_NUMBER = "number";
    public static final String PARAM_WRONG_CALL_STATE = "wrong_call_state";
    public static final String PARAM_ID = "id";
    public static final String PARAM_TURN_ON = "value";
    public static final String PARAM_DTMF_DIGIT = "digit";
    public static final String PARAM_NOTIFICATION_ID = "notification_id";

    public static void makeCall(final Context context, final String numberToCall)
    {
        context.startService(makeCallIntent(context, numberToCall, -1, false));
    }
    public static void makeCall(final Context context, final String numberToCall, boolean wrongCallState)
    {
        context.startService(makeCallIntent(context, numberToCall, -1, wrongCallState));
    }

    public static Intent makeCallIntent(final Context context, final String numberToCall, final int notificationId, boolean wrongCallState)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_CALL);
        intent.putExtra(PARAM_NUMBER, numberToCall);
        intent.putExtra(PARAM_NOTIFICATION_ID, notificationId);
        intent.putExtra(PARAM_WRONG_CALL_STATE, wrongCallState);
        return intent;
    }


    public static void regAccount(final Context context)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_REG_ACCOUNT);
        context.startService(intent);
    }

    public static void getCallState(final Context context, final int callId)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_GET_CALL_STATE);
        intent.putExtra(PARAM_ID, callId);
        context.startService(intent);
    }

    public static void getRegState(final Context context)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_GET_REG_STATE);
        context.startService(intent);
    }

    public static void muteCall(final Context context, final int callId)
    {
//        Intent intent = new Intent(context, SipService.class);
//        intent.setAction(ACTION_MUTE_CALL);
//        intent.putExtra(PARAM_ID, callId);
        context.startService(muteCallIntent(context, callId));
    }

    public static Intent muteCallIntent(final Context context, final int callId) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MUTE_CALL);
        intent.putExtra(PARAM_ID, callId);
        return intent;
    }

    public static void toggleSpeaker(final Context context, final int callId) {
        context.startService(toggleSpeakerIntent(context, callId));
    }

    public static Intent toggleSpeakerIntent(final Context context, final int callId) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_TOGGLE_SPEAKER);
        intent.putExtra(PARAM_ID, callId);
        return intent;
    }
    public static void setSpeaker(final Context context, final int callId,boolean turnOn) {
        context.startService(setSpeakerIntent(context, callId,turnOn));
    }
    public static Intent setSpeakerIntent(final Context context, final int callId, boolean turnOn) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_SPEAKER);
        intent.putExtra(PARAM_ID, callId);
        intent.putExtra(PARAM_TURN_ON,turnOn);
        return intent;
    }

    public static void sendDtmf(final Context context, final int callId, final String dtmDigit)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SEND_DTMF);
        intent.putExtra(PARAM_ID, callId);
        intent.putExtra(PARAM_DTMF_DIGIT, dtmDigit);
        context.startService(intent);
    }

    public static void hangUpCall(final Context context, final int callId)
    {

//        Intent intent = new Intent(context, SipService.class);
//        intent.setAction(ACTION_HANG_UP_CALL);
//        intent.putExtra(PARAM_ID, callId);
        context.startService(hangUpCallIntent(context, callId, OLPNotificationBuilder.NOTIFICATION_ONGOING_CALL));
    }

    public static Intent hangUpCallIntent(final Context context, final int callId, int notificationId) {

        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_HANG_UP_CALL);
        intent.putExtra(PARAM_ID, callId);
        intent.putExtra(PARAM_NOTIFICATION_ID, notificationId);
        return intent;


    }

    public static void answerCall(final Context context, final int callId)
    {

        context.startService(answerCallIntent(context, callId, -1));

    }

    public static Intent answerCallIntent(final Context context, final int callId, final int notificationId) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_ANSWER_CALL);
        intent.putExtra(PARAM_ID, callId);
        intent.putExtra(PARAM_NOTIFICATION_ID, notificationId);
        return intent;
    }


    public static void rejectCall(final Context context, final int callId)
    {
        context.startService(rejectCallIntent(context, callId, -1));
    }

    public static Intent rejectCallIntent(final Context context, final int callId, final int notificationId) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_REJECT_CALL);
        intent.putExtra(PARAM_ID, callId);
        intent.putExtra(PARAM_NOTIFICATION_ID, notificationId);
        return intent;
    }


    public static void unRegAccount(final Context context, final int from)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_UNREG_ACCOUNT);
        intent.putExtra(PARAM_ID, from);
        context.startService(intent);
    }

    public static void deInitialize(final Context context, final int from)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SIP_DEINIT);
        intent.putExtra(PARAM_ID, from);
        context.startService(intent);
    }

    public static void holdAllCalls(final Context context)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_HOLD_CALL);
        context.startService(intent);
    }

    public static void unHoldAllCalls(final Context context)
    {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_UNHOLD_CALL);
        context.startService(intent);
    }
    public static void setBluetoothHeadset(final Context context, boolean turnOn) {
        context.startService(setBluetoothHeadsetIntent(context,turnOn));
    }
    public static Intent setBluetoothHeadsetIntent(final Context context, boolean turnOn) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_SET_BLUETOOTH_HEADSET);
        intent.putExtra(PARAM_TURN_ON,turnOn);
        return intent;
    }
    public static void makeTone(final Context context) {
        context.startService(makeToneIntent(context));
    }
    public static Intent makeToneIntent(Context context) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MAKE_ALERT_TONE);
        return intent;
    }
    public static void muteRing(final Context context) {
        context.startService(muteRingIntent(context));
    }
    public static Intent muteRingIntent(final Context context) {
        Intent intent = new Intent(context, SipService.class);
        intent.setAction(ACTION_MUTE_RING);
        return intent;
    }

}
