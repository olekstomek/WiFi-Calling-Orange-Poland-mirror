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

package pl.orangelabs.wificalling.model.db;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import java.util.HashMap;
import java.util.Locale;

import pl.orangelabs.wificalling.model.ContactDetails;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;

/**
 * Created by Cookie on 2016-09-08.
 */

public class ContactDbHelper
{
    private final Context mCtx;

    public ContactDbHelper(final Context ctx)
    {
        mCtx = ctx;
    }

    static HashMap<String, Integer> GetColumnIndexByProjection(final String[] projections, final Cursor cursor)
    {
        HashMap<String, Integer> map = new HashMap<>();
        for (String projection : projections)
        {
            map.put(projection, cursor.getColumnIndex(projection));
        }

        return map;

    }


    public ContactDetails GetContactByPhoneNumber(final String number)
    {
        final String[] projection = new String[] {
            BaseColumns._ID,
            ContactsContract.PhoneLookup.NORMALIZED_NUMBER,
            ContactsContract.PhoneLookup.NUMBER,
            ContactsContract.PhoneLookup.TYPE,
            ContactsContract.PhoneLookup.DISPLAY_NAME
        };

        ContactDetails contact = new ContactDetails();
        Cursor contactCursor = null;

        try
        {
            final Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            contactCursor = mCtx.getContentResolver().query(contactUri, projection, null, null, null);

            if (contactCursor == null || contactCursor.getCount() == 0)
            {
                return null;
            }

            final int nameIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            final int numberNormalizedIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.NORMALIZED_NUMBER);
            final int phoneNumberIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER);
            final int typeIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE);

            contactCursor.moveToFirst();
            do
            {
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.mRawNumber = contactCursor.getString(numberNormalizedIndex);
                if (phoneNumber.mRawNumber == null)
                {
                    phoneNumber.mRawNumber = contactCursor.getString(phoneNumberIndex);
                }
                int type = contactCursor.getInt(typeIndex);
                phoneNumber.mPhoneKind = mCtx.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type));

                phoneNumber.mNumber = OLPPhoneNumberUtils.SimpleNumber(phoneNumber.mRawNumber, Locale.getDefault().getCountry());
                contact.mPhoneNumberList.add(phoneNumber);
                contact.mDisplayName = contactCursor.getString(nameIndex);

            } while (contactCursor.moveToNext());
        }
        finally
        {
            if (contactCursor != null)
            {
                contactCursor.close();
            }
        }


        return contact;
    }
}
