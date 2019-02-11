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

package pl.orangelabs.wificalling.t9;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.model.db.AsyncGetT1000Contacts;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * @author F
 */
public class T1000Entry
{
    public final LastSearchMatch mLastSearchMatch = new LastSearchMatch();
    private final List<NamePartNumbers> mNamePartNumbers = new ArrayList<>();
    public long mId;
    public String mThumbnailUri;
    public int mTimesContacted;
    public List<PhoneNumber> mNumbers;
    private String mName;
    private StructuredName mStructuredName;
    private String mNameNumbers;
    private SharedPrefs.ContactDisplayMode mNameNumbersMode;
    private SharedPrefs.ContactDisplayMode mSortingMode;
    private String mDisplayableName;
    private String mSortableName;

//    public T1000Entry(final String name, final List<String> number, final long id)
//    {
//        mStructuredName = new StructuredName(name);
//        mName = name;
//        mNumbers = number;
//        mNameNumbers = convertToNumbers(mName.toLowerCase(Locale.US));
//        mId = id;
//    }
//
//    public T1000Entry(final StructuredName structuredName, final List<String> number, final long id)
//    {
//        mStructuredName = structuredName;
//        mName = mStructuredName.getName();
//        mNumbers = number;
//        mNameNumbers = convertToNumbers(mName.toLowerCase(Locale.US));
//        mId = id;
//    }

    public T1000Entry(@NonNull final AsyncGetT1000Contacts.PartialNameResult result,
                      @Nullable final AsyncGetT1000Contacts.PartialPhotoResult photoResult,
                      @Nullable final List<PhoneNumber> number, final long id)
    {
        mStructuredName = result.mStructuredName == null ? new StructuredName(result.mDefaultDisplayName) : result.mStructuredName;
        mName = mStructuredName.getName();
        mThumbnailUri = photoResult == null ? null : photoResult.mThumbnailUri;
        mNumbers = number == null ? new ArrayList<>() : number;
        mTimesContacted = result.mTimesContacted;
        mId = id;
    }

    private static String convertToNumbers(final String name)
    {
        final StringBuilder sb = new StringBuilder();
        final String lowerName = name.toLowerCase(Locale.US);
        for (int i = 0; i < lowerName.length(); ++i)
        {
            final char ch = lowerName.charAt(i);
            if (T1000Dictionary.DIGIT_MAPPING.containsKey(ch))
            {
                sb.append(T1000Dictionary.DIGIT_MAPPING.get(ch));
            }
        }
        return sb.toString();
    }

    public T1000Entry updateDisplayMode(final SharedPrefs.ContactDisplayMode displayMode, final SharedPrefs.ContactDisplayMode sortMode)
    {
        if (mNameNumbersMode != displayMode || mNameNumbers == null || mSortingMode != sortMode)
        {
            mNameNumbersMode = displayMode;
            mSortingMode = sortMode;

            final boolean hasNameParts = !TextUtils.isEmpty(mStructuredName.mGiven) && !TextUtils.isEmpty(mStructuredName.mFamily);
            if (hasNameParts)
            {
                switch (mNameNumbersMode)
                {
                    default:
                    case FAMILY_NAME:
                        mDisplayableName = mStructuredName.mFamily + " " + mStructuredName.mGiven;
                        break;
                    case GIVEN_NAME:
                        mDisplayableName = mStructuredName.mGiven + " " + mStructuredName.mFamily;
                        break;
                }
                switch (mSortingMode)
                {
                    default:
                    case FAMILY_NAME:
                        mSortableName = mStructuredName.mFamily + " " + mStructuredName.mGiven;
                        break;
                    case GIVEN_NAME:
                        mSortableName = mStructuredName.mGiven + " " + mStructuredName.mFamily;
                        break;
                }
            }
            else if (!TextUtils.isEmpty(mStructuredName.mDisplay))
            {
                mDisplayableName = mStructuredName.mDisplay;
                mSortableName = mStructuredName.mDisplay;
            }
            else
            {
                mDisplayableName = mName;
                mSortableName = mName;
            }
            mSortableName = mSortableName.toLowerCase(Locale.US);

            mNamePartNumbers.clear();
            mNameNumbers = convertToNumbers(mDisplayableName);
            final String[] split = mDisplayableName.split(" ");
            for (int i = 0; i < split.length; ++i)
            {
                mNamePartNumbers.add(new NamePartNumbers(split[i], mDisplayableName, i > 0 ? mNamePartNumbers.get(i - 1).mIndex + 1 : 0));
            }
        }
        return this;
    }

