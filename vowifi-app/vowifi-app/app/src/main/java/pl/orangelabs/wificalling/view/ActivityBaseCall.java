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

package pl.orangelabs.wificalling.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.ArrayList;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.ctrl.OVChronometer;
import pl.orangelabs.wificalling.model.ContactCall;
import pl.orangelabs.wificalling.service.connection_service.ConnectionError;
import pl.orangelabs.wificalling.service.connection_service.ConnectionState;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStateMessenger;
import pl.orangelabs.wificalling.service.connection_service.ConnectionStateReceiver;
import pl.orangelabs.wificalling.sip.BroadcastSipReceiver;
import pl.orangelabs.wificalling.sip.SipMessagesReceiver;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.ContactThumbnailTask;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.util.flavoured.DefaultCallerPhoto;

import static android.view.View.GONE;

/**
 * @author Cookie
 */
public abstract class ActivityBaseCall extends ActivityBase
{
    protected ConnectionStateReceiver connectionStateReceiver;
    public static final String param_CALL_ID = "param_callId";
    public static final String param_CALL_TYPE = "param_callType";

    private static final long DELAY_MILLIS = 0;
    protected int mCallType = -1;
    protected int mCallId = -1;
    protected String mPhoneNumber;
    TextView viewCallerName;
    TextView viewCallerNumber;
    View viewCallsContainer;
    protected ContactCall contactCall = null;
    //ImageView imageView;
    ImageView viewCallerPhoto;
    protected OVChronometer viewTimeElapsed;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private PowerManager.WakeLock mWakeLock;
    private AlertDialog weakConnectionDialog = null;
    private SensorEventListener mSensorEventListener = new SensorEventListener()
    {
        @Override
        public void onSensorChanged(final SensorEvent event)
        {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY)
            {
                if (event.values[0] > 0)
                {
                    if (mWakeLock.isHeld())
                    {
                        mWakeLock.release();
                    }
                }
                else
                {
                    if (!mWakeLock.isHeld())
                    {
                        mWakeLock.acquire();
                    }
                }
            }

        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy)
        {

        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                             | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.activity_call_base);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        int field = 0x00000020;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            field = PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK;
        }
        else
        {
            try
            {
                field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
            }
            catch (Throwable ignored)
            {
                Log.d(this, ignored.toString());
            }
        }

        final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(field, getClass().getName());

        Bundle bundle = getIntent().getExtras();
        if (bundle != null)
        {
            mCallType = bundle.getInt(param_CALL_TYPE, -1);
            mCallId = bundle.getInt(param_CALL_ID, -1);
            mPhoneNumber = bundle.getString(SipServiceCommand.PARAM_NUMBER);
        }

        mBroadcastSipReceiver = new BroadcastSipReceiver(getMessageReceiver());
        mBroadcastSipReceiver.RegisterSipReceiver(this);
        connectionStateReceiver = new ConnectionStateReceiver(new ConnectionServiceReceiver());


        onInternalCreate(savedInstanceState);

        if (mPhoneNumber != null)
        {
            Log.d(this, "fillContactData for number: " + mPhoneNumber);
        }
        fillContactData(ContactCall.loadDetails(getApplicationContext(), mPhoneNumber));
    }

    protected SipMessagesReceiver getMessageReceiver()
    {
        return new SipMessageReceiverReceiver();
    }

    private void setViews()
    {
        viewCallsContainer = findViewById(R.id.view_call_number_container);
        viewCallerName = (TextView) findViewById(R.id.view_call_name);
        viewCallerNumber = (TextView) findViewById(R.id.view_call_number);
        viewCallerPhoto = (ImageView) findViewById(R.id.view_call_photo);
        viewTimeElapsed = (OVChronometer) findViewById(R.id.view_call_time_elapsed);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        Log.d(this, "displayMetrics" + displayMetrics.widthPixels);
        Log.d(this, "viewCallerPhoto" + viewCallerPhoto.getLayoutParams().width);

        viewCallerName.setMaxWidth(displayMetrics.widthPixels - viewCallerPhoto.getLayoutParams().width);

        //imageView = (ImageView) findViewById(R.id.call_background);
    }


    protected void fillContactData(final ContactCall contactCall)
    {
        if (contactCall != null)
        {
            this.contactCall = contactCall;
            viewCallerName.setText(contactCall.mDisplayName);
            if (contactCall.mThumbUri != null)
            {
                AsyncHelper.execute(new ContactThumbnailTask(viewCallerPhoto, Uri.parse(contactCall.mThumbUri), this, R.drawable.ic_avatar_full));
            }
            else
            {
                Drawable image = DefaultCallerPhoto.getDefaultPhoto(this);
                viewCallerPhoto.setImageDrawable(image);
                viewCallerPhoto.invalidate();

                Log.d(this, "set contact default image");
            }
            if (TextUtils.isEmpty(contactCall.mPhoneNumber))
            {
                viewCallerNumber.setVisibility(GONE);
            }
            else
            {
                viewCallerNumber.setText(contactCall.mPhoneNumber);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(this, "onResume");
        SipServiceCommand.getCallState(ActivityBaseCall.this, mCallId);
        if (mProximitySensor != null)
        {
            mSensorManager.registerListener(mSensorEventListener, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        connectionStateReceiver.registerConnectionStateReceiver(getApplicationContext());
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mSensorManager.unregisterListener(mSensorEventListener);
        connectionStateReceiver.unRegisterConnectionStateReceiver(getApplicationContext());
    }

    @Override
    protected void onDestroy()
    {
        if (mWakeLock.isHeld())
        {
            mWakeLock.release();
        }
        mBroadcastSipReceiver.UnRegisterSipReceiver(this);
        hideWeakConnectionDialog();
        super.onDestroy();
    }

    void setLayout(final int layoutId)
    {
        final ViewStub stub = (ViewStub) findViewById(R.id.call_layout_wrapper);
        stub.setLayoutResource(layoutId);
        stub.inflate();
        setViews();
    }

    abstract void onInternalCreate(@Nullable final Bundle savedInstanceState);

    void StartFinishProcedure()
    {
        Log.d(this, "StartFinishProcedure");
        Handler handler = new Handler();
        handler.postDelayed(this::finish, DELAY_MILLIS);
    }

    protected void OpenActivityCall()
    {
        Intent intent = new Intent(new Intent(ActivityBaseCall.this, ActivityCall.class));

        intent.putExtra(ActivityCall.param_CALL_ID, mCallId);
        intent.putExtra(param_CALL_TYPE, CallLog.Calls.INCOMING_TYPE);
        intent.putExtra(SipServiceCommand.PARAM_NUMBER, mPhoneNumber);


        ArrayList<Pair<View, String>> newList = new ArrayList<>();
        if (viewCallerPhoto.getVisibility() != GONE)
        {
            newList.add(Pair.create(viewCallerPhoto, ActivityBaseCall.this.getString(R.string.transition_call_photo)));
        }
        if (viewCallerName.getVisibility() != GONE)
        {
            newList.add(Pair.create(viewCallerName, ActivityBaseCall.this.getString(R.string.transition_call_name)));
        }
        if (viewCallerNumber.getVisibility() != GONE)
        {
            newList.add(Pair.create(viewCallerNumber, ActivityBaseCall.this.getString(R.string.transition_call_number)));
        }
        //newlist.add(Pair.create(imageView, "call_background"));
        ActivityOptionsCompat
            options =
            ActivityOptionsCompat.makeSceneTransitionAnimation(ActivityBaseCall.this, newList.toArray(new Pair[newList.size()]));
        shouldFinish = true;
        // The screen occurs only if the call is incoming, so i am adding a flag to intent for callog
        startActivity(intent, options.toBundle());
    }

    private boolean shouldFinish = false;

    @Override
    protected void onStop()
    {
        super.onStop();
        if (shouldFinish)
        {
            finish();
        }
    }

    protected class SipMessageReceiverReceiver extends SipMessagesReceiver
    {
        @Override
        protected void OnCallState(final int callId, final pjsip_inv_state state, final pjsip_status_code status, final boolean isLocalHold,
                                   final boolean isLocalMute, final long callDuration, final boolean isSpeakerOn)
        {
            if (mCallId != callId)
            {
                return;
            }

            if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
            {
                Log.d(this, "state dc, status " + status);
                handleDisconnectState(callDuration);
            }

            if (state == pjsip_inv_state.PJSIP_INV_STATE_NULL)
            {
                Log.d(this, "state null, status " + status);
                handleNullState();
            }
        }

        private void handleDisconnectState(long callDuration)
        {
            StartFinishProcedure();
        }

        private void handleNullState()
        {
            StartFinishProcedure();
        }
    }

    private void showWeakConnectionDialog()
    {
        if (weakConnectionDialog == null)
        {
            weakConnectionDialog = Utils.showDialogWithText(R.string.dialog_weak_signal, ActivityBaseCall.this);
        }
        else
        {
            if (weakConnectionDialog.isShowing())
            {
                return;
            }
        }
        weakConnectionDialog.show();
    }

    private void hideWeakConnectionDialog()
    {
        if (weakConnectionDialog != null)
        {
            weakConnectionDialog.dismiss();
        }
    }

    protected class ConnectionServiceReceiver extends ConnectionStateMessenger
    {
        @Override
        public void stateChange(ConnectionState connectionState)
        {
            super.stateChange(connectionState);
            if (connectionState.getConnectionError() == ConnectionError.WEAK_WIFI)
            {
                showWeakConnectionDialog();
            }
            else
            {
                hideWeakConnectionDialog();
            }
        }
    }
}
