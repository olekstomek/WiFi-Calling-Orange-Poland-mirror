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

package pl.orangelabs.wificalling.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.support.annotation.NonNull;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.PermissionUtils;

/**
 * Created by Cookie on 2016-09-06.
 */

public class ContactCall
{
    public String mDisplayName;
    public String mPhoneNumber;
    public String mPhoneKind;
    public String mThumbUri;
    public String LookupKey;

    public static @NonNull ContactCall loadDetails(Context context, String phoneNumber)
    {
        if (!TextUtils.isEmpty(phoneNumber))
        {
            if (phoneNumber.startsWith("-"))
            {
                ContactCall contactDataToDisplay = new ContactCall();
                contactDataToDisplay.mDisplayName = context.getString(R.string.call_log_entry_anonymouse_name);
                contactDataToDisplay.mPhoneNumber = context.getString(R.string.call_log_entry_anonymouse_number);
                //  contactDataToDiplay.mPhoneKind = getString(R.string.call_log_entry_anonymouse_kind);
                return contactDataToDisplay;
            }
            ContactCall contactDataToDisplay = null;
            if(PermissionUtils.isPermissionGranted(Manifest.permission.READ_CONTACTS, context))
            {
                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
                String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.TYPE, ContactsContract.PhoneLookup.PHOTO_URI
                        , ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI, ContactsContract.PhoneLookup.LOOKUP_KEY, ContactsContract.PhoneLookup._ID};
                ContentResolver contentResolver = context.getContentResolver();
                Cursor cursor =
                        contentResolver.query(
                                uri,
                                projection,
                                null,
                                null,
                                null);
                if (cursor != null)
                {
                    while (cursor.moveToNext())
                    {
                        contactDataToDisplay = new ContactCall();
                        contactDataToDisplay.mDisplayName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                        contactDataToDisplay.mThumbUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI));
                        if (contactDataToDisplay.mThumbUri == null)
                        {
                            contactDataToDisplay.mThumbUri = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
                        }
                        contactDataToDisplay.mPhoneNumber = phoneNumber;
                        contactDataToDisplay.LookupKey = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY));
                        contactDataToDisplay.mPhoneKind = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.TYPE));
                    }
                    cursor.close();
                }
            }
            if (contactDataToDisplay == null)
            {
                contactDataToDisplay = new ContactCall();
                contactDataToDisplay.mDisplayName = phoneNumber;
            }
            return contactDataToDisplay;
        }
        return new ContactCall();
    }
}
