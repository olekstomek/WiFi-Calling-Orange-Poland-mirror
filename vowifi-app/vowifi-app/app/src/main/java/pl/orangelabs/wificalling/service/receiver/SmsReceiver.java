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

package pl.orangelabs.wificalling.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.util.Settings;

/**
 * Created by damian on 10/14/16.
 */

public class SmsReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION))
        {
            final SmsMessage[] smsMessages;
            smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            try
            {
                if (smsMessages != null)
                {
                    for (final SmsMessage message : smsMessages)
                    {
                        String msg_from = message.getOriginatingAddress();
                        if (msg_from.equals(Settings.SMS_AUTHOR))
                        {
                            onWifiCallingSMS(message);
                        }
                    }
                }

                //TODO context.sendBroadcast();
            }
            catch (Exception e)
            {
                Log.d("Exception caught", e.getMessage());
            }
        }
    }

    private void onWifiCallingSMS(SmsMessage message)
    {
        App.getActivationComponent().onSMSHasCame(message.getMessageBody());
    }
}