    public boolean matches(final String str)
    {
        if (str.isEmpty())
        {
            mLastSearchMatch.clear();
            return true;
        }
        for (final NamePartNumbers namePart : mNamePartNumbers)
        {
            if (namePart.mPart.startsWith(str)) // single word match
            {
                mLastSearchMatch.update(LastSearchMatchType.NAME, null, namePart.mIndex, namePart.mIndex + str.length());
                return true;
            }
        }
        if (mNameNumbers.startsWith(str)) // whole displayed name match
        {
            final int length = str.length();
            int spaceCount = 0;
            for (int i = 1; i < mNamePartNumbers.size(); ++i)
            {
                if (length < mNamePartNumbers.get(i).mIndex - spaceCount)
                {
                    break;
                }
                ++spaceCount;
            }
            mLastSearchMatch.update(LastSearchMatchType.NAME, null, 0, length + spaceCount);
            return true;
        }
        for (final PhoneNumber number : mNumbers)
        {
            final int matchPosition = number.mNumber.indexOf(str);
            if (matchPosition >= 0) // phone number match
            {
                mLastSearchMatch.update(LastSearchMatchType.NUMBER, number.mNumber, matchPosition, matchPosition + str.length());
                return true;
            }
        }
        mLastSearchMatch.clear();
        return false;
    }

    public String displayableName()
    {
        return mDisplayableName;
    }

    public String sortableName()
    {
        return mSortableName;
    }

    public boolean hasPhoneNumber()
    {
        return mNumbers != null && !mNumbers.isEmpty();
    }

    public String anyPhoneNumber()
    {
        return hasPhoneNumber() ? mNumbers.get(0).mNumber : null;
    }

    public PhoneNumber getPhoneNumberByNumber(final String phoneNumber)
    {
        String simpleNumber = OLPPhoneNumberUtils.SimpleNumber(phoneNumber, Locale.getDefault().getCountry());

        for (PhoneNumber number : mNumbers)
        {
            if (simpleNumber.equals(OLPPhoneNumberUtils.SimpleNumber(number.GetNumberForSearch(), Locale.getDefault().getCountry())))
            {
                return number;
            }
        }

        return new PhoneNumber(phoneNumber, null, OLPPhoneNumberUtils.GetPhoneType(phoneNumber, Locale.getDefault().getCountry()));
    }

    public enum LastSearchMatchType
    {
        NONE,
        NAME,
        NUMBER
    }

    private static class NamePartNumbers
    {
        private final String mPart;
        private final int mIndex;

        private NamePartNumbers(final String fullPart, final String displayableName, final int startIndex)
        {
            mPart = convertToNumbers(fullPart);
            mIndex = displayableName.indexOf(fullPart, startIndex);
        }
    }

    public static class LastSearchMatch
    {
        public LastSearchMatchType mMatchType;
        public String mNumber;
        public int mIndexStart;
        public int mIndexEnd;

        private LastSearchMatch()
        {
            mMatchType = LastSearchMatchType.NONE;
        }

        private void update(final LastSearchMatchType matchType, final String number, final int indexStart, final int indexEnd)
        {
            mMatchType = matchType;
            mNumber = number;
            mIndexStart = indexStart;
            mIndexEnd = indexEnd;
        }

        public void clear()
        {
            mMatchType = LastSearchMatchType.NONE;
        }
    }
}
