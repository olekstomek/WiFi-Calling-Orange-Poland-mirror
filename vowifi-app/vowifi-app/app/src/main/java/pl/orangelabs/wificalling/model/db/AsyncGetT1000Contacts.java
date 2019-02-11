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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.t9.StructuredName;
import pl.orangelabs.wificalling.t9.T1000Entry;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * Created by Cookie on 2016-09-08.
 */
public class AsyncGetT1000Contacts extends AsyncTask<Void, Void, List<T1000Entry>>
{
    final private Context mCtx;
    final private OnContactDetailResultCallback mCallback;

    public AsyncGetT1000Contacts(final Context ctx, final OnContactDetailResultCallback callback)
    {
        mCtx = ctx;
        mCallback = callback;
    }

    @Override
    protected void onPostExecute(final List<T1000Entry> entryList)
    {
        super.onPostExecute(entryList);
        mCallback.onLoadCompleteListener(entryList);

    }

    @Override
    protected List<T1000Entry> doInBackground(final Void... params)
    {
        final long start = System.currentTimeMillis();
        final List<T1000Entry> t1000Entries = GetContacts2(mCtx);
        Log.i(this, "[perf] contacts loading: " + (System.currentTimeMillis() - start) + "ms");
        return t1000Entries;
    }

    private List<T1000Entry> GetContacts2(final Context ctx)
    {
        final String[] contactsProjection = new String[] {

            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,

            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.TIMES_CONTACTED,

            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,

            ContactsContract.Contacts.PHOTO_THUMBNAIL_URI

        };

        ContentResolver cr = ctx.getContentResolver();
        Cursor contactsCursor = cr.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            ContactsContract.Data.MIMETYPE + " IN (?, ?, ?)" + " AND " + ContactsContract.Data.HAS_PHONE_NUMBER + " > 0",
            new String[] {ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE},
            ContactsContract.Data.CONTACT_ID);

