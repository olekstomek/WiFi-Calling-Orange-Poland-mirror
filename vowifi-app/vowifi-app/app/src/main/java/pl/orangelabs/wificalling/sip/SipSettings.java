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

import org.pjsip.pjsua2.SipHeader;

import pl.orangelabs.wificalling.BuildConfig;

/**
 * Created by Cookie on 2016-08-12.
 */
public class SipSettings
{
    public static final String USER_AGENT = "VoWiFi_" + BuildConfig.VERSION_NAME;
    public static final long SIP_PORT = 6000L;
    public static final int LOG_LEVEL = 5;

    public static final short PJ_QOS_PARAM_HAS_DSCP = 1;
    public static final short PJ_QOS_VALUE_UDP = 0x1A;
    public static final short PJ_QOS_VALUE_RTP = 0x2E;

    public static final SipHeader VOWIFI_P_HEADER = new SipHeader();
    public static final SipHeader VOWIFI_INVITE = new SipHeader();
    public static final SipHeader VOWIFI_INVITE_ERLY_MEDIA = new SipHeader();
    public static final String SIP_REGEX = "[^0-9 '*' '#' ]";

    static
    {
        VOWIFI_P_HEADER.setHName("P-Access-Network-Info");
        VOWIFI_P_HEADER.setHValue("IEEE-802.11");
        VOWIFI_INVITE.setHName("Accept-Contact");
        VOWIFI_INVITE.setHValue("*;+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.mmtel\"");
        VOWIFI_INVITE_ERLY_MEDIA.setHName("P-Early-Media");
        VOWIFI_INVITE_ERLY_MEDIA.setHValue("supported");
    }


}
