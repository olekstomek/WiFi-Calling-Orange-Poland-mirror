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

import android.animation.Animator;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.Date;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.ctrl.OLPKeyboard;
import pl.orangelabs.wificalling.sip.SipMessagesReceiver;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.OLPNotificationBuilder;
import pl.orangelabs.wificalling.util.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * @author Cookie
 */
public class ActivityCall extends ActivityBaseCall
{

    private TextView viewCallState;

    private ImageView btnMute, btnSpeaker, btnKeyboard, btnEndCall;
    private boolean speakerIsActive = false, keyboardIsActive = false;
    private FrameLayout keyboardLayout;
    private LinearLayout buttonContainer;
    private RelativeLayout relativeLayoutViewCallerDataContainer;
    private OLPKeyboard olpKeyboardWhite;
    private HeadsetStateReceiver headsetStateReceiver;
    //    private BluetoothReceiver bluetoothReciever;
//    private boolean isHeadsetPlugged;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Transition makeSharedElementEnterTransition(Context context)
    {
        TransitionSet set = new TransitionSet();
        set.setOrdering(TransitionSet.ORDERING_TOGETHER);

        Transition changeBounds = new AutoTransition();
        changeBounds.addTarget(R.id.view_call_photo);
        changeBounds.addTarget(R.id.view_call_name);
        changeBounds.addTarget(R.id.view_call_number);
        changeBounds.addTarget(R.id.view_call_country);
        changeBounds.addTarget(R.id.view_call_hung_up);
        changeBounds.setInterpolator(new AccelerateDecelerateInterpolator());

        set.addTransition(changeBounds);

        return set;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Transition makeEnterTransition()
    {

        return new Fade().setDuration(0);
    }

    long startTime = 0;

    @Override
    void onInternalCreate(@Nullable final Bundle savedInstanceState)
    {
        setLayout(R.layout.activity_call);
        headsetStateReceiver = new HeadsetStateReceiver();
        System.currentTimeMillis();
        assignViews();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getWindow().setEnterTransition(makeEnterTransition());
            getWindow().setExitTransition(makeEnterTransition());
            getWindow().setSharedElementEnterTransition(makeSharedElementEnterTransition(this));
        }
        startTime = new Date().getTime();
        //Todo refactor it is should be in Service
        OLPNotificationBuilder olpNotificationBuilder = new OLPNotificationBuilder(this);
        olpNotificationBuilder
            .PutOngoingCallNotification(mCallId, mCallType, contactCall != null ? contactCall.mDisplayName : mPhoneNumber, startTime, false, false);
    }

    private boolean isMute = false;

    private void updateMuteBtnStatus(boolean isMute)
    {
        this.isMute = isMute;
        if (isMute)
        {
            btnMute.setImageResource(R.drawable.ic_mute2);
        }
        else
        {
            btnMute.setImageResource(R.drawable.ic_mute);
        }
    }

    private boolean isSpeakerOn = false;

    private void updateSpeakerBtnStatus(boolean isSpeakerOn)
    {
        this.isSpeakerOn = isSpeakerOn;
        if (isSpeakerOn)
        {
            btnSpeaker.setImageResource(R.drawable.ic_speak2);
        }
        else
        {
            btnSpeaker.setImageResource(R.drawable.ic_speak);
        }
    }

    private void setViewOnClick()
    {
        btnMute.setOnClickListener(view ->
        {
            SipServiceCommand.muteCall(ActivityCall.this, mCallId);
        });

        btnSpeaker.setOnClickListener(view ->
            {
                SipServiceCommand.toggleSpeaker(ActivityCall.this, mCallId);
            }

        );


        btnKeyboard.setOnClickListener(view ->
        {
            if (keyboardIsActive)
            {
                hideKeyboard();
            }
            else
            {
                showKeyboard();
            }

        });
    }

