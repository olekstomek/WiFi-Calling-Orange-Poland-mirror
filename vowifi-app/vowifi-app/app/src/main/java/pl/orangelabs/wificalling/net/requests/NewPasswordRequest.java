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

package pl.orangelabs.wificalling.net.requests;

import android.content.Context;

import pl.orangelabs.wificalling.App;

import static pl.orangelabs.wificalling.net.params.RequestParams.PARAM_HASH_PASSWORD;

/**
 * Created by marcin on 29.11.16.
 */

public class NewPasswordRequest extends DefaultActivationServerRequest
{
    private static String path = "newPassword/";

    public NewPasswordRequest()
    {
        super(path);
    }

    @Override
    public void init(Context context)
    {
        super.init(context);
        addParamHashedPassword();
    }
    public void addParamHashedPassword()
    {
        addBodyParam(PARAM_HASH_PASSWORD, App.getActivationComponent().getActivationDataKeeper().loadHashOldPassword());
    }
}
