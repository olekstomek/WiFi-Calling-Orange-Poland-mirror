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

import android.provider.CallLog;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.SipHeaderVector;
import org.pjsip.pjsua2.ToneDesc;
import org.pjsip.pjsua2.ToneDescVector;
import org.pjsip.pjsua2.ToneGenerator;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_role_e;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_media_status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.util.CallLogUtils;

/**
 * Created by Cookie on 2016-08-16.
 */
public class SipCall extends Call
{
    private SipAccount mAccount;

    private Pattern EARLY_MEDIA_PATTERN = Pattern.compile("P-Early-Media:.*".toLowerCase());
    private static final String EARLY_MEDIA_INACTIVE = "inactive";
    private static final String EARLY_MEDIA_RECEIVE_ONLY = "recevonly";

    private EarlyMedia mEarlyMedia = EarlyMedia.NONE;
    private ToneGenerator toneGenerator;

    private int mCallType = -1;
    private String mNumber = null;

    private boolean localHold = false;
    private boolean localMute = false;

    boolean isLocalSpeaker()
    {
        return localSpeaker;
    }

    void setLocalSpeaker(boolean localSpeaker)
    {
        this.localSpeaker = localSpeaker;
    }

    private boolean localSpeaker = false;

    SipCall(final SipAccount acc)
    {
        this(acc, -1);
    }

    boolean isLocalHold()
    {
        return localHold;
    }

    boolean isLocalMute()
    {
        return localMute;
    }


    SipCall(final SipAccount sipAccount, final int callId)
    {
        super(sipAccount, callId);
        mAccount = sipAccount;
    }

    @Override
    public void makeCall(final String dst_uri, final CallOpParam prm) throws Exception
    {
        final SipHeaderVector headerVector = prm.getTxOption().getHeaders();
        headerVector.add(SipSettings.VOWIFI_P_HEADER);
        headerVector.add(SipSettings.VOWIFI_INVITE);
        headerVector.add(SipSettings.VOWIFI_INVITE_ERLY_MEDIA);
        prm.getTxOption().setHeaders(headerVector);
        mCallType = CallLog.Calls.OUTGOING_TYPE;
        super.makeCall(dst_uri, prm);
        mAccount.getSipService().initializeCommunication();
    }

    void answerCall() throws Exception
    {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
        answer(param);
        mCallType = CallLog.Calls.INCOMING_TYPE;
        mAccount.getSipService().stopRingtone();
    }

    void sendBusyHereToIncomingCall()
    {
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);

