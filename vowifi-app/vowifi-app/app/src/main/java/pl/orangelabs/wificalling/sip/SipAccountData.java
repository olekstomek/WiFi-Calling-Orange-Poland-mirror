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

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountMwiConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.SipHeaderVector;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.pj_qos_params;
import org.pjsip.pjsua2.pjsua_100rel_use;
import org.pjsip.pjsua2.pjsua_call_hold_type;
import org.strongswan.android.logic.CharonVpnService;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.BuildConfig;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.SettingsApp;

import static org.pjsip.pjsua2.pjsua2JNI.PJ_FALSE_get;
import static pl.orangelabs.wificalling.sip.SipSettings.VOWIFI_P_HEADER;

public class SipAccountData
{
    private static String domain;
    //private final static String ip = "172.18.92.7";
    private static String prodIp;
    private static String testIp;
    //private static String domain = "172.18.91.247";
    //        private static String proxy = "10.120.15.106:5067";
    private final static String sip = "sip:";
    private final static String monkey = "@";
    private final static String VoWifiSuffix = "a";
    private static String relam;

    //  private final static String A_HEADER_VALUE = "Digest username=\"%s@ims.mnc003.mcc260.3gppnetwork.org\",realm=\"ims.mnc003.mcc260.3gppnetwork.org\",uri=\"sip:ims.mnc003.mcc260.3gppnetwork.org\",nonce=\"\",response=\"\",algorithm=MD5";

    private TransportConfig mTransportConfig = new TransportConfig();


    private final String mImsi;
    private final String mPhoneNo;
    private final String mPassword;

    /**
     * @param imsi
     * @param phoneNo
     *     phone number with +48
     * @param password
     */
    public SipAccountData(final String imsi, final String phoneNo, final String password)
    {
        this.mImsi = imsi;
        this.mPhoneNo = addCountryPrefix(phoneNo);
        this.mPassword = password;
    }
    @SuppressLint("HardwareIds")
    public SipAccountData(Context context)
    {
        if (BuildConfig.CONFIG_PROD)
        {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            this.mImsi = telephonyManager.getSubscriberId();
            this.mPhoneNo = App.getActivationComponent().getActivationDataKeeper().loadUserNumber();
            this.mPassword  = App.getActivationComponent().getActivationDataKeeper().loadSIPPassword();
        }
        else
        {
            this.mImsi = SettingsApp.sipRegisterImsi;
            this.mPhoneNo = SettingsApp.sipRegisterMsisdn;
            this.mPassword  = SettingsApp.sipPassword;
        }
        relam = context.getString(R.string.relam);
        testIp = context.getString(R.string.testIP);
        prodIp = context.getString(R.string.prodIP);
        domain = context.getString(R.string.domain);
    }

    private String addCountryPrefix(final String phoneNo)
    {
        if (phoneNo.startsWith("+48"))
        {
            return phoneNo;
        }
        else
        {
            return "+48" + phoneNo;
        }
    }

    public String getImsi()
    {
        return mImsi;
    }

    public String getPhoneNo()
    {
        return mPhoneNo;
    }

    public String getPassword()
    {
        return mPassword;
    }

    AccountConfig getAccountConfig()
    {
        final AccountConfig accountConfig = new AccountConfig();

        accountConfig.getRegConfig().setRegisterOnAdd(false);

        accountConfig.setIdUri(sip + getPhoneNo() + monkey + getDomain());

        if (SettingsApp.isProd)
        {
            accountConfig.getRegConfig().setRegistrarUri(sip + getIp());
        }
        else
        {
            accountConfig.getRegConfig().setRegistrarUri(sip + getDomain());
        }
        accountConfig.getRegConfig().setTimeoutSec(3600L);
        SipHeaderVector regHeaderVector = accountConfig.getRegConfig().getHeaders();
        regHeaderVector.add(VOWIFI_P_HEADER);
        //   VOWIFI_A_HEADER.setHValue(String.format(A_HEADER_VALUE, getImsi()));
        // regHeaderVector.add(VOWIFI_A_HEADER);
        accountConfig.getRegConfig().setHeaders(regHeaderVector);

        AuthCredInfoVector authCred = accountConfig.getSipConfig().getAuthCreds();
        authCred.clear();
        String userName = getImsi() + VoWifiSuffix + monkey + getDomain();

        AuthCredInfo credInfo = new AuthCredInfo();
        credInfo.setScheme("Digest");
        credInfo.setRealm(domain);
        credInfo.setUsername(userName);
        credInfo.setData(mPassword);
        //credInfo.setDataType(PJSIP_CRED_DATA_EXT_AKA.swigValue());

        //credInfo.setAkaK(mPassword);

        authCred.add(credInfo);


//        accountConfig.getNatConfig().setIceEnabled(true);
        accountConfig.getNatConfig().setSipOutboundUse(PJ_FALSE_get());
        accountConfig.getNatConfig().setUdpKaIntervalSec(15);
        accountConfig.getNatConfig().setContactRewriteMethod(1);
//          accountConfig.getNatConfig().setViaRewriteUse(PJ_FALSE_get());
        //       accountConfig.getNatConfig().setContactRewriteUse(PJ_FALSE_get());
//
        accountConfig.getCallConfig().setHoldType(pjsua_call_hold_type.PJSUA_CALL_HOLD_TYPE_RFC3264);
        accountConfig.getCallConfig().setPrackUse(pjsua_100rel_use.PJSUA_100REL_NOT_USED);

        AccountMwiConfig mwiConfig = accountConfig.getMwiConfig();
        mwiConfig.setEnabled(false);
        mwiConfig.setExpirationSec(3600L);
        accountConfig.setMwiConfig(mwiConfig);

        //     if (!SettingsApp.isProd)
        {
            accountConfig.getSipConfig().getProxies().add(sip + getProxy());
        }
//        accountConfig.getRegConfig().setTimeoutSec(3600L);
        //   accountConfig.getSipConfig().setContactForced(sip);


//        AccountPresConfig presHeaderVector = accountConfig.getPresConfig();
//        presHeaderVector.getHeaders().add(VOWIFI_P_HEADER);
//        accountConfig.setPresConfig(presHeaderVector);

//        mTransportConfig = new TransportConfig();
        // accountConfig.getMediaConfig().getTransportConfig();
        String lockalAddress = CharonVpnService.sLockalAddress;

        Log.d(this, "sLockalAddress" + lockalAddress);
        if (lockalAddress != null && !lockalAddress.isEmpty())
        {
            mTransportConfig.setPublicAddress(lockalAddress);
        }
        pj_qos_params qos_params = new pj_qos_params();
        qos_params.setFlags(SipSettings.PJ_QOS_PARAM_HAS_DSCP);
        qos_params.setDscp_val(SipSettings.PJ_QOS_VALUE_RTP);
        mTransportConfig.setQosParams(qos_params);

        accountConfig.getMediaConfig().setTransportConfig(mTransportConfig);

        return accountConfig;
    }

    public static String getIp()
    {
        String sipAddress = CharonVpnService.sSIPAddress;
        if (!sipAddress.isEmpty())
        {
            return sipAddress;
        }
        if (SettingsApp.isProd)
        {
            return prodIp;
        }
        else
        {
            return testIp;
        }
    }

    String getProxy()
    {
//        return getIp() + ":5060;lr;transport=tcp;hide";
        return getIp() + ":5060;lr;hide";
    }

    String getRealm()
    {
        return relam;
    }

    String getDomain()
    {
        return domain;
    }
}
