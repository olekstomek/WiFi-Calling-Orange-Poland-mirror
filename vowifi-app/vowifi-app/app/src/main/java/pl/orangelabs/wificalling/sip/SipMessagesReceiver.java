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

import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_state;

public abstract class SipMessagesReceiver
{
    protected void OnRegState(final int code, final boolean isRegistered)
    {
    }

    protected void OnCallState(final int callId, final pjsip_inv_state state, final pjsip_status_code status, final boolean isLocalHold,
                               final boolean isLocalMute, final long callDuration, final boolean isSpeakerOn)
    {
    }

    protected void OnStackState(final pjsua_state stackState, final SipService.DE_INIT_PLACES from)
    {
    }
}