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

import android.content.Context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * helper class to handle url + supplied params
 *
 * @author F
 */
public abstract class Request
{
    private final Map<String, String> mParams = new HashMap<>();
    private final Map<String, String> mBodyParams = new LinkedHashMap<>();
    private String mBase;
    private String mFullUrl;
    private String mFullBodyParams = null;
    private RequestMethod mRequestMethod;
    private String mHostName;

    public Request(final String baseUrl, String hostName, RequestMethod requestMethod)
    {
        mBase = baseUrl;
        mRequestMethod = requestMethod;
        mHostName = hostName;
        initParams();
    }
    public abstract void init(Context context);

    public Request(final Request baseRequest)
    {
        this(baseRequest.mBase,baseRequest.getHostName(), baseRequest.getRequestMethod());
        mParams.putAll(baseRequest.mParams);
    }

    public RequestMethod getRequestMethod()
    {
        return mRequestMethod;
    }

    private void initParams()
    {
        if (mBase == null)
        {
            return;
        }
        final String[] splitReq = mBase.split("\\?");
        if (splitReq.length == 2)
        {
            mBase = splitReq[0];
            final String[] params = splitReq[1].split("&");
            for (final String param : params)
            {
                final String[] keyValuePair = param.split("=");
                if (keyValuePair.length == 2)
                {
                    mParams.put(keyValuePair[0], keyValuePair[1]);
                }
            }
        }
    }

    public void addParam(final String key, final String value)
    {
        mFullUrl = null; // will force buildUrl() to recreate url
        mParams.put(key, value);
    }

    public void addParam(final String key, final int value)
    {
        addParam(key, Integer.toString(value));
    }

    public void removeParam(final String key)
    {
        if (mParams.containsKey(key))
        {
            mParams.remove(key);
        }
    }

    public void addBodyParam(final String key, final String value)
    {
        mBodyParams.put(key, value);
    }

    public void addBodyParam(final String key, final int value)
    {
        addBodyParam(key, Integer.toString(value));
    }

    public void removeBodyParam(final String key)
    {
        if (mBodyParams.containsKey(key))
        {
            mBodyParams.remove(key);
        }
    }


    public boolean hasParam(final String key)
    {
        return mParams.containsKey(key);
    }

    public String param(final String key)
    {
        return mParams.get(key);
    }

    public String buildUrl()
    {
        if (mFullUrl == null)
        {
            final StringBuilder sb = new StringBuilder(mBase);
            if (!mParams.isEmpty())
            {
                sb.append("?");
                for (final Map.Entry<String, String> entry : mParams.entrySet())
                {
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    sb.append("&");
                }
            }
            sb.deleteCharAt(sb.length()-1);
            mFullUrl = sb.toString();
        }
        return mFullUrl;
    }

    public String buildBodyParams()
    {
        if (mFullBodyParams == null)
        {
            final StringBuilder sb = new StringBuilder();
            if (!mBodyParams.isEmpty())
            {
                for (final Map.Entry<String, String> entry : mBodyParams.entrySet())
                {
                    sb.append(entry.getKey());
                    sb.append("=");
                    sb.append(entry.getValue());
                    sb.append("&");
                }
                sb.deleteCharAt(sb.length()-1);
            }
            mFullBodyParams = sb.toString();
        }

        return mFullBodyParams;
    }

    @Override
    public String toString()
    {
        return buildUrl();
    }

    public String getHostName()
    {
        return mHostName;
    }
}
