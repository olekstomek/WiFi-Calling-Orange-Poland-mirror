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

import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.basic.Request;
import pl.orangelabs.wificalling.net.basic.Response;

/**
 * Created by marcin on 25.01.17.
 */

public interface ActivationServerListener<ResponseT extends Response, RequestT extends Request>
{
    void onActivatedAccount();
    void onActivationAwaiting(ApiResponse<ResponseT, RequestT> mDefaultResponse);
    void onActivationFailed(ApiResponse<ResponseT, RequestT> mDefaultResponse);

    void onNewPasswordSuccess();
    void onNewPasswordAwaiting(ApiResponse<ResponseT, RequestT> mDefaultResponse);
    void onNewPasswordFailed(ApiResponse<ResponseT, RequestT> mDefaultResponse);

    void onNewCertificateSuccess(ApiResponse<ResponseT, RequestT> mDefaultResponse);
    void onNewCertificateFailed(ApiResponse<ResponseT, RequestT> mDefaultResponse);
}
