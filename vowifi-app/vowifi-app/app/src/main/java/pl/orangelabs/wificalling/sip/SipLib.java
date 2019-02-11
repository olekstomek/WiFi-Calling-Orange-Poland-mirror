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

import com.annimon.stream.Stream;

import org.pjsip.pjsua2.AudDevManager;
import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.AudioMediaVector;
import org.pjsip.pjsua2.CodecInfoVector;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.IntVector;
import org.pjsip.pjsua2.LogConfig;
import org.pjsip.pjsua2.PresNotifyParam;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.pj_log_decoration;
import org.pjsip.pjsua2.pj_qos_params;
import org.pjsip.pjsua2.pj_qos_type;
import org.pjsip.pjsua2.pjsip_evsub_state;
import org.pjsip.pjsua2.pjsip_transport_type_e;
import org.pjsip.pjsua2.pjsua_state;
import org.strongswan.android.logic.CharonVpnService;

import java.util.concurrent.atomic.AtomicBoolean;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.util.Settings;

/**
 * Created by Cookie on 2016-08-12.
 */
public class SipLib
{
    public static Endpoint ep;// = new Endpoint();
    private static SipLog sipLog; //don't make it local!!!
    private static TransportConfig sipTpConfig;// = new TransportConfig();
    private static EpConfig epConfig;// = new EpConfig();

    private static SipAccount mSipAccount;
    private AtomicBoolean mIsInitialized = new AtomicBoolean(false);

    boolean getIsInitialized()
    {
        return mIsInitialized.get();
    }

    SipAccount getSipAccount()
    {
        return mSipAccount;
    }

    private static SipLib mInstance;

    public static SipLib getInstance()
    {
        if (mInstance == null)
        {
            mInstance = new SipLib();
        }
        return mInstance;
    }

