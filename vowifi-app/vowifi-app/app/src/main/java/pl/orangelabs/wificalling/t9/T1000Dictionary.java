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

import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * @author F
 */
public class T1000Dictionary
{
    /*package*/ static final Map<Character, Integer> DIGIT_MAPPING = new HashMap<>();

    static
    {
        DIGIT_MAPPING.put('a', 2);
        DIGIT_MAPPING.put('ą', 2);
        DIGIT_MAPPING.put('b', 2);
        DIGIT_MAPPING.put('c', 2);
        DIGIT_MAPPING.put('ć', 2);
        DIGIT_MAPPING.put('d', 3);
        DIGIT_MAPPING.put('e', 3);
        DIGIT_MAPPING.put('ę', 3);
        DIGIT_MAPPING.put('f', 3);
        DIGIT_MAPPING.put('g', 4);
        DIGIT_MAPPING.put('h', 4);
        DIGIT_MAPPING.put('i', 4);
        DIGIT_MAPPING.put('j', 5);
        DIGIT_MAPPING.put('k', 5);
        DIGIT_MAPPING.put('l', 5);
        DIGIT_MAPPING.put('ł', 5);
        DIGIT_MAPPING.put('m', 6);
        DIGIT_MAPPING.put('n', 6);
        DIGIT_MAPPING.put('ń', 6);
        DIGIT_MAPPING.put('o', 6);
        DIGIT_MAPPING.put('ó', 6);
        DIGIT_MAPPING.put('p', 7);
        DIGIT_MAPPING.put('q', 7);
        DIGIT_MAPPING.put('r', 7);
        DIGIT_MAPPING.put('s', 7);
        DIGIT_MAPPING.put('ś', 7);
        DIGIT_MAPPING.put('t', 8);
        DIGIT_MAPPING.put('u', 8);
        DIGIT_MAPPING.put('v', 8);
        DIGIT_MAPPING.put('w', 9);
        DIGIT_MAPPING.put('x', 9);
        DIGIT_MAPPING.put('y', 9);
        DIGIT_MAPPING.put('z', 9);
        DIGIT_MAPPING.put('ź', 9);
        DIGIT_MAPPING.put('ż', 9);

        DIGIT_MAPPING.put('1', 1);
        DIGIT_MAPPING.put('2', 2);
        DIGIT_MAPPING.put('3', 3);
        DIGIT_MAPPING.put('4', 4);
        DIGIT_MAPPING.put('5', 5);
        DIGIT_MAPPING.put('6', 6);
        DIGIT_MAPPING.put('7', 7);
        DIGIT_MAPPING.put('8', 8);
        DIGIT_MAPPING.put('9', 9);
        DIGIT_MAPPING.put('0', 0);
    }

    public static class T1000DataSet
    {
        public final List<T1000Entry> mEntries;
        private SharedPrefs.ContactDisplayMode mSortingMode;

        public T1000DataSet(final List<T1000Entry> entries)
        {
            mEntries = new ArrayList<>();
            mEntries.addAll(entries);
        }

        @Nullable
        public T1000Entry findByNumber(final String numberToFind)
        {
            try
            {
                if (numberToFind == null)
                {
                    return null;
                }

                ListIterator<T1000Entry> it = mEntries.listIterator();
                while (it.hasNext())
                {
                    T1000Entry item = it.next();
                    if (isEntryContainsNumber(numberToFind, item))
                    {
                        return item;
                    }
                }
            }
            catch (ConcurrentModificationException ex)
            {
                Log.e(this, "", ex);
            }

            return null;
            //   return Stream.of(mEntries).filter(v -> isEntryContainsNumber(numberToFind, v)).findFirst().orElse(null); Caused by: java.util.ConcurrentModificationException
        }

        private boolean isEntryContainsNumber(String numberToFind, T1000Entry v)
        {
            numberToFind = numberToFind.replace("+48","");
            if (v != null)
            {
                for (PhoneNumber number : v.mNumbers)
                {
                    if (number.mNumber != null && number.mNumber.contains(numberToFind))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        @Nullable
        public T1000Entry findByNormalizeNumber(final String number)
        {
            final String normalizeNumber = OLPPhoneNumberUtils.SimpleNumber(number, Locale.getDefault().getCountry());
            for (final T1000Entry entry : mEntries)
            {
                for (final PhoneNumber entryNumbers : entry.mNumbers)
                {
                    if (OLPPhoneNumberUtils.SimpleNumber(entryNumbers.GetNumberForSearch(), Locale.getDefault().getCountry())
                        .equals(normalizeNumber))
                    {
                        return entry;
                    }
                }
            }
            return null;
        }

        public void sort(final SharedPrefs.ContactDisplayMode sortingMode)
        {
            mSortingMode = sortingMode;
            final List<T1000Entry> sorted =
                Stream.of(mEntries)
                    .sorted(new Comparator<T1000Entry>()
                    {
                        private Collator collator = Collator.getInstance(new Locale("pl", "PL"));

                        @Override
                        public int compare(T1000Entry t1000Entry, T1000Entry t1)
                        {
                            return collator.compare(t1000Entry.sortableName(), t1.sortableName());
                        }
                    })
//                    .sorted((lhs, rhs) -> lhs.sortableName().compareTo(rhs.sortableName()))
                    .collect(Collectors.toList());
            mEntries.clear();
            mEntries.addAll(sorted);
        }
    }
}
