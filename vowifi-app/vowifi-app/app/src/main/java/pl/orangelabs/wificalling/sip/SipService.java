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


import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;
import org.pjsip.pjsua2.pjsua_state;

import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.model.db.AsyncMarkAllCallsAsRead;
import pl.orangelabs.wificalling.service.BackgroundService;
import pl.orangelabs.wificalling.service.connection_service.ConnectionCommand;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.util.AudioManagerHelper;
import pl.orangelabs.wificalling.util.IntentUtil;
import pl.orangelabs.wificalling.util.OLPNotificationBuilder;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.WifiUtils;
import pl.orangelabs.wificalling.view.ActivityCall;
import pl.orangelabs.wificalling.view.ActivityIncomingCall;

import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_BAD_REQUEST;
import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_INTERNAL_SERVER_ERROR;
import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_NOT_FOUND;
import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_OK;
import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_REQUEST_TIMEOUT;
import static org.pjsip.pjsua2.pjsip_status_code.PJSIP_SC_SERVICE_UNAVAILABLE;
import static pl.orangelabs.wificalling.sip.SipServiceCommand.PARAM_TURN_ON;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.CancelNotification;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.NOTIFICATION_MAIN;
import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.NOTIFICATION_MISSED_CALL_ID;

public class SipService extends BackgroundService
{
    public static String sNumberToCallNativeDuringNextReceive;

    private BroadcastSipSender mBroadcastHelper;
    private static SipLib mLib;

    private static final int NOTIFICATION_ID = 1;

    private SharedPrefs mPrefs;
    private OLPNotificationBuilder mNotificationBuilder;

    public AudioManagerHelper getAudioManagerHelper()
    {
        return audioManagerHelper;
    }

    private AudioManagerHelper audioManagerHelper;


    @Override
    public void onCreate()
    {
        super.onCreate();
        mNotificationBuilder = new OLPNotificationBuilder(SipService.this);

        Log.d(this, "onCreate");

        mPrefs = new SharedPrefs(this);

        enqueueJob(() ->
        {
            audioManagerHelper = new AudioManagerHelper(getApplicationContext());
            mBroadcastHelper = new BroadcastSipSender(SipService.this);

            loadLib();
            mLib = SipLib.getInstance();
        });
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId)
    {
        if (intent != null)
        {
            enqueueJob(() ->
            {

                String action = intent.getAction();
                if (SipServiceCommand.ACTION_REG_ACCOUNT.equals(action))
                {
                    regAccount(intent);
                }
                else if (SipServiceCommand.ACTION_MAKE_CALL.equals(action))
                {
                    makeCall(intent);
                }
                else if (SipServiceCommand.ACTION_GET_CALL_STATE.equals(action))
                {
                    getCallState(intent);
                }
                else if (SipServiceCommand.ACTION_GET_REG_STATE.equals(action))
                {
                    getRegState();
                }
                else if (SipServiceCommand.ACTION_HANG_UP_CALL.equals(action))
                {
                    hungUpCall(intent);
                }
                else if (SipServiceCommand.ACTION_MUTE_CALL.equals(action))
                {
                    muteCall(intent);
                }
                else if (SipServiceCommand.ACTION_ANSWER_CALL.equals(action))
                {
                    answerCall(intent);
                }
                else if (SipServiceCommand.ACTION_REJECT_CALL.equals(action))
                {
                    rejectCall(intent);
                }
                else if (SipServiceCommand.ACTION_UNREG_ACCOUNT.equals(action))
                {
                    unRegAccount(intent);
                }
                else if (SipServiceCommand.ACTION_SEND_DTMF.equals(action))
                {
                    sendDTMF(intent);
                }
                else if (SipServiceCommand.ACTION_TOGGLE_SPEAKER.equals(action))
                {
                    toggleSpeaker(intent);
                }
                else if (SipServiceCommand.ACTION_SET_SPEAKER.equals(action))
                {
                    setSpeaker(intent);
                }
                else if (SipServiceCommand.ACTION_SIP_DEINIT.equals(action))
                {
                    deinitialize(intent);
                }
                else if (SipServiceCommand.ACTION_HOLD_CALL.equals(action))
                {
                    holdCalls(intent);
                }
                else if (SipServiceCommand.ACTION_UNHOLD_CALL.equals(action))
                {
                    unHoldCalls(intent);
                }
                else if (SipServiceCommand.ACTION_SET_BLUETOOTH_HEADSET.equals(action))
                {
                    setBluetoothHeadset(intent);
                }
                else if (SipServiceCommand.ACTION_MAKE_ALERT_TONE.equals(action))
                {
                    makeTone();
                }
                else if (SipServiceCommand.ACTION_MUTE_RING.equals(action))
                {
                    muteRing();
                }
            });
        }
        return START_NOT_STICKY;
    }

