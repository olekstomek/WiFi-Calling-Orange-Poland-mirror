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

import android.net.Uri;

import java.util.List;

/**
 * Created by Cookie on 2016-09-06.
 */

public class VWCallLog
{
    public int id;
    public String mDisplayName;
    public String mPhoneNumber;
    public String mPhoneKind;
    public long mDate;
    public long mDuration;
    public int mType;
    public boolean mNew;
    public Uri mThumbnailUri;
    public String mContactUri;
//    public ContactDetails mContactDetails;

    public static boolean isOneNumber(List<VWCallLog> callLogs){
        VWCallLog tempVwCallLog = callLogs.get(0);
        if (tempVwCallLog.mDisplayName == null){
            return false;
        }
        for (VWCallLog callLog :callLogs) {
            if (callLog.mDisplayName == null || !tempVwCallLog.mDisplayName.equals(callLog.mDisplayName)){
                return false;
            }
        }
        return true;
    }
}