    private void Initialize(final SipService sipService)
    {
        Log.d(this, "init");
        if (mIsInitialized == null || mIsInitialized.get())
        {
            return;
        }

        try
        {
            if (ep == null)
            {
                ep = new Endpoint();
                sipTpConfig = new TransportConfig();
                epConfig = new EpConfig();
            }

            ep.libCreate();

            if (Settings.LOGGING_ENABLED)
            {
                /* Set log config. */
                LogConfig log_cfg = epConfig.getLogConfig();
                sipLog = new SipLog();
                log_cfg.setLevel(6L);
                log_cfg.setConsoleLevel(6L);
                log_cfg.setWriter(sipLog);
                log_cfg.setDecor(log_cfg.getDecor() &
                                 ~(pj_log_decoration.PJ_LOG_HAS_CR.swigValue() |
                                   pj_log_decoration.PJ_LOG_HAS_NEWLINE.swigValue() |
                                   pj_log_decoration.PJ_LOG_HAS_THREAD_SWC.swigValue()
                                 ));
            }

            //  epConfig.getMedConfig().setThreadCnt(2);

            UaConfig ua_cfg = epConfig.getUaConfig();
            ua_cfg.setUserAgent(SipSettings.USER_AGENT + " " + ep.libVersion().getFull());
            ua_cfg.setMaxCalls(1);

            ep.libInit(epConfig);

            epConfig.getMedConfig().setNoVad(true);


            sipTpConfig.setPort(SipSettings.SIP_PORT);
            sipTpConfig.setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
            pj_qos_params qos_params = new pj_qos_params();
            qos_params.setFlags(SipSettings.PJ_QOS_PARAM_HAS_DSCP);
            qos_params.setDscp_val(SipSettings.PJ_QOS_VALUE_UDP);
            sipTpConfig.setQosParams(qos_params);

            String localAddress = CharonVpnService.sLockalAddress;
            if (localAddress != null && !localAddress.isEmpty())
            {
                sipTpConfig.setPublicAddress(localAddress);
            }
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
//
            sipTpConfig.setPort(sipTpConfig.getPort() + 10);
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);

//            ep.med


//            digitVector = new ToneDigitVector();


            CodecInfoVector codecInfoVector = ep.codecEnum();
            for (int i = 0; i < codecInfoVector.size(); i++)
            {
//                Log.d(this, "codec" + codecInfoVector.get(i).getDesc());
                Log.d(this, "codec" + codecInfoVector.get(i).getCodecId());
//                if (codecInfoVector.get(i).getCodecId().contains("AMR") && !codecInfoVector.get(i).getCodecId().contains("PCMA"))
//                {
//                    Log.d(this, "codec prio");
                ep.codecSetPriority(codecInfoVector.get(i).getCodecId(), (short) 0);
//                }
                if (codecInfoVector.get(i).getCodecId().contains("PCMA"))
                {
                    ep.codecSetPriority(codecInfoVector.get(i).getCodecId(), (short) 1);
                }

                if (codecInfoVector.get(i).getCodecId().contains("AMR/8000"))
                {
                    ep.codecSetPriority(codecInfoVector.get(i).getCodecId(), (short) 2);
                }
                if (codecInfoVector.get(i).getCodecId().contains("AMR-WB"))
                {
                    ep.codecSetPriority(codecInfoVector.get(i).getCodecId(), (short) 3);
                }
            }

            ep.libStart();

            sipService.notifyStackState(ep.libGetState());
            mIsInitialized = new AtomicBoolean(true);
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
            mIsInitialized = new AtomicBoolean(false);
            sipService.notifyStackState(pjsua_state.PJSUA_STATE_NULL);
        }
    }


    void DeInitialize(final SipService sipService)
    {
        // Try force GC to avoid late destroy of PJ objects as they should be deleted before lib is destroyed.
        Log.d(this, "DeInitialize");
        if (mIsInitialized == null || !mIsInitialized.get())
        {
            sipService.notifyStackState(pjsua_state.PJSUA_STATE_NULL);
            return;
        }
        else
        {
            mIsInitialized = new AtomicBoolean(false);
        }

        Log.d(this, "DeInitialize deleteAudioMedia();");
        deleteAudioMedia();

        Log.d(this, "DeInitialize deleteCall();");
        deleteCall(sipService);

        Log.d(this, "DeInitialize deleteAccount();");
        deleteAccount();

//        Log.d(this, "libStopWorkerThreads");
//        ep.libStopWorkerThreads();

//        epConfig.getUaConfig().delete();
//        epConfig.getMedConfig().delete();
//        epConfig.getLogConfig().delete();
//        epConfig.delete();

        try
        {
            IntVector transportVector = ep.transportEnum();
            for (int i = 0; i < transportVector.size(); i++)
            {
                Log.d(this, "DeInitialize transportClose();");
                ep.transportClose(i);
            }
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }
        Log.d(this, "DeInitialize sipTpConfig.delete();");
        sipTpConfig.delete();
        sipTpConfig = null;

//        if (sipLog != null)
//        {
//            Log.d(this, "DeInitialize sipLog.delete();");
////            sipLog.delete();
//            epConfig.getLogConfig().setWriter(null);
//        }

        // Shutdown pjsua. Note that Endpoint destructor will also invoke libDestroy(), so this will be a test of double libDestroy().

        try
        {

            Runtime.getRuntime().gc();
            Thread.sleep(200);
            Log.d(this, "DeInitialize ep.libDestroy();");
            ep.libDestroy();
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }

        // Force delete Endpoint here, to avoid deletion from a non-registered thread (by GC?).
        Log.d(this, "DeInitialize ep.delete();");
        ep.delete();
        ep = null;

        sipService.notifyStackState(pjsua_state.PJSUA_STATE_NULL);
    }

    private void deleteCall(final SipService sipService)
    {
        try
        {
            ep.hangupAllCalls();
            if (mSipAccount != null)
            {
                Stream.of(mSipAccount.getCallIDs()).filter(callId -> callId != -1).forEach(callId ->
                {
                    SipCall call = mSipAccount.getCall(callId);
                    if (call != null)
                    {
                        call.delete();
                        call = null;
                    }
                    sipService.notifyCallDisconnected(callId);
                });
            }

        }
        catch (Exception e)
        {
            Log.d(this, "deleteCall");
        }
    }

    private void deleteAudioMedia()
    {
        try
        {
            AudioMediaVector audioMediaVector = ep.mediaEnumPorts();
            for (int i = 0; i < audioMediaVector.size(); i++)
            {
                Log.d(this, "audioMedia deleted");
                AudioMedia audioMedia = audioMediaVector.get(i);
                ep.mediaRemove(audioMedia);
                audioMedia.delete();
            }
            Log.d(this, "audDevManager deleted");
            ep.audDevManager().delete();
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }
    }


    public AudDevManager getAudDevManager()
    {
        return ep.audDevManager();
    }


    void addAccount(final SipAccountData sipAccountData, SipService sipService)
    {
        SipLib.getInstance().Initialize(sipService);

        Log.d(this, "RegisterAccount");

        mSipAccount = new SipAccount(sipAccountData, sipService);
        try
        {
            mSipAccount.create();
        }
        catch (final Exception e)
        {
            Log.e(this, "", e);
        }
    }

    void RegisterAccount() throws Exception
    {
        if (mSipAccount != null)
        {
            mSipAccount.setRegistration(true);

        }
    }

    void deleteAccount()
    {
        if (mSipAccount != null)
        {
            try
            {
                mSipAccount.delete();
            }
            catch (final Exception e)
            {
                Log.e(this, "", e);
            }
        }
        mSipAccount = null;
    }

    void ModifyAccount(final SipAccountData sipAccountData)
    {
        Log.d(this, "ModifyAccount");
        if (mSipAccount != null)
        {
            try
            {
                if (mSipAccount.isValid())
                {
                    mSipAccount.modify(sipAccountData.getAccountConfig());
                }
            }
            catch (final Exception e)
            {
                Log.e(this, "", e);
            }
        }
    }

    void unregisterAccounts()
    {
        Log.d(this, "unregisterAccounts");
        if (mSipAccount != null)
        {
            try
            {
                mSipAccount.setRegistration(false);
            }
            catch (final Exception e)
            {
                Log.e(this, "", e);
            }
        }
    }

    private void sendEvent()
    {
        PresNotifyParam param = new PresNotifyParam();
        param.setState(pjsip_evsub_state.PJSIP_EVSUB_STATE_ACTIVE);
        try
        {
            mSipAccount.presNotify(param);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int getCallsNumber()
    {
        return getSipAccount() != null ? getSipAccount().getCallSize() : 0;
    }
    public boolean isActiveCall()
    {
        return getSipAccount() != null && getSipAccount().getActiveCall() != null;
    }

}
