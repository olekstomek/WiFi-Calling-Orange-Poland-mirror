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

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PrivateKey;

import pl.orangelabs.wificalling.net.params.RequestParams;
import pl.orangelabs.wificalling.service.activation_service.CSRKey;
import pl.orangelabs.wificalling.service.activation_service.CsrHelper;

/**
 * Created by marcin on 29.11.16.
 */

public class DefaultActivationServerRequestCSR extends DefaultActivationServerRequest
{
    public PrivateKey privateKey = null;
    protected DefaultActivationServerRequestCSR(String path)
    {
        super(path);
    }

    @Override
    public void init(Context context)
    {
        super.init(context);
        generateCSR(context);
    }

    public void addParamCSR(@NonNull String csr)
    {
        try
        {
            csr =  URLEncoder.encode(csr,"utf-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        addBodyParam(RequestParams.PARAM_CSR, csr);
    }

    protected void addParams(@NonNull String imsi,@NonNull String imei,@NonNull String msisdn,@NonNull String csr)
    {
        addParams(imsi,imei,msisdn);
        addParamCSR(csr);
    }
    private void generateCSR(Context context)
    {
        CSRKey csrKey = CsrHelper.getFormattedCSR(context);
        assert csrKey != null;
        setPrivateKey(csrKey.getPrivateKey());
        this.addParamCSR(csrKey.getCert());
    }

    public void setPrivateKey(PrivateKey privateKey)
    {
        this.privateKey = privateKey;
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }
}
