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

import java.util.Random;

import pl.orangelabs.wificalling.util.flavoured.DividersFlavouredDataSet;

/**
 * @author F
 */

public final class Dividers
{
    private static final DividerData[] DATA_CALLS;
    private static final DividerData[] DATA_CONTACTS;
    private static final Random RANDOM = new Random();

    static
    {
        DATA_CALLS = DividersFlavouredDataSet.getData(DividerType.CALLS);
        DATA_CONTACTS = DividersFlavouredDataSet.getData(DividerType.CONTACTS);
    }

    private Dividers()
    {
    }

    private static DividerData[] dataSet(final DividerType type)
    {
        return type == DividerType.CONTACTS ? DATA_CONTACTS : DATA_CALLS;
    }

    public static DividerData get(final DividerType type)
    {
        final DividerData[] dataSet = dataSet(type);
        return dataSet[RANDOM.nextInt(dataSet.length)];
    }

    public enum DividerType
    {
        CALLS,
        CONTACTS
    }

    public static class DividerData
    {
        public final int mColor;
        public final int mImageResId;

        public DividerData(final int color, final int imageResId)
        {
            mColor = color;
            mImageResId = imageResId;
        }
    }
}