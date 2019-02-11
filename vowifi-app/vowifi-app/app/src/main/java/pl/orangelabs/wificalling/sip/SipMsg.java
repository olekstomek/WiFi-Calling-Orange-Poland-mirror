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

import java.util.ArrayList;

public class SipMsg
{
    public final static String INCOMING_CALL = "sip_incoming_call";
    public final static String CALL_STATE = "sip_call_state";
    public final static String REG_STATE = "sip_account_reg_state";
    public final static String STACK_STATE = "sip_stack_state";

    public static final ArrayList<String> ALL_ACTIONS = new ArrayList<>();
    public static final ArrayList<String> ACCOUNT_ACTIONS = new ArrayList<>();
    public static final ArrayList<String> CALL_ACTIONS = new ArrayList<>();

    static
    {
        ACCOUNT_ACTIONS.add(REG_STATE);

        CALL_ACTIONS.add(INCOMING_CALL);
        CALL_ACTIONS.add(CALL_STATE);

        ALL_ACTIONS.add(STACK_STATE);
        ALL_ACTIONS.addAll(ACCOUNT_ACTIONS);
        ALL_ACTIONS.addAll(CALL_ACTIONS);
    }


}