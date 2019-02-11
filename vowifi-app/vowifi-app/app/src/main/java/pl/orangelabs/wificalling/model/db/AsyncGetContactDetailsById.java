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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

import java.io.InputStream;
import java.util.HashMap;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.model.ContactDetails;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;

public class AsyncGetContactDetailsById extends AsyncTask<Long, Void, ContactDetails>
{

    OnContactDetailResultCallback mOnResultCallback;

    private Context mCtx;

    public AsyncGetContactDetailsById(final Context ctx, final OnContactDetailResultCallback onResultCallback)
    {
        mCtx = ctx;
        mOnResultCallback = onResultCallback;
    }

    @Override
    protected ContactDetails doInBackground(final Long... params)
    {
        long contactId = params[0];
        return GetContactDetailsById(mCtx, contactId);
    }

    @Override
    protected void onPostExecute(final ContactDetails contactDetails)
    {
        mOnResultCallback.OnLoadCompleteListener(contactDetails);
    }

    private Bitmap loadContactPhoto(ContentResolver cr, long id)
    {

        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(cr, uri, true);
        if (input == null)
        {
            return null;
        }
        return BitmapFactory.decodeStream(input);
    }

    private ContactDetails GetContactDetailsById(final Context ctx, final long contactId)
    {
        final String[] projection = new String[] {
            BaseColumns._ID,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.LABEL
        };

        final String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        final String[] selectionArg = new String[] {String.valueOf(contactId)};

        Cursor phoneCursor = null;

        ContactDetails contact = new ContactDetails();
        try
        {
            phoneCursor = ctx.getContentResolver()
                .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, selectionArg, null);
            if (phoneCursor != null && phoneCursor.getCount() > 0)
            {
                final HashMap<String, Integer> indexMap = ContactDbHelper.GetColumnIndexByProjection(projection, phoneCursor);
                phoneCursor.moveToFirst();
                do
                {
                    PhoneNumber phoneNumber = new PhoneNumber();
                    phoneNumber.mRawNumber = phoneCursor.getString(indexMap.get(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                    if (phoneNumber.mRawNumber == null)
                    {
                        phoneNumber.mRawNumber = phoneCursor.getString(indexMap.get(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                    int type = phoneCursor.getInt(indexMap.get(ContactsContract.CommonDataKinds.Phone.TYPE));
                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                    {
                        phoneNumber.mPhoneKind = phoneCursor.getString(indexMap.get(ContactsContract.CommonDataKinds.Phone.LABEL));
                    }
                    else
                    {
                        phoneNumber.mPhoneKind = mCtx.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type));
                    }
                    phoneNumber.isDefault = phoneCursor.getInt(indexMap.get(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)) != 0;
                    phoneNumber.mNumber = OLPPhoneNumberUtils.SimpleNumber(phoneNumber.mRawNumber, "");
                    phoneNumber.mPhoneKindId = type;

                    contact.mPhoneNumberList.add(phoneNumber);
                    contact.mDisplayName = phoneCursor.getString(indexMap.get(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                } while (phoneCursor.moveToNext());
            }
            else
            {
                String[] contactProjection = new String[] {
                    BaseColumns._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                };

                final String contactSelection = ContactsContract.Contacts._ID + " = ?";
                final String[] contactSelectionArg = new String[] {String.valueOf(contactId)};
                phoneCursor =
                    ctx.getContentResolver()
                        .query(ContactsContract.Contacts.CONTENT_URI, contactProjection, contactSelection, contactSelectionArg, null);
                if (phoneCursor != null && phoneCursor.getCount() > 0)
                {

                    final HashMap<String, Integer> indexMap = ContactDbHelper.GetColumnIndexByProjection(contactProjection, phoneCursor);
                    phoneCursor.moveToFirst();
                    do
                    {
                        contact.mDisplayName = phoneCursor.getString(indexMap.get(ContactsContract.Contacts.DISPLAY_NAME));

                    } while (phoneCursor.moveToNext());
                }
            }
            contact.mPhotoBitmap = loadContactPhoto(ctx.getContentResolver(), contactId);
        }
        catch (Exception e)
        {
            Log.w(this, "error during gets phone numbers", e);
        }
        finally
        {
            if (phoneCursor != null)
            {
                phoneCursor.close();
            }
        }

        return contact;

    }

    public interface OnContactDetailResultCallback
    {
        void OnLoadCompleteListener(final ContactDetails contactDetails);
    }
}