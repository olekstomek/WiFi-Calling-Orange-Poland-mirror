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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.util.Settings;


public class UrlLoader<ResponseT extends Response>
{
//    private static final String TAG = "JSONReader";

    // constructor
    public UrlLoader()
    {
        super();
    }


    public synchronized ResponseT getRawStringFromUrl(final String urlText, @Nullable String bodyParams, @Nullable String hostName, RequestMethod requestMethod, Class<ResponseT> responseTClass) throws IOException, IllegalAccessException, InstantiationException
    {
        ResponseT responseT = responseTClass.newInstance();
        if (Settings.LOGGING_ENABLED)
        {
            Log.i(this, String.format("Try to download data from: %s body param: %s request method: %s", urlText, bodyParams, requestMethod));
        }
        HttpsURLConnection urlConnection = initHttpsURLConnection(urlText, bodyParams, hostName, requestMethod);
        try
        {
            int responseCode = urlConnection.getResponseCode();
            responseT.setResponseCode(responseCode);
            if (responseCode < 300)
            {
                InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                final String data = readInputStream(inputStream);
                if (data != null && !data.isEmpty())
                {
                    responseT.setPlainTextResponse(data);
                }
            }
        }
        catch (IOException exception)
        {
            throw exception;
        }
        finally
        {
            urlConnection.disconnect();
        }
        return responseT;
    }

    @NonNull
    private HttpsURLConnection initHttpsURLConnection(String urlText, @Nullable String bodyParams, @Nullable String hostName, RequestMethod requestMethod) throws IOException
    {
        HostnameVerifier hostnameVerifier = null;
        if (hostName != null && !hostName.isEmpty())
        {
            hostnameVerifier = getHostnameVerifier(hostName);
        }
        URL url = new URL(urlText);
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestMethod(requestMethod.name());
        urlConnection.setHostnameVerifier(hostnameVerifier);
        urlConnection.setRequestProperty("Connection", "close");
        if (bodyParams != null && !bodyParams.isEmpty())
        {
            setBodyParams(bodyParams, urlConnection);
        }
        return urlConnection;
    }

    private void setBodyParams(@NonNull String bodyParams, HttpsURLConnection urlConnection) throws IOException
    {
        OutputStream os = urlConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(bodyParams);
        writer.flush();
        writer.close();
        os.close();
    }

    @NonNull
    private HostnameVerifier getHostnameVerifier(@Nullable final String hostName)
    {
        HostnameVerifier hostnameVerifier;
        hostnameVerifier = (hostname, session) ->
        {
            HostnameVerifier hv =
                    HttpsURLConnection.getDefaultHostnameVerifier();
            return hv.verify(hostName, session);
        };
        return hostnameVerifier;
    }
    public synchronized String readInputStream(final InputStream is) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line).append("\n");
        }
        is.close();
        return sb.toString();
    }
}
