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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import pl.orangelabs.log.Log;

/**
 * Created by marcin on 21.12.16.
 */

public class AudioManagerHelper
{
    private AudioManager audioManager;
    private Ringtone mRingTone;
    private Vibrator mVibrator;
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;
    private static final long[]
        VIBRATOR_PATTERN_RING =
        {0,
            1000,
            1000};
    private static final long[]
        VIBRATOR_PATTERN_TONE =
        {0,
            200,
            400,
            200,
            400,
            200};

    public AudioManagerHelper(Context context)
    {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void initializeCall()
    {
        initializeBluetoothHeadset();
        requestAudioFocus();
    }

    private Runnable runnable;
    private int times = 20;
    private Handler handler;

    public void initializeBluetoothHeadset()
    {
        initializeBluetoothHeadsetHandler();
    }

    /**
     * hotfix for Android's wrong behaviour, We got ACTION_ACL_CONNECTED but headset isn't initialized
     * after a while it starts working
     */
    private void initializeBluetoothHeadsetHandler()
    {
        runnable = () ->
        {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            int headsetConnectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
            if (headsetConnectionState != BluetoothAdapter.STATE_DISCONNECTED)
            {
                Log.d(this, "startBluetoothSco");
                audioManager.startBluetoothSco();
            }
            else
            {
                if (times != 0)
                {
                    times--;
                    handler.postDelayed(runnable, 500);
                }
            }
        };
        times = 30;
        if (handler != null)
        {
            handler.removeCallbacks(null);
        }
        handler = new Handler();
        handler.post(runnable);
    }

    public void stopBluetoothHeadset()
    {
        if (handler != null)
        {
            handler.removeCallbacks(runnable);
        }
        audioManager.stopBluetoothSco();
    }

    public void restAudioManager()
    {
        stopBluetoothHeadset();
        audioManager.setSpeakerphoneOn(false);
        audioManager.setMicrophoneMute(false);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.abandonAudioFocus(onAudioFocusChangeListener);
        Log.d(this, "restAudioManager");
    }

    public synchronized void startRingtone(Context context)
    {
        Log.d(this, "startRingtone");
        makeVibrate(context, VIBRATOR_PATTERN_RING, false);
        try
        {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mRingTone = RingtoneManager.getRingtone(context, notification);
            try
            {
                setLoopingForRingtone();
            }
            catch (Exception e)
            {
                Log.d(this, "Error while trying to setLoopingForRingtone!", e);
            }
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            mRingTone.play();
        }
        catch (Exception exc)
        {
            Log.d(this, "Error while trying to play ringtone!", exc);
        }
    }

    private void setLoopingForRingtone() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method method = mRingTone.getClass().getMethod("setLooping", boolean.class);
        method.setAccessible(true);
        method.invoke(mRingTone, true);
    }

    public void makeVibrateTone(Context context)
    {
        makeVibrate(context, VIBRATOR_PATTERN_TONE, true);
    }

    public void makeVibrate(Context context, long[]
        VIBRATOR_PATTERN, boolean alwaysVibrate)
    {
        final String setName;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
        {
            setName = android.provider.Settings.System.VIBRATE_WHEN_RINGING;
        }
        else
        {
            setName = "vibrate_when_ringing";
        }
        boolean
            vibrateOnRinging = android.provider.Settings.System.getInt(context.getContentResolver(), setName, 0) == 1;
        if (vibrateOnRinging || alwaysVibrate)
        {
            mVibrator.vibrate(VIBRATOR_PATTERN, -1);
        }
    }

    public synchronized void stopRingtone()
    {
        Log.d(this, "stopRingtone");
        mVibrator.cancel();

        if (mRingTone != null)
        {
            try
            {
                if (mRingTone.isPlaying())
                {
                    mRingTone.stop();
                }
            }
            catch (Exception ignored)
            {
                Log.d(this, ignored.toString());
            }
        }
    }

    public void initliazeComunication()
    {
        Log.d(this, "initliazeComunication");
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    private void requestAudioFocus()
    {
        Log.d(this, "requestAudioFocus");
        onAudioFocusChangeListener = focusChange ->
        {

        };
        audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    public void setMicrophoneMute(boolean value)
    {
        Log.d(this, "setMicrophoneMute" + value);
        audioManager.setMicrophoneMute(value);
    }

    public void setSpeakerphoneOn(boolean value)
    {
        Log.d(this, "setSpeakerphoneOn" + value);
        audioManager.setSpeakerphoneOn(value);
    }

    public void muteRing()
    {
        stopRingtone();
    }
}
