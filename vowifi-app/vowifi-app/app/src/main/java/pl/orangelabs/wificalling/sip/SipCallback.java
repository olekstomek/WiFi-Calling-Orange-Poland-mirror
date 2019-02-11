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

import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnMwiInfoParam;
import org.pjsip.pjsua2.OnRegStateParam;

/**
 * Created by Cookie on 2016-08-12.
 */
public interface SipCallback
{
    void onRegState(OnRegStateParam regStateParam);

    void onIncomingCall(final OnIncomingCallParam incomingCallParam);

    void onIncomingCall(final SipCall sipCall);

    void onInstantMessage(final OnInstantMessageParam instantMessageParam);

    void onMwiInfo(final OnMwiInfoParam mwiInfoParam);

    void onCallState(SipCall prm);

    void onCallMediaState(OnCallMediaStateParam prm);

}
