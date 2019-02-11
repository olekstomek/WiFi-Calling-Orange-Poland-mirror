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

package pl.orangelabs.wificalling.net.responses;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import pl.orangelabs.wificalling.net.basic.Response;
import pl.orangelabs.wificalling.service.activation_service.ActivationFailResponseCode;

import static pl.orangelabs.wificalling.net.params.ResponseParams.CERT;
import static pl.orangelabs.wificalling.net.params.ResponseParams.CODE;
import static pl.orangelabs.wificalling.net.params.ResponseParams.DESCRIPTION;
import static pl.orangelabs.wificalling.net.params.ResponseParams.RESPONSE_CODE;
import static pl.orangelabs.wificalling.net.params.ResponseParams.RESPONSE_DESCRIPTION;

/**
 * Created by marcin on 29.11.16.
 */

public class DefaultActivationServerResponse extends Response
{
    private String cert = null;
    private String ResponseDescription = null;
    private ActivationFailResponseCode activationFailResponseCode = null;


    @Override
    public void parse() throws JSONException, UnsupportedEncodingException
    {
        JSONObject jsonObj = new JSONObject(getPlainTextResponse());
        setResponseCode(jsonObj.getInt(RESPONSE_CODE));
        if (jsonObj.has(RESPONSE_DESCRIPTION))
        {
            String responseDescription = URLDecoder.decode(jsonObj.getString(RESPONSE_DESCRIPTION), "UTF-8");
            JSONObject responseObject = new JSONObject(responseDescription);
            setResponseDescription(responseObject.getString(DESCRIPTION));
            setActivationFailResponseCode(ActivationFailResponseCode.getEnumFromInt(Integer.valueOf(responseObject.getString(CODE))));
        }
        if (jsonObj.has(CERT))
        {
            setCert(jsonObj.getString(CERT));
        }
    }

    @Override
    public boolean isCachedDataValid()
    {
        return cert != null;
    }

    @Override
    public String getFakeData()
    {
        //TODO
        return "";
    }

    public String getCert()
    {
        return cert;
    }

    public void setCert(String cert) throws UnsupportedEncodingException
    {
        if (cert != null)
        {
            cert = URLDecoder.decode(cert, "utf-8");
        }
        this.cert = cert;
    }

    public String getResponseDescription()
    {
        return ResponseDescription;
    }

    public void setResponseDescription(String responseDescription)
    {
        ResponseDescription = responseDescription;
    }

    @Override
    public String toString()
    {
        return "response code : " + getResponseCode() + " response : " + getResponseDescription() + " Cert : " + getCert();
    }

    public ActivationFailResponseCode getActivationFailResponseCode()
    {
        return activationFailResponseCode;
    }

    public void setActivationFailResponseCode(ActivationFailResponseCode activationFailResponseCode)
    {
        this.activationFailResponseCode = activationFailResponseCode;
    }
}
