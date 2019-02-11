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

package pl.orangelabs.wificalling.sip;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

import pl.orangelabs.log.Log;

/**
 * Created by Cookie on 2016-08-12.
 */
public class SipLog extends LogWriter
{
    @Override
    public void write(LogEntry entry)
    {
        switch (entry.getLevel())
        {
            case 0:
                Log.e(this, entry.getMsg());
                break;
            case 1:
                Log.w(this, entry.getMsg());
                break;
            case 2:
                Log.d(this, entry.getMsg());
                break;
            case 3:
                Log.i(this, entry.getMsg());
                break;
            case 4:
                Log.v(this, entry.getMsg());
                break;
            case 5:
            default:
                Log.v(this, entry.getMsg());
                break;
        }
    }
}