    private void assignViews()
    {
        viewCallState = (TextView) findViewById(R.id.view_call_country);

        btnMute = (ImageView) findViewById(R.id.view_call_button_mute);
        btnSpeaker = (ImageView) findViewById(R.id.view_call_button_speak);
        btnKeyboard = (ImageView) findViewById(R.id.view_call_button_key);
        btnEndCall = (ImageView) findViewById(R.id.view_call_hung_up);

        buttonContainer = (LinearLayout) findViewById(R.id.view_call_controls_container);

        keyboardLayout = (FrameLayout) findViewById(R.id.view_call_keyboard_container);
        olpKeyboardWhite = (OLPKeyboard) findViewById(R.id.call_keyboard);
        relativeLayoutViewCallerDataContainer = (RelativeLayout) findViewById(R.id.view_caller_data_container);

        //To animate revealing of the keyboard smoothly in initialization it is visible but with alpha 0.0f
        keyboardLayout.setAlpha(0.0f);

        buttonContainer.setAlpha(0.0f);
        buttonContainer.animate().alpha(1.0f).setDuration(1500).start();

        setViewOnClick();


        olpKeyboardWhite.addCallback(new OLPKeyboard.IOnKeyboardCallback()
        {
            @Override
            public void onTextChanged(String text)
            {
                if (text.equals(""))
                {
                    olpKeyboardWhite.setNumberFieldVisible(false);
                }
                else
                {
                    SipServiceCommand.sendDtmf(ActivityCall.this, mCallId, text.substring(text.length() - 1));
                    olpKeyboardWhite.setNumberFieldVisible(true);
                }
            }

            @Override
            public void onCallPressed(String text)
            {

            }

            @Override
            public void onCloseRequested()
            {

            }
        });
        btnEndCall.setOnClickListener(view ->
        {
            endCall();
        });
        handleHardwareKeyboard();

    }

    private void handleHardwareKeyboard()
    {
        if (Utils.hasHardwareKeyboard(getResources()))
        {
            btnKeyboard.setVisibility(View.INVISIBLE);
            showKeyboard();
        }
    }

    private void showKeyboard()
    {
        keyboardLayout.bringToFront();
        keyboardLayout.invalidate();
        keyboardLayout.setVisibility(VISIBLE);
        keyboardLayout.animate()
            .alpha(1.0f)
            .setDuration(500)
            .setListener(null)
            .start();
        btnKeyboard.setImageResource(R.drawable.ic_key2);
        keyboardIsActive = true;

        animateContactDetail();
    }

    private void animateContactDetail()
    {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) (size.x / -3.09);
        int widthMargin = (int) (size.x / 10.8);
        int heightText = (int) (size.y / -2.20);
        int height = (int) (size.y / -3.84);
        viewCallerPhoto.animate().translationX(width).translationY(height).scaleY((float) 0.5).scaleX((float) 0.5).start();
        relativeLayoutViewCallerDataContainer.animate().translationY(heightText).translationX(widthMargin);
        setLayoutParamsCenterHorizontal(false, viewCallerName);
        setLayoutParamsCenterHorizontal(false, viewTimeElapsed);
        setLayoutParamsCenterHorizontal(false, viewCallsContainer);
    }

    private void hideKeyboard()
    {
        keyboardLayout.animate()
            .alpha(0.0f)
            .setDuration(500)
            .setListener(new Animator.AnimatorListener()
            {
                @Override
                public void onAnimationStart(Animator animator)
                {

                }

                @Override
                public void onAnimationEnd(Animator animator)
                {
                    keyboardLayout.setVisibility(GONE);

                }

                @Override
                public void onAnimationCancel(Animator animator)
                {

                }

                @Override
                public void onAnimationRepeat(Animator animator)
                {

                }
            })
            .start();
        viewCallerPhoto.animate().translationX(0).translationY(0).scaleY((float) 1).scaleX((float) 1).start();
        relativeLayoutViewCallerDataContainer.animate().translationY(0).translationX(0);
        btnKeyboard.setImageResource(R.drawable.ic_key);
        keyboardIsActive = false;
        setLayoutParamsCenterHorizontal(true, viewCallerName);
        setLayoutParamsCenterHorizontal(true, viewCallsContainer);
        setLayoutParamsCenterHorizontal(true, viewTimeElapsed);
    }

    private void setLayoutParamsCenterHorizontal(boolean centerHorizontal, View view)
    {
        RelativeLayout.LayoutParams layoutParams =
            (RelativeLayout.LayoutParams) view.getLayoutParams();
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, centerHorizontal ? RelativeLayout.TRUE : 0);
        view.setLayoutParams(layoutParams);
    }

    @Override
    protected void onResume()
    {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetStateReceiver, filter);


        filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
