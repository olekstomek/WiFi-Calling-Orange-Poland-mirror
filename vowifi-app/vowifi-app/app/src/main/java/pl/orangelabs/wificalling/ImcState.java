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

package pl.orangelabs.wificalling;

public enum ImcState
{
    UNKNOWN(0),
    ALLOW(1),
    BLOCK(2),
    ISOLATE(3);

    private final int mValue;

    ImcState(int value)
    {
        mValue = value;
    }

    /**
     * Get the enum entry from a numeric value, if defined
     *
     * @param value
     *     numeric value
     * @return the enum entry or null
     */
    public static ImcState fromValue(int value)
    {
        for (ImcState state : ImcState.values())
        {
            if (state.mValue == value)
            {
                return state;
            }
        }
        return null;
    }

    /**
     * Get the numeric value of the IMC state.
     *
     * @return numeric value
     */
    public int getValue()
    {
        return mValue;
    }
}