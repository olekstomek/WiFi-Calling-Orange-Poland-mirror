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

import android.annotation.SuppressLint;
import android.provider.CallLog;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.SipHeader;
import org.pjsip.pjsua2.SipHeaderVector;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.HashMap;
import java.util.Set;

import pl.orangelabs.log.Log;

/**
 * Created by Cookie on 2016-08-12.
 */
public class SipAccount extends Account
{
    private SipService mSipService;
    private SipAccountData mData;
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, SipCall> activeCalls = new HashMap<>();

//    private SipService mSipService;

    SipAccount(final SipAccountData data, final SipService sipService)
    {
        super();
        mData = data;
        mSipService = sipService;
    }

    public SipService getSipService()
    {
        return mSipService;
    }

    public void create() throws Exception
    {
        create(mData.getAccountConfig(), true);
    }

    protected void removeCall(int callId)
    {
        SipCall call = activeCalls.get(callId);

        if (call != null)
        {
            Log.d(this, "Removing call with ID: " + callId);
            call.delete();
            activeCalls.remove(callId);
        }
    }

    public SipCall getCall(int callId)
    {
        return activeCalls.get(callId);
    }

    public Set<Integer> getCallIDs()
    {
        return activeCalls.keySet();
    }

    public int getCallSize()
    {
        Log.d(this, "getCallSize" + activeCalls.size());
        return activeCalls.size();
    }

    public int getActiveCallId()
    {
        for (final Integer callId : getCallIDs())
        {
            SipCall sipCall = getCall(callId);
            if (sipCall.getCurrentState() != pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED)
            {
                return callId;
            }
        }
        return -1;
    }
    public SipCall getActiveCall()
    {

        int activeCall = getActiveCallId();
        if (activeCall != -1)
        {
            return activeCalls.get(activeCall);
        }
        return null;
    }

    public String getActiveCallNumber()
    {
        int activeCall = getActiveCallId();
        if (activeCall < 0)
        {
            return "";
        }
        SipCall sipCall = getCall(getActiveCallId());
        try
        {
            SipCallInfo sipCallInfo = new SipCallInfo(sipCall.getInfo());
            return sipCallInfo.getDisplayName();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public SipCall addIncomingCall(int callId)
    {
        SipCall call = new SipCall(this, callId);
        call.setCallType(CallLog.Calls.MISSED_TYPE);
        activeCalls.put(callId, call);
        Log.d(this, "Added incoming call with ID " + callId + " to " + mData.getPhoneNo());
        return call;
    }

    public SipCall addOutgoingSOSCall()
    {
        SipCall call = new SipCall(this);
        CallOpParam callOpParam = new CallOpParam();

        final SipHeaderVector headerVector = callOpParam.getTxOption().getHeaders();
        SipHeader sosHeader = new SipHeader();
        sosHeader.setHName("To URN");
        sosHeader.setHValue("URN:service:sos");
        headerVector.add(sosHeader);

        callOpParam.getTxOption().setHeaders(headerVector);
        try
        {
            call.makeCall("URN:service:sos", callOpParam);
            activeCalls.put(call.getId(), call);
            Log.d(this, "New outgoing call with ID: " + call.getId());
            return call;

        }
        catch (Exception exc)
        {
            Log.d(this, "Error while making outgoing call", exc);
            return null;
        }
    }

    public SipCall addOutgoingCall(final String numberToDial)
    {
        SipCall call = new SipCall(this);
        call.setCallType(CallLog.Calls.OUTGOING_TYPE);
        call.setNumber(numberToDial);
        CallOpParam callOpParam = new CallOpParam();
        try
        {
            if (numberToDial.startsWith("sip:"))
            {
                call.makeCall(numberToDial, callOpParam);
            }
            else
            {
                call.makeCall("sip:" + numberToDial + "@" + mData.getDomain(), callOpParam);
//                call.makeCall("sip:" + numberToDial + "@" + mData.getDomain(), callOpParam);
            }

            activeCalls.put(call.getId(), call);
            Log.d(this, "New outgoing call with ID: " + call.getId());
            return call;

        }
        catch (Exception exc)
        {
            Log.d(this, "Error while making outgoing call", exc);
            return null;
        }
    }


    @Override
    public void onRegState(final OnRegStateParam prm)
    {
        super.onRegState(prm);
        Log.d(this, "onRegState");
        if (mSipService != null)
        {
            mSipService.updateService();
            mSipService.getRegState();
        }
    }

    @Override
    public void onIncomingCall(final OnIncomingCallParam prm)
    {
        super.onIncomingCall(prm);
        Log.d(this, "onIncomingCall");

        SipCall call = addIncomingCall(prm.getCallId());

        try
        {
            if (activeCalls.size() > 1)
            {
                call.sendBusyHereToIncomingCall();
                Log.d(this, "sending busy to call ID: " + prm.getCallId());
                return;
            }


            // Answer with 180 Ringing
            CallOpParam callOpParam = new CallOpParam();
            callOpParam.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
            call.answer(callOpParam);
            Log.d(this, "Sending 180 ringing");

            mSipService.startRingtone();

            SipCallInfo callInfo = new SipCallInfo(call.getInfo());
            call.setNumber(callInfo.getDisplayName());
            mSipService.initializeCall();
            mSipService.showIncomingCall(prm.getCallId(), callInfo.getDisplayName());
        }
        catch (Exception exc)
        {
            Log.d(this, "Error while getting call info", exc);
        }

    }
}
