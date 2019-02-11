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

package pl.orangelabs.wificalling.net.basic;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;

/**
 * Created by marcin on 05.12.16.
 */

public abstract class Response
{
    private String plainTextResponse;
    private int responseCode;

    public Response(String plainTextResponse, int responseCode)
    {
        this.plainTextResponse = plainTextResponse;
        this.responseCode = responseCode;
    }

    public Response()
    {
    }

    public String getPlainTextResponse()
    {
        return plainTextResponse;
    }

    public void setPlainTextResponse(String plainTextResponse)
    {
        this.plainTextResponse = plainTextResponse;
    }

    public int getResponseCode()
    {
        return responseCode;
    }

    public void setResponseCode(int responseCode)
    {
        this.responseCode = responseCode;
    }

    public abstract void parse() throws JSONException, UnsupportedEncodingException;

    public abstract boolean isCachedDataValid();

    public boolean isDataValid()
    {
        return getResponseCode() < 300;
    }

    public abstract String getFakeData();
}
