package org.strongswan.android.logic;

import java.util.ArrayList;

public class IpsecpMsg
{
    public final static String STATE_CHANGE = "ipsec_state_change";

    public static final ArrayList<String> ALL_ACTIONS = new ArrayList<>();

    static
    {
        ALL_ACTIONS.add(STATE_CHANGE);
    }


}