    private void muteRing()
    {
        getAudioManagerHelper().muteRing();
    }


    @Override
    public void onDestroy()
    {
        enqueueJob(() ->
        {
            Log.d(this, "Destroying SipService");
            mLib.DeInitialize(SipService.this);
            final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_MAIN);

        });

        super.onDestroy();
    }

    private void loadLib()
    {
        try
        {
            System.loadLibrary("opencore-amrnb");
            System.loadLibrary("opencore-amrwb");
            System.loadLibrary("vo-amrwbenc");
            System.loadLibrary("pjsua2");
            Log.d(this, "PJSIP pjsua2 loaded");
        }
        catch (UnsatisfiedLinkError error)
        {
            Log.e(this, "Error while loading PJSIP pjsua2 native library: ", error);
        }
    }

    public void updateNotification()
    {
        ConnectionCommand.makeUpdateNotificationIntent(getApplicationContext());
    }

    public BroadcastSipSender getBroadcastHelper()
    {
        if (mBroadcastHelper == null)
        {
            mBroadcastHelper = new BroadcastSipSender(SipService.this);
        }
        return mBroadcastHelper;
    }

    PowerManager.WakeLock wakeLock;

    public void updateService()
    {
        AccountInfo info = null;
        if (mLib.getSipAccount() != null)
        {
            try
            {
                info = mLib.getSipAccount().getInfo();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if (info != null && info.getRegStatus() == PJSIP_SC_OK && info.getRegIsActive())
            {
                startForeground(NOTIFICATION_MAIN, mNotificationBuilder.GetMainNotification(mNotificationBuilder.getMainNotificationText(ConnectionService.getConnectionState()), true));
                PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
                wakeLock.acquire();
            }
            else
            {
                if (wakeLock != null)
                {
                    wakeLock.release();
                }
            }
        }
        // ConnectionCommand.makeGetStateIntent(getApplicationContext());
    }

    protected void notifyCallDisconnected(int callID)
    {
        getBroadcastHelper()
            .callState(callID, pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED.swigValue(), pjsip_status_code.PJSIP_SC_DECLINE.swigValue(), false,
                false, 0, false);
    }

    protected void notifyNullCall(int callID)
    {
        getBroadcastHelper()
            .callState(callID, pjsip_inv_state.PJSIP_INV_STATE_NULL.swigValue(), pjsip_status_code.PJSIP_SC_DECLINE.swigValue(), false,
                false, 0, false);
    }

    protected void notifyStackState(pjsua_state state)
    {
        getBroadcastHelper().sipStackState(state.swigValue(), mDEINIT_places);
    }

    private SipCall getCall(final int callId)
    {
        final SipAccount account = mLib.getSipAccount();

        if (account == null)
        {
            return null;
        }
        return account.getCall(callId);
    }

    private void regAccount(final Intent intent)
    {
        if (ConnectionService.isServiceON(getApplicationContext()))
        {
            final SipAccountData accountData = new SipAccountData(getApplicationContext());
            try
            {
                if (mLib.getSipAccount() == null)
                {
                    mLib.addAccount(accountData, SipService.this);
                    mLib.RegisterAccount();
                }
                else
                {
                 AccountInfo accountInfo = mLib.getSipAccount().getInfo();
                    if (accountInfo.getRegStatus().equals(pjsip_status_code.PJSIP_SC_OK) && accountInfo.getRegIsActive())
                    {
                        //zarejestrowany ok
                        mLib.ModifyAccount(accountData);
                    }
                    else
                    {
                        if (accountInfo.getRegIsActive())
                        {
                            mLib.unregisterAccounts();
                        }
                        mLib.deleteAccount();
                        mLib.addAccount(accountData, SipService.this);
                        mLib.RegisterAccount();
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(this, "", e);
                getBroadcastHelper().registrationState(500, false);
            }
        }
    }

    private DE_INIT_PLACES mDEINIT_places = DE_INIT_PLACES.OTHER;

    private void unRegAccount(final Intent intent)
    {
        if (mLib.getSipAccount() == null)
        {
            Log.d(this, "no active account");
            getRegState();
            return;
        }
        AccountInfo info = null;
        try
        {
            info = mLib.getSipAccount().getInfo();
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }

        if (info == null || info.getRegIsActive())
        {
            mLib.unregisterAccounts();
        }
        else
        {
            getRegState();
            return;
        }
        if (!WifiUtils.isInternetConnectionAvailable(getApplicationContext()))
        {
            getRegState();
        }
        if (intent != null)
        {
            mDEINIT_places = DE_INIT_PLACES.values()[intent.getIntExtra(SipServiceCommand.PARAM_ID, DE_INIT_PLACES.OTHER.ordinal())];
        }
        else
        {
            mDEINIT_places = DE_INIT_PLACES.OTHER;
        }
        //     mLib.DeInitialize(SipService.this);
    }

    private void deinitialize(final Intent intent)
    {
//        if (mLib.getSipAccount() == null)
//        {
//            Log.d(this, "no active account");
//        }
//        else
//        {
//            mLib.unregisterAccounts();
//        }

        if (intent != null)
        {
            mDEINIT_places = DE_INIT_PLACES.values()[intent.getIntExtra(SipServiceCommand.PARAM_ID, DE_INIT_PLACES.OTHER.ordinal())];
        }
        else
        {
            mDEINIT_places = DE_INIT_PLACES.OTHER;
        }
        mLib.DeInitialize(SipService.this);
    }


    public static synchronized void setNumberToCallNativeDuringNextReceive(final String numberToCallNativeDuringNextReceive)
    {
        sNumberToCallNativeDuringNextReceive = numberToCallNativeDuringNextReceive;
    }

    public static synchronized String getNumberToCallNativeDuringNextReceive()
    {
        return sNumberToCallNativeDuringNextReceive;
    }

    private void makeCall(final Intent intent)
    {
        final String number = intent.getStringExtra(SipServiceCommand.PARAM_NUMBER);


        final int notificationId = intent.getIntExtra(SipServiceCommand.PARAM_NOTIFICATION_ID, -1); //oddzwoń

        if (notificationId == NOTIFICATION_MISSED_CALL_ID)
        {
            new AsyncMarkAllCallsAsRead(getApplicationContext()).execute();
            CancelNotification(getApplicationContext(), notificationId);
        }

        if (number == null || number.isEmpty())
        {
            Log.e(this, "numer is null");
            return;
        }
        if (OLPPhoneNumberUtils.shouldCallEmergencyNativeFromApp(number)
                || OLPPhoneNumberUtils.isUSSDNumber(number)
                || !ConnectionService.isServiceON(getApplicationContext())
                || mLib.getSipAccount() == null)
        {
            Log.d(this, "call by native");
            setNumberToCallNativeDuringNextReceive(number);
            startActivity(IntentUtil.GetCallIntent(SipService.this, number));
            return;
        }
        Log.d(this, "try make call to: " + number);
        boolean isWrongCallState = intent.getBooleanExtra(SipServiceCommand.PARAM_WRONG_CALL_STATE, false);
        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager.getCallState() != TelephonyManager.CALL_STATE_IDLE && !isWrongCallState)
        {
            Toast.makeText(this, R.string.ongoing_call, Toast.LENGTH_SHORT).show();
            return;
        }

        setNumberToCallNativeDuringNextReceive(null);
        try
        {
            if (mLib.getSipAccount().getCallSize() == 0)
            {
                audioManagerHelper.initializeCall();
                int callId = -1;
                if (number.equals("112"))
                {
                    call112EmergencyNumber();
                }
                else
                {
                    String newNumber;
                    if (number.startsWith("+"))
                    {
                        newNumber = number.replaceAll(SipSettings.SIP_REGEX, "");
                        newNumber = "+" + newNumber;
                    } else
                    {
                        newNumber = number.replaceAll(SipSettings.SIP_REGEX, "");
                    }
                    Log.d(this, "try get make call to newNumber " + newNumber);
                    SipCall sipCall = mLib.getSipAccount().addOutgoingCall(newNumber);
                    callId = sipCall.getId();
                }
                showCallActivity(callId, number);
            }
            else
            {
                showCallActivity(mLib.getSipAccount().getActiveCallId(), mLib.getSipAccount().getActiveCallNumber());
            }
        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while making call", exc);
            showCallActivity(-1, number);
        }
    }


    private void call112EmergencyNumber()
    {
        //// TODO: 22.05.17 write handle call to 112
    }

    private void getCallState(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        Log.d(this, "try get call state for: " + callId);
        if (callId == -1)
        {
            notifyNullCall(callId);
        }

        final SipCall sipCall = getCall(callId);
        if (sipCall == null)
        {
            notifyNullCall(callId);
            return;
        }

        Log.d(this, "getCallState before send info: " + callId);

        getBroadcastHelper().callState(callId, sipCall.getCurrentState().swigValue(), sipCall.getLastStatus().swigValue(), sipCall.isLocalHold(),
            sipCall.isLocalMute(), sipCall.getCallDuration(), sipCall.isLocalSpeaker());

        Log.d(this, "getCallState after send info: " + callId);
    }

    public void getRegState()
    {
        if (!mLib.getIsInitialized() || mLib.getSipAccount() == null)
        {
            getBroadcastHelper().registrationState(400, false);
            return;
        }

        try
        {
            AccountInfo info = mLib.getSipAccount().getInfo();
            if(info!=null)
            {
                getBroadcastHelper().registrationState(info.getRegStatus().swigValue(), info.getRegIsActive());
                info.delete();
            }
        }
        catch (Exception e)
        {
            Log.d(this, "Error while getting reg state code", e);
        }
    }


    private void hungUpCall(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        try
        {
            final SipCall sipCall = getCall(callId);

            if (sipCall == null)
            {
                Log.d(this, "hangUpCall, call is null!");
                notifyNullCall(callId);
                return;
            }
            Log.d(this, "hungUpCall");
            sipCall.hangUp();

        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while hanging up call", exc);
            notifyNullCall(callId);
        }
    }

    private void answerCall(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        final String number = intent.getStringExtra(SipServiceCommand.PARAM_NUMBER);
        final int notificationId = intent.getIntExtra(SipServiceCommand.PARAM_NOTIFICATION_ID, -1); //oddzwoń
        if (notificationId != -1)
        {
            OLPNotificationBuilder.CancelNotification(SipService.this, notificationId);
        }

        try
        {
            final SipCall sipCall = getCall(callId);

            if (sipCall == null)
            {
                notifyNullCall(callId);
                return;
            }
            sipCall.answerCall();
            if (mLib.getSipAccount().getCallSize() == 0)
            {
                showCallActivity(sipCall.getId(), number);
            }
            else
            {
                showCallActivity(mLib.getSipAccount().getActiveCallId(), mLib.getSipAccount().getActiveCallNumber());
            }
        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while answer call", exc);
            notifyNullCall(callId);
        }
    }

    private void rejectCall(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        try
        {
            final SipCall sipCall = getCall(callId);
            if (sipCall == null)
            {
                notifyNullCall(callId);
                return;
            }
            sipCall.sendBusyHereToIncomingCall();
        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while answer call", exc);
            notifyNullCall(callId);
        }
    }

    private void muteCall(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);

        try
        {
            final SipCall sipCall = getCall(callId);

            if (sipCall == null)
            {
                notifyNullCall(callId);
                return;
            }

            sipCall.toggleMute();
            audioManagerHelper.setMicrophoneMute(sipCall.isLocalMute());
        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while mute  call", exc);
            notifyNullCall(callId);
        }

        getCallState(intent);
    }

    private void sendDTMF(final Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        final String digit = intent.getStringExtra(SipServiceCommand.PARAM_DTMF_DIGIT);

        Log.d(this, "sendDTMF " + digit);
        try
        {
            final SipCall sipCall = getCall(callId);

            if (sipCall == null)
            {
                return;
            }

            sipCall.dialDtmf(digit);

        }
        catch (final Exception exc)
        {
            Log.d(this, "Error while sendDTMF to call", exc);
        }
    }


    private void showCallActivity(final int callId, final String numberToCall)
    {
        if (!handleIncomingCall())
        {
            final Intent intent = new Intent(SipService.this, ActivityCall.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ActivityCall.param_CALL_ID, callId);
            intent.putExtra(ActivityCall.param_CALL_TYPE, CallLog.Calls.OUTGOING_TYPE);
            intent.putExtra(SipServiceCommand.PARAM_NUMBER, numberToCall);
            startActivity(intent);
        }

    }
    private boolean handleIncomingCall()
    {
        SipCall activeSipCall = mLib.getSipAccount().getActiveCall();
        if (activeSipCall != null && activeSipCall.getCallType() == CallLog.Calls.MISSED_TYPE)
        {
            showIncomingCall(activeSipCall.getId(), activeSipCall.getNumber());
            return true;
        }
        return false;
    }


    private void toggleSpeaker(Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        final SipCall sipCall = getCall(callId);

        if (sipCall == null)
        {
            notifyNullCall(callId);
            return;
        }

        boolean turnOn = sipCall.toggleSpeaker();
        audioManagerHelper.setSpeakerphoneOn(turnOn);
        getCallState(intent);
    }

    private void makeTone()
    {
        audioManagerHelper.makeVibrateTone(getApplicationContext());
    }

    private void setSpeaker(Intent intent)
    {
        final int callId = intent.getIntExtra(SipServiceCommand.PARAM_ID, -1);
        final SipCall sipCall = getCall(callId);
        final boolean turnOn = intent.getBooleanExtra(PARAM_TURN_ON, false);

        if (sipCall == null)
        {
            notifyNullCall(callId);
            return;
        }
        sipCall.setLocalSpeaker(turnOn);
        audioManagerHelper.setSpeakerphoneOn(turnOn);
        getCallState(intent);

    }

    public void initializeCall()
    {
        enqueueJob(() -> getAudioManagerHelper().initializeCall());
    }

    public void showIncomingCall(final int callId, final String number)
    {
        final Intent intent = new Intent(SipService.this, ActivityIncomingCall.class);
        if (App.isAnyActivityActive())
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        else
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        Bundle bundle = new Bundle();
        bundle.putInt(ActivityCall.param_CALL_ID, callId);
        bundle.putInt(ActivityCall.param_CALL_TYPE, CallLog.Calls.MISSED_TYPE);
        bundle.putString(SipServiceCommand.PARAM_NUMBER, number);
        intent.putExtras(bundle);
        startActivity(intent);

        OLPNotificationBuilder notificationBuilder = new OLPNotificationBuilder(this);
        notificationBuilder.PutIncomingCallNotification(callId, CallLog.Calls.MISSED_TYPE, number);

    }

    public static void unregisterSipService(Context context)
    {
        SipServiceCommand.unRegAccount(context, DE_INIT_PLACES.OTHER.ordinal());
    }

    public boolean isHomeScreenVisible(Context context)
    {

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo defaultLauncher = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        String defaultLauncherPackage = defaultLauncher.activityInfo.packageName;
//        Log.d(context, defaultLauncherPackage);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses)
        {
//            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
//                for (String activeProcess : processInfo.pkgList) {
//                    Log.d(context, activeProcess);
//                    if (activeProcess.equals(defaultLauncherPackage))
//                        Log.d(context, activeProcess);
            Log.d(context, "process: " + processInfo.pkgList[0]);
            return processInfo.pkgList[0].equalsIgnoreCase(defaultLauncherPackage);


        }
//            }
//        }


        return false;
    }

    public boolean isScreenLocked(Context context)
    {
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return myKM.inKeyguardRestrictedInputMode();

    }

    private void holdCalls(final Intent intent)
    {
        SipAccount activeAccount = mLib.getSipAccount();
        if (activeAccount == null)
        {
            return;
        }
        if (activeAccount.getCallSize() > 0)
        {
            for (int callId : activeAccount.getCallIDs())
            {
                try
                {
                    activeAccount.getCall(callId).setHold(new CallOpParam(true));
                }
                catch (Exception e)
                {
                    Log.e(this, "", e);
                }
            }
        }
    }

    private void unHoldCalls(final Intent intent)
    {
        SipAccount activeAccount = mLib.getSipAccount();
        if (activeAccount == null)
        {
            return;
        }
        if (activeAccount.getActiveCallId() > 0)
        {
            try
            {
                CallOpParam prm = new CallOpParam();
                CallSetting opt = prm.getOpt();
                opt.setAudioCount(1);
                opt.setVideoCount(0);
                opt.setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                prm.setOptions(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                activeAccount.getCall(activeAccount.getActiveCallId()).reinvite(prm);
            }
            catch (Exception e)
            {
                Log.e(this, "", e);
            }
        }
    }

    public enum DE_INIT_PLACES
    {
        CHARON_REVOKE,
        CHARON_DESTROY,
        CHARON_STOP,
        OTHER
    }

    public static boolean isSIPRegisterError(pjsip_status_code regStatus, boolean isRegistered)
    {
        return regStatus != null && ((regStatus == PJSIP_SC_OK && !isRegistered) ||
                                     (regStatus == pjsip_status_code.PJSIP_SC_UNAUTHORIZED
                                             || regStatus == pjsip_status_code.PJSIP_SC_FORBIDDEN
                                             || regStatus == PJSIP_SC_REQUEST_TIMEOUT
                                             || (regStatus == PJSIP_SC_NOT_FOUND)
                                             || regStatus == PJSIP_SC_BAD_REQUEST
                                             || regStatus == PJSIP_SC_INTERNAL_SERVER_ERROR
                                             || regStatus == PJSIP_SC_SERVICE_UNAVAILABLE));
    }

    public void stopRingtone()
    {
        audioManagerHelper.stopRingtone();
    }

    public void startRingtone()
    {
        //audioManagerHelper = new AudioManagerHelper(getApplicationContext());
        audioManagerHelper.startRingtone(getApplicationContext());
    }

    public void initializeCommunication()
    {
        audioManagerHelper.initliazeComunication();
    }

    private void setBluetoothHeadset(Intent intent)
    {
        if (intent.getBooleanExtra(PARAM_TURN_ON, false))
        {
            final SipAccount account = mLib.getSipAccount();

            if (account == null)
            {
                return;
            }
            if (account.getCallSize() > 0)
            {
               audioManagerHelper.initializeBluetoothHeadset();
            }
        }
        else
        {
            audioManagerHelper.stopBluetoothHeadset();
        }
    }


}
