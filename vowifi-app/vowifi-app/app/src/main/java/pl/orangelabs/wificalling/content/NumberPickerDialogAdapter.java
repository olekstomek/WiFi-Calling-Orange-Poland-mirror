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

import pl.orangelabs.wificalling.content.holders.NumberPickerDialogVH;
import pl.orangelabs.wificalling.content.items.NumberPickerItem;

/**
 * @author Cookie
 */
public class NumberPickerDialogAdapter extends BaseAdapter<NumberPickerItem, NumberPickerDialogVH>
{
    public IOnPhoneAction mCallback;

    @Override
    public NumberPickerDialogVH onCreateViewHolder(final ViewGroup parent, final int viewType)
    {
        return new NumberPickerDialogVH(parent);
    }

    @Override
    protected void bind(final NumberPickerDialogVH holder, final NumberPickerItem item, final int viewType)
    {
        holder.mNumber.setText(item.mPhoneNumber.mNumber);
        holder.itemView.setOnClickListener(v -> {
            if (mCallback != null)
            {
                mCallback.onPressed(item.mPhoneNumber.mNumber);
            }
        });
        holder.mKind.setText(item.mPhoneNumber.mPhoneKind);
    }

    public void setCallback(final IOnPhoneAction callback)
    {
        mCallback = callback;
    }

    public interface IOnPhoneAction
    {
        void onPressed(final String number);
    }

}
