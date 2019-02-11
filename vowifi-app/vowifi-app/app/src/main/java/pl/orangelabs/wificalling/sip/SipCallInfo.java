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

import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.TimeVal;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Cookie on 2016-08-16.
 */
public class SipCallInfo
{
    private final CallInfo mCallInfo;
    String regex = "[<]?sip:+(.+?)@(.+?)[>]?";
    private final Pattern displayNamePattern = Pattern.compile(regex);//"^\"([^\"]+).*?sip:(.*?)>$");

    private static final String UNKNOWN = "Unknown";

    private String displayName;
    private String remoteUri;

    public SipCallInfo(final CallInfo callInfo)
    {
        mCallInfo = callInfo;
        String temp = callInfo.getRemoteUri();

        if (temp == null || temp.isEmpty())
        {
            displayName = remoteUri = UNKNOWN;
            return;
        }

        Matcher completeInfo = displayNamePattern.matcher(temp);
        if (completeInfo.matches())
        {
            displayName = completeInfo.group(1);
            remoteUri = completeInfo.group(2);
        }
        else
        {
            displayName = temp;
        }
    }

    public CallInfo getCallInfo()
    {
        return mCallInfo;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getRemoteUri()
    {
        return remoteUri;
    }

    /**
     * Get call duration in seconds
     *
     * @return call duration in seconds
     */
    public long getDuration()
    {
        TimeVal callTime = mCallInfo.getConnectDuration();
        return callTime.getSec() + TimeUnit.MILLISECONDS.toSeconds(callTime.getMsec());
    }

    public pjsip_inv_state getState()
    {
        return mCallInfo.getState();
    }

    public pjsip_status_code getLastStatusCode()
    {
        return mCallInfo.getLastStatusCode();
    }

    public int getId()
    {
        return mCallInfo.getId();
    }
}
