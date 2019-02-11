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
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.HashMap;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.holders.ContactsVH;
import pl.orangelabs.wificalling.content.holders.DividerVH;
import pl.orangelabs.wificalling.content.items.ContactItem;
import pl.orangelabs.wificalling.content.items.DividerItem;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.ContactThumbnailTask;
import pl.orangelabs.wificalling.util.Utils;

/**
 * @author F
 */
public class ContactsPageAdapter<T extends RecyclerItemBase> extends BaseSwipeableAdapter<T, VHBase> implements FastScrollRecyclerView.SectionedAdapter
{
    public static final int VIEW_TYPE_CONTACT = 1;
    public static final int VIEW_TYPE_DIVIDER = 2;
    private HashMap<String, Integer> mMapIndex;

    public ContactsPageAdapter(final Context ctx, final RecyclerView recyclerView, final IOnAdapterAction callback)

    {
        super(ctx, recyclerView, callback);

    }

    @Override
    protected boolean isSwipeable(final RecyclerItemBase item)
    {
        return item.mType == VIEW_TYPE_CONTACT;
    }

    @Override
    public VHBase onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        switch (viewType)
        {
            case VIEW_TYPE_CONTACT:
                return new ContactsVH(parent);
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
            case VIEW_TYPE_CONTACT:
                bindContact((ContactsVH) holder, (ContactItem) item);
                break;
            case VIEW_TYPE_DIVIDER:
                bindDivider((DividerVH) holder, (DividerItem) item);
                break;
        }
    }

    private void bindDivider(final DividerVH holder, final DividerItem item)
    {
        holder.mHeader.setText(item.mHeader);
        holder.mImage.setImageResource(item.mDividerData.mImageResId);
        holder.itemView.setBackgroundColor(item.mDividerData.mColor);
    }

    protected void bindContact(final ContactsVH holder, final ContactItem item)
    {
        holder.mHeader.setText(item.mEntry.displayableName(), TextView.BufferType.SPANNABLE);
        holder.itemView.setOnClickListener(v -> item.mCallback.onPressed(holder, item));
        if (item.mEntry.mThumbnailUri != null)
        {
            AsyncHelper.execute(new ContactThumbnailTask(holder.mImageView, item.mEntry.mThumbnailUri, holder.mImageView.getContext()));
            //holder.mImageView.setImageURI(Uri.parse(item.mEntry.mThumbnailUri));
        }
        else
        {
            holder.mImageView.setImageResource(R.drawable.ic_avatar_thumbnail);
        }
    }


    @NonNull
    @Override
    public String getSectionName(int position)
    {
        if (item(position) instanceof DividerItem)
        {
            DividerItem item = (DividerItem) item(position);
            return item.mHeader;
        }
        else if (item(position) instanceof ContactItem)
        {
            ContactItem item = (ContactItem) item(position);
            return Utils.firstLetterUnicode(item.mEntry.sortableName());
        }
        return "";
    }
}
