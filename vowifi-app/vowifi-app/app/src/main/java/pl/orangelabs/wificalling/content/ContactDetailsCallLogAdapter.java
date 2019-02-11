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

package pl.orangelabs.wificalling.content;

import android.content.Context;
import android.provider.CallLog;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.ViewGroup;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.holders.ContactDetailsCallLogVH;
import pl.orangelabs.wificalling.content.items.ContactDetailsConnectionItem;

/**
 * @author Cookie
 */
public class ContactDetailsCallLogAdapter extends BaseAdapter<ContactDetailsConnectionItem, ContactDetailsCallLogVH>
{
    private Context context;

    public ContactDetailsCallLogAdapter(Context context)
    {
        this.context = context;
    }

    @Override
    public ContactDetailsCallLogVH onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        return new ContactDetailsCallLogVH(parent);
    }

    @Override
    protected void bind(final ContactDetailsCallLogVH holder, final ContactDetailsConnectionItem item, final int viewType)
    {
        final Context context = holder.itemView.getContext();
        String timePassedString = (String) DateUtils.getRelativeDateTimeString(context, item.mCallLog.mDate, DateUtils.MINUTE_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_WEEKDAY);

        holder.mDate.setText(timePassedString);
        holder.mNumber.setText(item.mCallLog.mPhoneNumber);
        holder.mKind.setText(GetCallLogType(item.mCallLog.mType));
        if (item.mCallLog.mType == CallLog.Calls.MISSED_TYPE)
        {
            holder.mKind.setTextColor(ContextCompat.getColor(context, R.color.red));
        }
        else
        {
            holder.mKind.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
        }
        holder.mDuration.setText(getDurationText(item.mCallLog.mDuration));
    }

    private String getDurationText(final long duration)
    {
        String durationString;
        if (TimeUnit.SECONDS.toHours(duration) > 1)
        {
            durationString = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                TimeUnit.SECONDS.toHours(duration),
                TimeUnit.SECONDS.toMinutes(duration),
                TimeUnit.SECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(duration)));
        }
        else
        {
            durationString = String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.SECONDS.toMinutes(duration),
                TimeUnit.SECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(duration)));
        }
        return durationString;
    }

    private int GetCallLogType(final int type)
    {
        switch (type)
        {
            case CallLog.Calls.INCOMING_TYPE:
            {
                return R.string.call_log_type_incoming;
            }
            case CallLog.Calls.MISSED_TYPE:
            {
                return R.string.call_log_type_missed;
            }
            case CallLog.Calls.OUTGOING_TYPE:
            {
                return R.string.call_log_type_outgoing;
            }
            case CallLog.Calls.VOICEMAIL_TYPE:
            {
                return R.string.call_log_type_voice_mail;
            }
            case CallLog.Calls.REJECTED_TYPE:
            {
                return R.string.call_log_type_rejected;
            }
            default:
                return R.string.call_log_entry_type_other;
        }
    }
}
