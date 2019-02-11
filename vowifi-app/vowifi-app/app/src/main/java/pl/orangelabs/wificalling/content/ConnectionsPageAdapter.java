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
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.Locale;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.holders.ConnectionsVH;
import pl.orangelabs.wificalling.content.holders.DividerVH;
import pl.orangelabs.wificalling.content.items.ContactDetailsConnectionItem;
import pl.orangelabs.wificalling.content.items.DividerItem;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.ContactThumbnailTask;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;

/**
 * @author F
 */
public class ConnectionsPageAdapter extends BaseSwipeableAdapter<RecyclerItemBase, VHBase>
{
    public static final int VIEW_TYPE_CONNECTION = 1;
    public static final int VIEW_TYPE_DIVIDER = 2;
    private static final SimpleDateFormat CONNECTION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private OnConnectionClickListener mOnConnectionClickListener;

    public ConnectionsPageAdapter(final Context ctx, final RecyclerView recyclerView, final IOnAdapterAction callback,
    OnConnectionClickListener onConnectionClickListener)
    {
        super(ctx, recyclerView, callback);
        mOnConnectionClickListener = onConnectionClickListener;
    }

    @Override
    protected boolean isSwipeable(final RecyclerItemBase item)
    {
        return false;
    }

    @Override
    public VHBase onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        switch (viewType)
        {
            case VIEW_TYPE_CONNECTION:
                return new ConnectionsVH(parent);
            case VIEW_TYPE_DIVIDER:
                return new DividerVH(parent, mMetrics);
        }
        return null;
    }

    @Override
    protected void bind(final VHBase holder, final RecyclerItemBase item, final int viewType)
    {
        switch (viewType)
        {
            case VIEW_TYPE_CONNECTION:
                bindConnection((ConnectionsVH) holder, (ContactDetailsConnectionItem) item);
                break;
            case VIEW_TYPE_DIVIDER:
                bindDivider((DividerVH) holder, (DividerItem) item);
                break;
        }
    }

    private int connectionTypeToStringResId(final int type)
    {
        switch (type)
        {
            case CallLog.Calls.INCOMING_TYPE:
                return R.string.call_log_entry_type_incoming;
            case CallLog.Calls.OUTGOING_TYPE:
                return R.string.call_log_entry_type_outgoing;
            case CallLog.Calls.MISSED_TYPE:
                return R.string.call_log_entry_type_missed;
            case CallLog.Calls.VOICEMAIL_TYPE:
                return R.string.call_log_type_voice_mail;
            case CallLog.Calls.REJECTED_TYPE:
                return R.string.call_log_type_rejected;
            default:
                Log.i(this, "Unhandled connection type: " + type);
                return R.string.call_log_entry_type_other;
        }
    }
    private int connectionTypeToDrawableResId(final int type)
    {
        switch (type)
        {
            case CallLog.Calls.INCOMING_TYPE:
                return R.drawable.ic_incoming;
            case CallLog.Calls.OUTGOING_TYPE:
                return R.drawable.ic_outgoing;
            case CallLog.Calls.MISSED_TYPE:
                return R.drawable.ic_missed;
            default:
                Log.i(this, "Unhandled connection type: " + type);
                return R.drawable.ic_incoming;
        }
    }

    private String connectionTime(final long duration)
    {
        return String.format(Locale.US, "%d:%02d", duration / 60L, duration % 60L);
    }

    private void bindConnection(final ConnectionsVH holder, final ContactDetailsConnectionItem item)
    {
        final Context ctx = holder.itemView.getContext();
        final boolean hasName = !TextUtils.isEmpty(item.mCallLog.mDisplayName);
        holder.itemView.setOnClickListener(view -> mOnConnectionClickListener.OnConnectionClick(item));
        if (item.mCallLog.mPhoneNumber != null && item.mCallLog.mPhoneNumber.startsWith("-"))
        {
            holder.mHeader.setText(ctx.getString(R.string.call_log_entry_anonymouse_name));
            holder.mPhone.setText(ctx.getString(R.string.call_log_entry_anonymouse_number));
        }
        else
        {
            if (hasName)
            {
                holder.mHeader.setText(item.mCallLog.mDisplayName);
                holder.mPhone.setText(ctx.getString(R.string.call_log_entry_phone_field_formatting,
                    item.mCallLog.mPhoneKind, OLPPhoneNumberUtils.formatNumber(item.mCallLog.mPhoneNumber, Locale.getDefault().getCountry())));
            }
            else
            {
                holder.mHeader.setText(OLPPhoneNumberUtils.formatNumber(item.mCallLog.mPhoneNumber, Locale.getDefault().getCountry()));
                holder.mPhone.setText(item.mCallLog.mPhoneKind);
            }
        }

        if (item.mCallLog.mThumbnailUri != null)
        {
//            holder.mAvatar.setImageURI(item.mCallLog.mThumbnailUri);
            AsyncHelper.execute(new ContactThumbnailTask(holder.mAvatar, item.mCallLog.mThumbnailUri, ctx,R.drawable.ic_avatar_thumbnail));
        }
        else
        {
            holder.mAvatar.setImageResource(R.drawable.ic_avatar_thumbnail);
        }

        holder.mType.setText(ctx.getString(R.string.call_log_entry_time_field_formatting,
            ctx.getString(connectionTypeToStringResId(item.mCallLog.mType)), connectionTime(item.mCallLog.mDuration)));
        holder.mDate.setText(DateUtils.getRelativeDateTimeString(ctx, item.mCallLog.mDate, DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS, DateUtils.FORMAT_ABBREV_WEEKDAY));
        holder.mState.setImageDrawable(ResourcesCompat.getDrawable(ctx.getResources(),connectionTypeToDrawableResId(item.mCallLog.mType),null));
    }

    private void bindDivider(final DividerVH holder, final DividerItem item)
    {
        holder.mHeader.setText(item.mHeader);
        holder.mImage.setImageResource(item.mDividerData.mImageResId);
        holder.itemView.setBackgroundColor(item.mDividerData.mColor);
    }
    public interface OnConnectionClickListener
    {
        void OnConnectionClick(ContactDetailsConnectionItem contactDetailsConnectionItem);
    }
}
