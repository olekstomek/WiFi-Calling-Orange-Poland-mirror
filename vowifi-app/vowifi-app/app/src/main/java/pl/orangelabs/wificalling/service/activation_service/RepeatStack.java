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

package pl.orangelabs.wificalling.service.activation_service;

import java.util.Stack;

/**
 * Created by marcin on 14.03.17.
 */

public class RepeatStack
{
    private RepeatObject currentRepeatObject = null;
    private Stack<RepeatObject> repeatObjectStack = new Stack<>();


    private boolean nextTime = false;

    RepeatStack()
    {
        repeatObjectStack.push(new RepeatObject(60L,1));
        repeatObjectStack.push(new RepeatObject(10800L,1));
        repeatObjectStack.push(new RepeatObject(3600L,2));
        repeatObjectStack.push(new RepeatObject(300L,2));
        repeatObjectStack.push(new RepeatObject(60L,1));
        currentRepeatObject = repeatObjectStack.pop();
    }
    public RepeatStack(boolean connection)
    {
        repeatObjectStack.push(new RepeatObject(300L,1));
        repeatObjectStack.push(new RepeatObject(60L,1));
        repeatObjectStack.push(new RepeatObject(30L,1));
        repeatObjectStack.push(new RepeatObject(10L,1));
        currentRepeatObject = repeatObjectStack.pop();
    }

    public Long getTime()
    {
        return currentRepeatObject.getTime();
    }

    public boolean isNextTime()
    {
        return nextTime;
    }

    public boolean hasNext()
    {
        if (currentRepeatObject.getRepeat() > 0)
        {
            currentRepeatObject.setRepeat(currentRepeatObject.getRepeat()-1);
            if (currentRepeatObject.getRepeat() == 0)
            {
                nextTime = false;
            }
        }
        else
        {
            if (!repeatObjectStack.empty())
            {
                currentRepeatObject = repeatObjectStack.pop();
                currentRepeatObject.setRepeat(currentRepeatObject.getRepeat()-1);
                nextTime = true;
            }
            else
            {
                return false;
            }
        }
        return true;
    }


    private class RepeatObject
    {
        public RepeatObject(long time, int repeat)
        {
            this.time = time;
            this.repeat = repeat;
        }
        private long time;
        private int repeat;

        public int getRepeat()
        {
            return repeat;
        }

        public void setRepeat(int repeat)
        {
            this.repeat = repeat;
        }

        public long getTime()
        {
            return time;
        }
    }
}