//        this.registerReceiver(mReceiver, filter);
        super.onResume();
    }

    public void endCall()
    {
        Log.d(this, "btnEndCall clicked! call ID: " + mCallId);
        SipServiceCommand.hangUpCall(ActivityCall.this, mCallId);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_ENDCALL)
        {
            endCall();
            return true;
        }
        else
        {
            return super.onKeyDown(keyCode, event);
        }
    }
    //    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//            String bluetooth = "BluetoothReceiver";
//
//
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                Log.d(bluetooth, "Device found"); //Device found
//            }
//            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//                Log.d(bluetooth, "ACL connected "); //Device is now connected
//            }
//            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                Log.d(bluetooth, "Discovery finished"); //Done searching
//            }
//            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//                Log.d(bluetooth, "disconnect requested"); //Device is about to disconnect
//            }
//            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//                Log.d(bluetooth, "acl disconnected"); //Device has disconnected
//            }
//        }
//    };

    @Override
    protected void onPause()
    {
        unregisterReceiver(headsetStateReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected SipMessagesReceiver getMessageReceiver()
    {
        return new SipMessageReceiverReceiver();
    }

    private class SipMessageReceiverReceiver extends ActivityBaseCall.SipMessageReceiverReceiver
    {
        @Override
        protected void OnCallState(final int callId, final pjsip_inv_state state, final pjsip_status_code status, final boolean isLocalHold,
                                   final boolean isLocalMute, final long callDuration, boolean isSpeakerOn)
        {
            if (mCallId != callId)
            {
                return;
            }
            updateMuteBtnStatus(isLocalMute);
            updateSpeakerBtnStatus(isSpeakerOn);
            //Todo refactor it is should be in Service
            OLPNotificationBuilder olpNotificationBuilder = new OLPNotificationBuilder(getApplicationContext());

            olpNotificationBuilder
                .PutOngoingCallNotification(mCallId, mCallType, contactCall != null ? contactCall.mDisplayName : mPhoneNumber, startTime, isSpeakerOn,
                    isMute);

            viewCallState.setText(GetStringCallState(state));

            if (state.equals(pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED))

            {
                viewTimeElapsed.stop();
            }
            if (state.equals(pjsip_inv_state.PJSIP_INV_STATE_CONNECTING))
            {
                viewTimeElapsed.setAlpha(0.0f);
                viewTimeElapsed.setVisibility(VISIBLE);
                viewTimeElapsed.animate().alpha(1.0f).setDuration(1500).start();

            }
            if (state.equals(pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED))
            {
                viewTimeElapsed.setVisibility(VISIBLE);
                viewTimeElapsed.start(callDuration);
            }

            super.OnCallState(callId, state, status, isLocalHold, isLocalMute, callDuration, isSpeakerOn);
        }
    }


    private class HeadsetStateReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG))
            {

                int state = intent.getIntExtra("state", -1);
                if (state == 1)
                {
                    Log.d(this, "headphones connected!");
                    SipServiceCommand.setSpeaker(ActivityCall.this, mCallId, false);
                }
            }

        }
    }

    private String GetStringCallState(pjsip_inv_state state)
    {
        if (state == pjsip_inv_state.PJSIP_INV_STATE_NULL)
        {
            return getString(R.string.call_state_early);
        }
        if (state == pjsip_inv_state.PJSIP_INV_STATE_INCOMING)
        {
            return getString(R.string.call_state_incoming);
        }
        else if (state == pjsip_inv_state.PJSIP_INV_STATE_EARLY)
        {
            return getString(R.string.call_state_early);
        }
        else if (state == pjsip_inv_state.PJSIP_INV_STATE_CALLING)
        {
            return getString(R.string.call_state_early);
        }
        else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONNECTING)
        {
            return getString(R.string.call_state_connecting);
        }
        else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)
        {
            return getString(R.string.call_state_connecting);
        }
        else if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
        {
            return getString(R.string.call_state_disconnect);
        }
        else
        {
            return state.toString();
        }
    }

}





