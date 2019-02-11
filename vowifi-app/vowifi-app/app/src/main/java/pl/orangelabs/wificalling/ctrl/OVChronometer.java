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

package pl.orangelabs.wificalling.ctrl;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.Chronometer;

import java.util.concurrent.TimeUnit;

import pl.orangelabs.wificalling.util.flavoured.FontHandler;

public class OVChronometer extends Chronometer
{
    private long msElapsed = 0L;
    private long msLastElapsed = 0L;
    private boolean isRunning = false;

    public OVChronometer(final Context context)
    {
        super(context);
        init(context);
    }

    public OVChronometer(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public OVChronometer(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context)
    {
        FontHandler.setFont(this, true);
    }


    @Override
    public void start()
    {
        super.start();
        setBase(SystemClock.elapsedRealtime() - msElapsed);
        isRunning = true;
    }


    public void start(long callDurationInSec)
    {
        setBase(SystemClock.elapsedRealtime() -(callDurationInSec*1000));
        isRunning = true;
        super.start();
    }

    public long getElapsed()
    {
//        updateElapsed();
        return TimeUnit.MILLISECONDS.toSeconds(msElapsed);
    }

    private void updateElapsed()
    {
        msElapsed = (int) (SystemClock.elapsedRealtime() - this.getBase());
    }

    @Override
    public void stop()
    {
        super.stop();
        if (isRunning)
        {
            updateElapsed();
            msLastElapsed = getElapsed();
        }
        isRunning = false;
    }
}