        if (contactsCursor != null && contactsCursor.getCount() > 0)
        {
            @SuppressLint("UseSparseArrays") final Map<Long, List<PhoneNumber>> partialPhoneResults = new HashMap<>();
            @SuppressLint("UseSparseArrays") final Map<Long, PartialNameResult> partialNameResults = new HashMap<>();
            @SuppressLint("UseSparseArrays") final Map<Long, PartialPhotoResult> partialPhotoResults = new HashMap<>();
            HashMap<String, Integer> contactIndex = ContactDbHelper.GetColumnIndexByProjection(contactsProjection, contactsCursor);
            contactsCursor.moveToFirst();
            do
            {
                final long id = contactsCursor.getLong(contactIndex.get(ContactsContract.Data.CONTACT_ID));
                switch (contactsCursor.getString(contactIndex.get(ContactsContract.Data.MIMETYPE)))
                {
                    case ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE:
                        addPartialPhotoResult(partialPhotoResults, id,
                            contactsCursor.getString(contactIndex.get(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)));
                        break;
                    case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:

                        String phoneNumber = contactsCursor.getString(contactIndex.get(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        int type = contactsCursor.getInt(contactIndex.get(ContactsContract.CommonDataKinds.Phone.TYPE));

                        String phoneNumberType;
                        if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                        {
                            phoneNumberType = contactsCursor.getString(contactIndex.get(ContactsContract.CommonDataKinds.Phone.LABEL));
                        }
                        else
                        {
                            phoneNumberType = mCtx.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type));
                        }

                        addPartialPhoneResult(partialPhoneResults, id, phoneNumber, phoneNumberType, type);

                        break;
                    case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                        String defaultDisplayName = contactsCursor.getString(contactIndex.get(ContactsContract.Data.DISPLAY_NAME));
                        String nameDisp =
                            contactsCursor.getString(contactIndex.get(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                        String nameGiven =
                            contactsCursor.getString(contactIndex.get(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                        String nameFam =
                            contactsCursor.getString(contactIndex.get(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                        final int timesContacted =
                            contactsCursor.getInt(contactIndex.get(ContactsContract.CommonDataKinds.StructuredName.TIMES_CONTACTED));

                        if (defaultDisplayName != null)
                        {
                            defaultDisplayName = defaultDisplayName.trim();
                        }
                        if (nameDisp != null)
                        {
                            nameDisp = nameDisp.trim();
                        }
                        if (nameGiven != null)
                        {
                            nameGiven = nameGiven.trim();
                        }
                        if (nameFam != null)
                        {
                            nameFam = nameFam.trim();
                        }

                        addPartialNameResult(partialNameResults, id, defaultDisplayName, nameDisp, nameGiven, nameFam, timesContacted);
                        break;
                }
            }
            while (contactsCursor.moveToNext());
            contactsCursor.close();
            return prepareMergedContacts(partialPhoneResults, partialNameResults, partialPhotoResults);
        }
        return new ArrayList<>();
    }

    private List<T1000Entry> prepareMergedContacts(final Map<Long, List<PhoneNumber>> partialPhoneResults,
                                                   final Map<Long, PartialNameResult> partialNameResults,
                                                   final Map<Long, PartialPhotoResult> partialPhotoResults)
    {
        final SharedPrefs prefs = new SharedPrefs(mCtx);
        final SharedPrefs.ContactDisplayMode dispMode =
            prefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_CONTACT_DISPLAY_MODE, SharedPrefs.Defaults.CONTACT_DISPLAY_MODE);
        final SharedPrefs.ContactDisplayMode sortMode =
            prefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_CONTACT_SORTING_MODE, SharedPrefs.Defaults.CONTACT_SORTING_MODE);
        return Stream.of(partialNameResults)

            .map(v -> new T1000Entry(v.getValue(), partialPhotoResults.get(v.getKey()), partialPhoneResults.get(v.getKey()), v.getKey()))
            .map(v -> v.updateDisplayMode(dispMode, sortMode))
            .sorted((lhs, rhs) -> lhs.sortableName().compareTo(rhs.sortableName()))
            .collect(Collectors.toList());
    }

    private void addPartialPhotoResult(final Map<Long, PartialPhotoResult> partialPhotoResults, final long id, final String uri)
    {
        if (uri == null)
        {
            return;
        }
        partialPhotoResults.put(id, new PartialPhotoResult(uri));
    }

    private void addPartialPhoneResult(final Map<Long, List<PhoneNumber>> partialPhoneResults, final long id, final String phone,
                                       final String phoneType, final int type)
    {
        if (!partialPhoneResults.containsKey(id))
        {
            partialPhoneResults.put(id, new ArrayList<>());
        }
        final String normalizedNumber = OLPPhoneNumberUtils.normalizeNumber(phone);
        PhoneNumber phoneNumber = new PhoneNumber(normalizedNumber, phoneType, type);
        partialPhoneResults.get(id).add(phoneNumber);
    }

    private void addPartialNameResult(final Map<Long, PartialNameResult> partialNameResults, final long id,
                                      final String defaultDisplayName, final String nameDisp, final String nameGiven,
                                      final String nameFamily, final int timesContacted)
    {
        StructuredName structuredName = null;
        if (nameDisp != null || nameGiven != null || nameFamily != null)
        {
            structuredName = new StructuredName(nameDisp, nameGiven, nameFamily);
        }
        partialNameResults.put(id, new PartialNameResult(structuredName, defaultDisplayName, timesContacted));
    }

    public interface OnContactDetailResultCallback
    {
        void onLoadCompleteListener(final List<T1000Entry> entryList);
    }

    public static class PartialPhotoResult
    {
        public final String mThumbnailUri;

        public PartialPhotoResult(final String thumbnailUri)
        {
            mThumbnailUri = thumbnailUri;
        }
    }

    public static class PartialNameResult
    {
        public final String mDefaultDisplayName;
        public final StructuredName mStructuredName;
        public int mTimesContacted;

        public PartialNameResult(@Nullable final StructuredName structuredName, final String defaultDisplayName, final int timesContacted)
        {
            mStructuredName = structuredName;
            mDefaultDisplayName = defaultDisplayName;
            mTimesContacted = timesContacted;
        }
    }
}
