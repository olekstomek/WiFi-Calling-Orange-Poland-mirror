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

import java.util.Locale;

import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;

/**
 * Created by Cookie on 2015-02-03.
 */
public class PhoneNumber
{

    public String mNumber;
    public String mRawNumber;
    public boolean isDefault;
    public String mPhoneKind;
    public int mPhoneKindId;

    public PhoneNumber()
    {
    }

    public PhoneNumber(final String number, final String phoneKind, final int type)
    {
        this.mNumber = number;
        this.mPhoneKind = phoneKind;
        this.mPhoneKindId = type;
    }

    public String GetNumberForSearch()
    {
        return mNumber != null ? mNumber : mRawNumber;
    }

    public String GetNumberForShow()
    {
        String phNumber = OLPPhoneNumberUtils.formatNumberToE164(GetNumberForSearch(), Locale.getDefault().getCountry());
        if (phNumber == null)
        {
            return GetNumberForSearch();
        }
        else
        {
            return phNumber;
        }
    }


}
