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

/**
 * Created by Cookie on 2016-09-09.
 */

public class StructuredName
{
    public String mDisplay;
    public String mGiven;
    public String mFamily;

    public StructuredName(final String display, final String given, final String family)
    {
        mDisplay = display;
        mGiven = given;
        mFamily = family;
    }

    public StructuredName(final String display)
    {
        mDisplay = display;
    }

    public String getName()
    {
        if (mDisplay != null)
        {
            return mDisplay;
        }
        else
        {
            return mGiven + " " + mFamily;
        }
    }

    @Override
    public String toString()
    {
        return "d=" + mDisplay + ", g=" + mGiven + ", f=" + mFamily;
    }
}
