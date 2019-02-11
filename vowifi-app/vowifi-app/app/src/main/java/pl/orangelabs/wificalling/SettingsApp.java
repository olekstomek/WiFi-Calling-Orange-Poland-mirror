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

package pl.orangelabs.wificalling;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.annimon.stream.Stream;

/**
 * Created by Cookie on 2016-09-12.
 */

public class SettingsApp
{
    public static final boolean isProd;
    public static final boolean showWifiSettings = true;
    static final String ipsecGateway;
    static String ipsecLocalId;
    static String ipsecPassword;
    static final String ipsecRemoteId;
    public static int ipsecKey;
    public static int ipsecCert;
    public static String sipRegisterMsisdn;
    public static String sipPassword;
    public static String sipRegisterImsi;
    public static boolean showRssiToast = false;
    public static boolean debugAdditionalInfo = true;
    public static final String[] MANDATORY_PERMISSIONS = new String[]
            {
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.RECEIVE_SMS,
            };
    public static final int REQUEST_CODE_PERMISSIONS = 21;
    static
    {
        isProd = BuildConfig.CONFIG_PROD;
        if (isProd)
        {
            debugAdditionalInfo =false;
            ipsecGateway = "epdg.epc.mnc003.mcc260.pub.3gppnetwork.org";
            ipsecRemoteId = "ims";
        }
        else
        {
            ipsecKey = R.raw.cert_key;
            ipsecCert = R.raw.cert_x509;
            ipsecLocalId = "A" + BuildConfig.CONFIG_IMSI + "@nai.epc.mnc003.mcc260.3gppnetwork.org";
            ipsecGateway = "217.116.100.1";
            ipsecRemoteId = "ims.test";
            ipsecPassword = BuildConfig.CONFIG_IPSEC_PASS;
            sipPassword = BuildConfig.CONFIG_SIP_PASS;
            sipRegisterImsi = BuildConfig.CONFIG_IMSI;
            sipRegisterMsisdn = BuildConfig.CONFIG_MSISDN;
        }
    }
    public static boolean isPermissionsGranted(Context context)
    {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return  Stream.of(MANDATORY_PERMISSIONS).filter(v -> ContextCompat.checkSelfPermission(context, v) != PackageManager.PERMISSION_GRANTED).count() <= 0L;
    }
}
