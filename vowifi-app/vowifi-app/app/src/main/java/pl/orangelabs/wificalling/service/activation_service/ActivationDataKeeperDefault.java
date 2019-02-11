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

package pl.orangelabs.wificalling.service.activation_service;

import android.content.Context;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.Utils;

import static pl.orangelabs.wificalling.service.activation_service.KeystoreHandling.getDateAfterDays;

/**
 * Created by marcin on 20.01.17.
 */

public class ActivationDataKeeperDefault implements ActivationDataKeeper
{
    private Context mContext;
    private SharedPrefs sharedPrefs = null;
    private static String VPN_PASS = "PASS";
    private static String SIP_PASS = "PASS_SIP";
    private static String USER_NUMBER = "USER_NUMBER";
    private static String USER_IMSI = "USER_IMSI";
    private static String ACTIVATION_STATE = "ACTIVATION_STATE";
    private static String ACTIVATION_DATE = "ACTIVATION_DATE";
    private static String CERTIFICATE_EXPIRATION_DATE = "CERTIFICATE_EXPIRATION_DATE";
    private static int EXPIRATION_DAYS = 96;

    private static String ALIAS = "OrangeKeyAlias";

    public ActivationDataKeeperDefault(Context context)
    {
        mContext = context;
        sharedPrefs = new SharedPrefs(context);
    }
    @Override
    public void saveVPNPassword(String password)
    {
        String encryptedPassword = KeystoreHandling.encryptString(ALIAS,password,mContext);
        sharedPrefs.Save(VPN_PASS,encryptedPassword);
    }

    @Override
    public String loadVPNPassword()
    {
        return loadEncryptedString(VPN_PASS);
    }

    @Override
    public void saveSIPPassword(String password)
    {
        String encryptedPassword = KeystoreHandling.encryptString(ALIAS,password,mContext);
        sharedPrefs.Save(SIP_PASS,encryptedPassword);
    }

    @Override
    public String loadSIPPassword()
    {
        return loadEncryptedString(SIP_PASS);
    }

    @Override
    public String loadHashOldPassword()
    {
        return Utils.getHash256FromString(loadVPNPassword());
    }


    @Override
    public void saveUsersNumber(String userNumber)
    {
        sharedPrefs.Save(USER_NUMBER, userNumber);
    }

    @Override
    public String loadUserNumber()
    {
        return sharedPrefs.Load(USER_NUMBER, "");
    }

    @Override
    public void saveImsiConnectedWithPassword(String imsi)
    {
        sharedPrefs.Save(USER_IMSI,imsi);
    }

    @Override
    public String loadImsi()
    {
        return sharedPrefs.Load(USER_IMSI, "");
    }

    @Override
    public ActivationState loadState()
    {

        return ActivationState.values()[sharedPrefs.Load(ACTIVATION_STATE, 0)];
    }

    @Override
    public void saveState(ActivationState activationState)
    {
        sharedPrefs.Save(ACTIVATION_STATE, activationState.ordinal());
    }

    @Override
    public void saveActivationDate(long date)
    {
        sharedPrefs.Save(ACTIVATION_DATE, date);
    }

    @Override
    public long loadActivationDate()
    {
        return sharedPrefs.Load(ACTIVATION_DATE, Long.MAX_VALUE);
    }

    @Override
    public void saveCertificateExpirationDate()
    {
        sharedPrefs.Save(CERTIFICATE_EXPIRATION_DATE, getDateAfterDays(EXPIRATION_DAYS).getTime());
    }

    @Override
    public long loadCertificateExpirationDate()
    {
        return sharedPrefs.Load(CERTIFICATE_EXPIRATION_DATE, Long.MAX_VALUE);
    }

    @Override
    public void clearActivation()
    {
        saveVPNPassword("");
        saveSIPPassword("");
        saveImsiConnectedWithPassword("");
    }

    private String loadEncryptedString(String key)
    {
        String encryptedText = sharedPrefs.Load(key, null);
        if (encryptedText != null)
        {
            return KeystoreHandling.decryptString(ALIAS, encryptedText, mContext);
        }
        else
        {
            return "";
        }
    }
}
