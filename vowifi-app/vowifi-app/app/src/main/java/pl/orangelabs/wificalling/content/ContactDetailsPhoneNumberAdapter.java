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

import android.view.ViewGroup;

import pl.orangelabs.wificalling.content.holders.ContactDetailsPhoneNumberVH;
import pl.orangelabs.wificalling.content.items.PhoneNumberItem;

/**
 * @author Cookie
 */
public class ContactDetailsPhoneNumberAdapter extends BaseAdapter<PhoneNumberItem, ContactDetailsPhoneNumberVH>
{
    private final IOnItemAction mItemCallback;

    public ContactDetailsPhoneNumberAdapter(final IOnItemAction itemCallback)
    {
        mItemCallback = itemCallback;
    }

    @Override
    public ContactDetailsPhoneNumberVH onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        return new ContactDetailsPhoneNumberVH(parent);
    }

    @Override
    protected void bind(final ContactDetailsPhoneNumberVH holder, final PhoneNumberItem item, final int viewType)
    {
        holder.mPhoneKind.setText(item.mPhoneNumber.mPhoneKind);
        holder.mPhoneNumber.setText(item.mPhoneNumber.GetNumberForShow());
        holder.itemView.setOnClickListener(v -> mItemCallback.onEntryPressed(item));
    }

    public interface IOnItemAction
    {
        void onEntryPressed(final PhoneNumberItem item);
    }
}
