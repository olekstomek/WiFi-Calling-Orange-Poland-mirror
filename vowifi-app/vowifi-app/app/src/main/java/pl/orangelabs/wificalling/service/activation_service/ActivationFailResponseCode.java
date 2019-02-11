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

/**
 * Created by marcin on 31.01.17.
 */

public enum ActivationFailResponseCode
{
    REPEAT_ERROR_WITHOUT_MESSAGE(99, 600, false, true),
    NO_REPEAT_ERROR_WITH_MESSAGE(599, 800, true, false),
    REPEAT_ERROR_WITH_MESSAGE(799, Integer.MAX_VALUE, true, true);


    private int minValue = -1;
    private int maxValue = -1;
    private int value = -1;
    private boolean showMessage = false;
    private boolean repeatError = false;
    ActivationFailResponseCode(int minValue, int maxValue, boolean showMessage, boolean repeatError)
    {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.showMessage = showMessage;
        this.repeatError = repeatError;
    }

    public static ActivationFailResponseCode getEnumFromInt(int value)
    {
        for (ActivationFailResponseCode responseCode : ActivationFailResponseCode.values())
        {
            if (value > responseCode.minValue && value < responseCode.maxValue)
            {
                responseCode.setValue(value);
                return responseCode;
            }
        }
        return null;
    }

    public void setValue(int value)
    {
        this.value = value;
    }

    public boolean isShowMessage()
    {
        return showMessage;
    }

    public boolean isRepeatError()
    {
        return repeatError;
    }

    public int getMinValue()
    {
        return minValue;
    }
    public int getMaxValue()
    {
        return maxValue;
    }
}
