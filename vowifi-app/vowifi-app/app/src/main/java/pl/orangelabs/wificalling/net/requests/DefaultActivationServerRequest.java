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

package pl.orangelabs.wificalling.net.requests;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;

import pl.orangelabs.wificalling.BuildConfig;
import pl.orangelabs.wificalling.net.basic.Request;
import pl.orangelabs.wificalling.net.basic.RequestMethod;
import pl.orangelabs.wificalling.net.params.RequestParams;

/**
 * Created by marcin on 29.11.16.
 */

public abstract class DefaultActivationServerRequest extends Request
{

    protected DefaultActivationServerRequest(String path)
    {

        super(BuildConfig.APP_URL + path,"*.orange.pl", RequestMethod.POST);
    }

    public void addParams(@NonNull String imsi, @NonNull String imei, @NonNull String msisdn)
    {
        addParamIMSI(imsi);
        addParamIMEI(imei);
    }

    @SuppressLint("HardwareIds")
    @Override
    public void init(Context context)
    {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        this.addParamIMEI(telephonyManager.getDeviceId());
        this.addParamIMSI(telephonyManager.getSubscriberId());
    }

    public void addParamIMSI(@NonNull String imsi)
    {
        addBodyParam(RequestParams.PARAM_IMSI, imsi);
    }

    public void addParamIMEI(@NonNull String imei)
    {
        addBodyParam(RequestParams.PARAM_IMEI, imei);
    }
}
