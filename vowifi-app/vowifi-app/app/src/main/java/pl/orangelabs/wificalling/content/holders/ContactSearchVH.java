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

package pl.orangelabs.wificalling.content.holders;

import android.view.ViewGroup;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;

/**
 * @author F
 */
public class ContactSearchVH extends ContactsVH
{
    public final TextView mNumberView;

    public ContactSearchVH(final ViewGroup v)
    {
        super(v, R.layout.recspec_contacts_search_item);
        mNumberView = (TextView) itemView.findViewById(R.id.recspec_contacts_item_number);
    }
}