        try
        {
            hangup(param);
        }
        catch (Exception exc)
        {
            Log.d(this, "Failed to send busy here", exc);
        }
    }

    void hangUp() throws Exception
    {
        Log.d(this, "hangUp");
        CallOpParam param = new CallOpParam();
        param.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
        hangup(param);
    }

    boolean toggleMute()
    {
        if (localMute)
        {
            setMute(false);
            return !localHold;
        }

        setMute(true);
        return localHold;
    }

    boolean toggleSpeaker()
    {
        setLocalSpeaker(!localSpeaker);
        return localSpeaker;

    }

    private void setMute(boolean mute)
    {
        // return immediately if we are not changing the current state
        if ((localMute && mute) || (!localMute && !mute))
        {
            return;
        }

        CallInfo info;
        try
        {
            info = getInfo();
        }
        catch (Exception exc)
        {
            Log.e(this, "setMute: error while getting call info", exc);
            return;
        }

        for (int i = 0; i < info.getMedia().size(); i++)
        {
            Media media = getMedia(i);
            CallMediaInfo mediaInfo = info.getMedia().get(i);

            if (mediaInfo.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO
                && media != null
                && mediaInfo.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE)
            {
                AudioMedia audioMedia = AudioMedia.typecastFromMedia(media);

                // connect or disconnect the captured audio
                try
                {
                    AudDevManager mgr = SipLib.ep.audDevManager();

                    if (mute)
                    {
                        mgr.getCaptureDevMedia().stopTransmit(audioMedia);
                        localMute = true;
                    }
                    else
                    {
                        mgr.getCaptureDevMedia().startTransmit(audioMedia);
                        localMute = false;
                    }

                }
                catch (Exception exc)
                {
                    Log.e(this, "setMute: error while connecting audio media to sound device", exc);
                }
            }
        }
    }

    pjsip_inv_state getCurrentState()
    {
        try
        {
            CallInfo info = getInfo();
            return info.getState();
        }
        catch (Exception exc)
        {
            Log.d(this, "Error while getting call Info", exc);
            return pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED;
        }
    }

    pjsip_status_code getLastStatus()
    {
        try
        {
            CallInfo info = getInfo();
            return info.getLastStatusCode();
        }
        catch (Exception exc)
        {
            Log.d(this, "Error while getting call Info", exc);
            return pjsip_status_code.PJSIP_SC_DECLINE;
        }
    }

    long getCallDuration()
    {
        try
        {
            SipCallInfo info = new SipCallInfo(getInfo());
            return info.getDuration();
        }
        catch (Exception exc)
        {
            Log.d(this, "Error while getting call Info", exc);
            return 0;
        }
    }

    @Override
    public void onCallState(final OnCallStateParam prm)
    {
        super.onCallState(prm);
        try
        {
            SipCallInfo info = new SipCallInfo(getInfo());
            int callID = info.getId();
            pjsip_inv_state callState = info.getState();
            pjsip_status_code callStatus = info.getLastStatusCode();

            mAccount.getSipService().getBroadcastHelper()
                .callState(callID, callState.swigValue(), callStatus.swigValue(), localHold, localMute, info.getDuration(), localSpeaker);

            if (callState == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
            {
                Log.d(this, "dump" + dump(true, ""));
                stopEarlyMedia();
                mAccount.getSipService().stopRingtone();
                mAccount.getSipService().getAudioManagerHelper().restAudioManager();
                CallLogUtils.registerCallToCallLog(mNumber, info.getDuration(),  mCallType, mAccount.getSipService().getApplicationContext());
                mAccount.getSipService().updateNotification();
                mAccount.removeCall(callID);

                //   if (!SettingsApp.isProd)
                //     {
                //    this.delete();
                //    }
            }
            else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONNECTING)
            {
                stopEarlyMedia();
            }
            else if (callState == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED)
            {
                stopEarlyMedia();
                mAccount.getSipService().stopRingtone();
                mAccount.getSipService().initializeCommunication();
            }

            else if (callState == pjsip_inv_state.PJSIP_INV_STATE_EARLY)
            {
                if (info.getCallInfo().getRole() == pjsip_role_e.PJSIP_ROLE_UAC
                    && (info.getCallInfo().getLastStatusCode() == pjsip_status_code.PJSIP_SC_RINGING
                        || info.getCallInfo().getLastStatusCode() == pjsip_status_code.PJSIP_SC_PROGRESS))
                {
                    String message = prm.getE().getBody().getTsxState().getSrc().getRdata().getWholeMsg();
                    MangeEarlyMedia(message);

                }
            }
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }
    }

    private void MangeEarlyMedia(final String sipMessage)
    {
        mEarlyMedia = EarlyMedia.NONE;
        if (sipMessage != null && !sipMessage.isEmpty())
        {
            Matcher matcher = EARLY_MEDIA_PATTERN.matcher(sipMessage.toLowerCase());
            if (matcher.find())
            {
                Log.d(this, "find" + matcher.group());
                if (matcher.group().contains(EARLY_MEDIA_INACTIVE))
                {
                    startEarlyMediaToRemote(EarlyMedia.LOCAL);
                }
                else if (matcher.group().contains(EARLY_MEDIA_RECEIVE_ONLY))
                {
                    startEarlyMediaToRemote(EarlyMedia.REMOTE);
                }
            }
            else
            {
                startEarlyMediaToRemote(EarlyMedia.LOCAL);
            }
        }
    }

    private void startEarlyMediaToRemote(EarlyMedia earlyMedia)
    {
        if (earlyMedia == EarlyMedia.NONE)
        {
            return;
        }

        stopEarlyMedia();

        mEarlyMedia = earlyMedia;
        startRingBackTone();
        if (earlyMedia == EarlyMedia.LOCAL)
        {
            try
            {
                if (toneGenerator != null)
                {
                    toneGenerator.startTransmit(SipLib.ep.audDevManager().getPlaybackDevMedia());
                }
            }
            catch (Exception e)
            {
                Log.e(this, "error on startEarly to " + earlyMedia, e);
            }
        }
        else if (earlyMedia == EarlyMedia.REMOTE)
        {
            try
            {
                if (toneGenerator != null)
                {
                    AudioMedia am = getActiveAudioMedia();
                    if (am != null)
                    {
                        toneGenerator.startTransmit(am);
                    }
                }
            }
            catch (Exception e)
            {
                Log.e(this, "error on startEarly to " + earlyMedia, e);
            }
        }
    }

    private AudioMedia getActiveAudioMedia() throws Exception
    {
        CallInfo ci;
        ci = getInfo();

        final CallMediaInfoVector cmiv = ci.getMedia();
        for (int i = 0; i < cmiv.size(); i++)
        {
            final CallMediaInfo cmi = cmiv.get(i);
            if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                (cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE ||
                 cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD))
            {
                final Media m = getMedia(i);
                return AudioMedia.typecastFromMedia(m);
            }
        }
        return null;
    }


    private void stopEarlyMedia()
    {
        try
        {
            if (toneGenerator != null)
            {
                if (mEarlyMedia == EarlyMedia.LOCAL)
                {
                    toneGenerator.stopTransmit(SipLib.ep.audDevManager().getPlaybackDevMedia());
                }
                else if (mEarlyMedia == EarlyMedia.REMOTE)
                {
                    try
                    {
                        AudioMedia audioMedia = getActiveAudioMedia();
                        if (audioMedia != null)
                        {
                            toneGenerator.stopTransmit(audioMedia);
                        }
                    }
                    catch (Exception e)
                    {
                        Log.e(this, "stopEarlyMedia", e);
                    }
                }
                toneGenerator.stop();
                toneGenerator.delete();
                toneGenerator = null;
            }
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }
    }


    private void startRingBackTone()
    {
        ToneDescVector toneDescVector = new ToneDescVector();
        ToneDesc toneDesc = new ToneDesc();
        toneDesc.setFreq1((short) 425);
        toneDesc.setOn_msec((short) 1000);
        toneDesc.setOff_msec((short) 4000);
        toneDescVector.add(toneDesc);
        try
        {
            if (toneGenerator == null)
            {
                toneGenerator = new ToneGenerator();
                toneGenerator.createToneGenerator(8000, 1);
            }
            toneGenerator.play(toneDescVector, true);
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }
    }

    @Override
    public void onCallMediaState(final OnCallMediaStateParam prm)
    {
        super.onCallMediaState(prm);

        try
        {
            AudioMedia am = getActiveAudioMedia();
            if (am != null)
            {
                am.adjustRxLevel(2f);
                SipLib.ep.audDevManager().getCaptureDevMedia().adjustTxLevel(2f);
//                SipLib.ep.audDevManager().getCaptureDevMedia().adjustRxLevel(5f);

                am.startTransmit(SipLib.ep.audDevManager().getPlaybackDevMedia());
                SipLib.ep.audDevManager().getCaptureDevMedia().startTransmit(am);
            }
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }

    }

    public void setNumber(String mNumber)
    {
        this.mNumber = mNumber;
    }

    public String getNumber()
    {
        return mNumber;
    }

    public int getCallType()
    {
        return mCallType;
    }

    public void setCallType(int mCallType)
    {
        this.mCallType = mCallType;
    }

    private enum EarlyMedia
    {
        NONE,
        REMOTE,
        LOCAL
    }
}
