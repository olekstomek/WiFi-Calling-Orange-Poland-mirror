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

package pl.orangelabs.wificalling.util;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

import java.util.LinkedList;
import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.BuildConfig;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.model.ContactCall;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionState;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStatus;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.view.ActivityCall;
import pl.orangelabs.wificalling.view.ActivityIncomingCall;
import pl.orangelabs.wificalling.view.ActivityInit;
import pl.orangelabs.wificalling.view.ActivityMain;

/**
 * @author Cookie
 */

public class OLPNotificationBuilder {
    public static final int NOTIFICATION_MISSED_CALL_ID = 101;
    public static final int NOTIFICATION_INCOMING_CALL = 99;
    public static final int NOTIFICATION_MAIN = 99;
    public static final int NOTIFICATION_ONGOING_CALL = 99;
    private NotificationCompat.Builder mNotificationBuilder;
    public static boolean showAllNotifications = BuildConfig.SHOW_ALL_NOTIFICATION;

    public OLPNotificationBuilder(final Context context) {
        mNotificationBuilder =
                new NotificationCompat.Builder(context);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_app_logo).setContentTitle(context.getString(R.string.app_name));
    }

    public void putMissedManyCallsNotification(boolean isOneCall, int count, String displayName, String number)
    {
        final Context context = mNotificationBuilder.mContext;

        PendingIntent pendingIntent = getPendingIntentCalls(context);

        mNotificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorAccent));
        mNotificationBuilder.setSmallIcon(R.drawable.ic_missed).setAutoCancel(true);
        if (isOneCall)
        {
            mNotificationBuilder.setContentTitle(context.getString(R.string.notification_call_missed));
            mNotificationBuilder.setContentText(displayName);
        }
        else
        {
            mNotificationBuilder.setContentTitle(context.getString(R.string.notification_many_calls_missed_title));
            final String callListText = context.getResources().getQuantityString(R.plurals.notificiation_many_calls_missed_text, count, count);
            mNotificationBuilder.setContentText(callListText);
        }
        mNotificationBuilder.setContentIntent(pendingIntent);
        if (displayName != null)
        {
            final Intent makeCallIntent = SipServiceCommand.makeCallIntent(context, number, NOTIFICATION_MISSED_CALL_ID, false);
            makeCallIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            final PendingIntent actionCall = PendingIntent.getService(context, 0, makeCallIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            final NotificationCompat.Action
                    actionCallBack =
                    new NotificationCompat.Action(R.drawable.ic_call, context.getString(R.string.notification_missed_action_call_back), actionCall);
            mNotificationBuilder.addAction(actionCallBack);
            if (!isOneCall)
            {
                mNotificationBuilder.setContentInfo(displayName);
            }
        }

        PutNotification(NOTIFICATION_MISSED_CALL_ID);
    }

    private PendingIntent getPendingIntentCalls(Context context)
    {
        final Intent missedCallIntent = new Intent();
        missedCallIntent.setAction(Intent.ACTION_VIEW);
        missedCallIntent.setType(CallLog.Calls.CONTENT_TYPE);
        return PendingIntent.getActivity(context, 0, missedCallIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void updateMissedCallNotifications(List<VWCallLog> callLogList, Context context)
    {
        List<VWCallLog> tempCallLogs = new LinkedList<>();
        for (VWCallLog vwCallLog : callLogList)
        {
            if (vwCallLog.mType == CallLog.Calls.MISSED_TYPE && vwCallLog.mNew){
                tempCallLogs.add(vwCallLog);
            }
        }
        String displayName;
        boolean isOneCall = false;
        if (tempCallLogs.size() == 1)
        {
            isOneCall = true;
            displayName =  tempCallLogs.get(0).mDisplayName;
        }
        else if (tempCallLogs.size() > 1 && VWCallLog.isOneNumber(tempCallLogs))
        {
            displayName = tempCallLogs.get(0).mDisplayName;
        }
        else
        {
            CancelNotification(context, NOTIFICATION_MISSED_CALL_ID);
            return;
        }
        OLPNotificationBuilder builder = new OLPNotificationBuilder(context);
        builder.putMissedManyCallsNotification(isOneCall, tempCallLogs.size(), displayName, tempCallLogs.get(0).mPhoneNumber);
    }

    public void PutIncomingCallNotification(final int callId,int callType, String number) {
        final Context context = mNotificationBuilder.mContext;

        final Intent answerCallIntent = SipServiceCommand.answerCallIntent(context, callId, NOTIFICATION_INCOMING_CALL);
        answerCallIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final Intent rejectCallIntent = SipServiceCommand.rejectCallIntent(context, callId, NOTIFICATION_INCOMING_CALL);
        rejectCallIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent actionAnswerCall = PendingIntent.getService(context, 0, answerCallIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent actionRejectCall = PendingIntent.getService(context, 0, rejectCallIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // TODO: 10/25/2016 ikony do zmiany
        NotificationCompat.Action actionAnswer = new NotificationCompat.Action(R.drawable.ic_action_phone_start,
                context.getString(R.string.notification_call_answer), actionAnswerCall);

        NotificationCompat.Action actionReject = new NotificationCompat.Action(R.drawable.ic_action_phone_missed,
                context.getString(R.string.notification_call_reject), actionRejectCall);
        PendingIntent mPendingIntent = getPendingIntentActivityCall(callId, callType, number, mNotificationBuilder.mContext, ActivityIncomingCall.class);

        mNotificationBuilder.setContentTitle(ContactCall.loadDetails(context,number).mDisplayName)

                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setContentIntent(mPendingIntent)
                .setSmallIcon(R.drawable.ic_status)
                .setOnlyAlertOnce(true)
                .setContentText(context.getString(R.string.notification_call_incoming))
                .addAction(actionAnswer)
                .setAutoCancel(false)
                .addAction(actionReject)
                .setPriority(Notification.PRIORITY_MAX);


        PutNotification(NOTIFICATION_INCOMING_CALL);


    }
    public Notification GetMainNotification(@NonNull ConnectionState connectionState)
    {
        return GetMainNotification(getMainNotificationText(connectionState), connectionState.getConnectionStatus() == ConnectionStatus.CONNECTED);
    }
    public Notification GetMainNotification(@Nullable String contentText, boolean isOn)
    {
        final Context context = mNotificationBuilder.mContext;
        mNotificationBuilder.setContentTitle(context.getString(R.string.app_name));
        mNotificationBuilder.setColor(ContextCompat.getColor(context, isOn ? R.color.colorAccent : android.R.color.darker_gray));
        if (contentText != null){
            mNotificationBuilder.setContentText(contentText);
        }
        mNotificationBuilder.setSmallIcon(isOn ? R.drawable.ic_status : R.drawable.ic_status_app_cross);
        mNotificationBuilder.setOnlyAlertOnce(true);
        Class<? extends Activity> classIntent;
        if (isOn)
        {
            classIntent = ActivityMain.class;
        }
        else
        {
            classIntent = ActivityInit.class;
        }
        Intent notificationIntent = new Intent(context, classIntent);
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder.setContentIntent(mPendingIntent);

        return mNotificationBuilder.build();
    }


    public void PutOngoingCallNotification(final int callId,int callType, String number, long when, boolean speaker, boolean mute) {

        final Context context = mNotificationBuilder.mContext;
        RemoteViews remoteViews = getRemoteViews(callId, speaker,number, context.getString(R.string.notification_call_ongoing), mute, context);
        PendingIntent mPendingIntent = getPendingIntentActivityCall(callId,callType, number, mNotificationBuilder.mContext, ActivityCall.class);
        NotificationCompat.DecoratedCustomViewStyle decoratedCustomViewStyle = new NotificationCompat.DecoratedCustomViewStyle();
        mNotificationBuilder
                .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                .setStyle(decoratedCustomViewStyle)
                .setContentIntent(mPendingIntent)
                .setCustomContentView(remoteViews)
                .setSmallIcon(R.drawable.ic_status)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setWhen(when)
                .setUsesChronometer(true) // for timer
                .setPriority(Notification.PRIORITY_MAX);


        PutNotification(NOTIFICATION_ONGOING_CALL);
    }

    @NonNull
    private RemoteViews getRemoteViews(int callId, boolean speaker, String title, String content, boolean mute, Context context)
    {
        final Intent hangUpIntent = SipServiceCommand.hangUpCallIntent(context, callId, NOTIFICATION_ONGOING_CALL);
        hangUpIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final Intent muteIntent = SipServiceCommand.muteCallIntent(context, callId);
        muteIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final Intent speakerIntent = SipServiceCommand.toggleSpeakerIntent(context, callId);
        speakerIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int speakerIcon = speaker ? R.drawable.ic_volume_up_marked_48px : R.drawable.ic_volume_up_black_48px;
        int muteIcon = mute ? R.drawable.ic_mic_off_marked_48px : R.drawable.ic_mic_off_black_24px;
        PendingIntent hangUpPendingIntent = PendingIntent.getService(context, 0, hangUpIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent mutePendingIntent = PendingIntent.getService(context, 0, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent speakerPendingIntent = PendingIntent.getService(context, 0, speakerIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_custom_buttons);
        remoteViews.setOnClickPendingIntent(R.id.btn_speaker,speakerPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.btn_end_call, hangUpPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.btn_mute, mutePendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.tv_end_call,hangUpPendingIntent);
        remoteViews.setImageViewResource(R.id.btn_speaker, speakerIcon);
        remoteViews.setImageViewResource(R.id.btn_mute, muteIcon);
        remoteViews.setTextViewText(R.id.tv_title, title);
        remoteViews.setTextViewText(R.id.tv_content, content);
        return remoteViews;
    }

    private PendingIntent getPendingIntentActivityCall(int callId,int callType, String number, Context context,  Class<?> cls)
    {
        Bundle bundle = new Bundle();
        bundle.putInt(ActivityCall.param_CALL_ID, callId);
        bundle.putInt(ActivityCall.param_CALL_TYPE, callType);
        bundle.putString(SipServiceCommand.PARAM_NUMBER, number);
        Intent notificationIntent = new Intent(context, cls);
        notificationIntent.putExtras(bundle);
        return PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void PutNotification(int notificationId) {
        final Context context = mNotificationBuilder.mContext;
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification n = mNotificationBuilder.build();
        notificationManager.notify(notificationId, n);
    }

    public static void CancelNotification(final Context context, int notificationId) {
        Log.d(Log.STATIC_CTX, "CancelNotification " + notificationId);

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        CloseSystemDialogs(context);
    }

    private static void CloseSystemDialogs(final Context context) {
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }
   public String getMainNotificationText(ConnectionState connectionState)
    {
        String contentText = null;
        switch (connectionState.getConnectionStatus())
        {
            case CONNECTED:
                contentText = mNotificationBuilder.mContext.getString(R.string.nt_active_state);
                break;
            case DISCONNECTING:
                if (showAllNotifications)
                contentText = mNotificationBuilder.mContext.getString(R.string.nt_disconnecting);
                break;
            case DISCONNECTED:
                if (showAllNotifications)
                contentText = mNotificationBuilder.mContext.getString(R.string.nt_disabled_state);
                break;
            case CONNECTION_ERROR:
                switch (connectionState.getConnectionError()){
                    case NO_WIFI:
                        if (showAllNotifications)
                        contentText = getErrorText(R.string.nt_no_wifi);
                        break;
                    case NO_INTERNET_CONNECTION:
                        if (showAllNotifications)
                        contentText =  getErrorText(R.string.nt_no_internet_connection);
                        break;
                    case NO_NETWORK:
                        if (showAllNotifications)
                        contentText = getErrorText(R.string.wifi_waiting);
                        break;
                    case VPN_PERMISSION_CANCELED:
                        if (showAllNotifications)
                        contentText = getErrorText(R.string.nt_permission_canceled);
                        break;
                    case WEAK_WIFI:
                        contentText = getErrorText(R.string.nt_weak_wifi);
                        break;
                    default:
                        if (showAllNotifications)
                        contentText = getErrorText(R.string.nt_error_connecting);
                }
                break;
            case CONNECTING:
                if (showAllNotifications)
                contentText = mNotificationBuilder.mContext.getString(R.string.nt_connecting);
                break;
        }
        return contentText;
    }
    public String getErrorText(int text)
    {
        return mNotificationBuilder.mContext.getString(text);
    }
    public void UpdateMainNotification(ConnectionState connectionState)
    {
        if (ConnectionService.isServiceON(mNotificationBuilder.mContext) || connectionState.getConnectionStatus() == ConnectionStatus.DISCONNECTING)
        {
            String contentText = getMainNotificationText(connectionState);
            if (contentText == null)
            {
                if (!showAllNotifications)
                {
                    CancelNotification(mNotificationBuilder.mContext, NOTIFICATION_MAIN);
                }
                return;
            }
            GetMainNotification(contentText,connectionState.getConnectionStatus() == ConnectionStatus.CONNECTED);
            PutNotification(NOTIFICATION_MAIN);
        }
    }


}
