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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.holders.ContactSearchVH;
import pl.orangelabs.wificalling.content.items.ContactItem;
import pl.orangelabs.wificalling.t9.T1000Entry;

/**
 * @author F
 */

public class ContactsSearchAdapter extends ContactsPageAdapter<ContactItem>
{

    private final ForegroundColorSpan mSearchMatchSpan;

    public ContactsSearchAdapter(final Context ctx, final RecyclerView recyclerView,
                                 final IOnAdapterAction callback)
    {
        super(ctx, recyclerView, callback);
        mSearchMatchSpan = new ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.colorAccent));
    }

    @Override
    protected boolean isSwipeable(final RecyclerItemBase item)
    {
        return false;
    }

    @Override
    public VHBase onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        if (viewType == VIEW_TYPE_CONTACT)
        {
            return new ContactSearchVH(parent);
        }
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    protected void bind(final VHBase holder, final RecyclerItemBase item, final int viewType)
    {
        super.bind(holder, item, viewType);
        if (viewType == VIEW_TYPE_CONTACT)
        {
            bindSearchContact((ContactSearchVH) holder, (ContactItem) item);
        }
    }

    private void bindSearchContact(final ContactSearchVH holder, final ContactItem item)
    {
        final boolean hasPhoneNumber = item.mEntry.hasPhoneNumber();
        holder.mNumberView.setVisibility(hasPhoneNumber ? View.VISIBLE : View.GONE);
        holder.mNumberView.setText(item.mEntry.anyPhoneNumber());
        bindContactMatching(holder, item.mEntry.mLastSearchMatch);
    }

    private void bindContactMatching(final ContactSearchVH holder, final T1000Entry.LastSearchMatch lastSearchMatch)
    {
        switch (lastSearchMatch.mMatchType)
        {
            case NAME:
                ((SpannableString) holder.mHeader.getText())
                    .setSpan(mSearchMatchSpan, lastSearchMatch.mIndexStart, lastSearchMatch.mIndexEnd, 0);
                break;
            case NUMBER:
                holder.mNumberView.setText(lastSearchMatch.mNumber, TextView.BufferType.SPANNABLE);
                ((SpannableString) holder.mNumberView.getText())
                    .setSpan(mSearchMatchSpan, lastSearchMatch.mIndexStart, lastSearchMatch.mIndexEnd, 0);
                break;
            default:
                break;
        }
    }
}
