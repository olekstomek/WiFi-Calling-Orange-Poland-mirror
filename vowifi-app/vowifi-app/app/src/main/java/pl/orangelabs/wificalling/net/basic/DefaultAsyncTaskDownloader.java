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

package pl.orangelabs.wificalling.net.basic;


import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.util.WifiUtils;

/**
 * Created by marcin on 29.11.16.
 */

public class DefaultAsyncTaskDownloader<ResponseT extends Response, RequestT extends Request> extends AsyncTask<Void,Void, ApiResponse<ResponseT,RequestT>>
{
    private RequestT mDefaultRequest = null;
    private Class<ResponseT> mResponseTClass = null;
    private JSONDownloader<ResponseT, RequestT> mDefaultDownloader = null;
    private AsyncTaskDownloaderListener<ResponseT,RequestT> mAsyncTaskDownloaderListener = null;
    private Context mContext = null;

    public DefaultAsyncTaskDownloader(@NonNull RequestT defaultRequest,@NonNull Class<ResponseT> responseTClass,
                                      @NonNull AsyncTaskDownloaderListener<ResponseT, RequestT> asyncTaskDownloaderListener,@NonNull Context context)
    {
        mDefaultRequest = defaultRequest;
        mResponseTClass = responseTClass;
        mDefaultDownloader = new JSONDownloader<>();
        mAsyncTaskDownloaderListener = asyncTaskDownloaderListener;
        mContext = context;
    }

    @Override
    protected ApiResponse<ResponseT,RequestT> doInBackground(Void... params)
    {
        ApiResponse<ResponseT, RequestT> mDefaultResponse;
        if (!WifiUtils.isInternetConnectionAvailable(mContext))
        {
            mDefaultResponse =  new ApiResponse<>(ApiResponse.ErrorType.NO_WIFI, null, null);
        }
        else
        {
            mDefaultRequest.init(mContext);
            mDefaultResponse = mDefaultDownloader.load(mContext, mDefaultRequest, mResponseTClass, DownloadMode.FORCE_DOWNLOAD);
        }
        Log.d(this, mDefaultResponse.toString());
        return mDefaultResponse;
    }

    @Override
    protected void onPostExecute(ApiResponse<ResponseT,RequestT> defaultResponseApiResponse)
    {
        super.onPostExecute(defaultResponseApiResponse);
        if (isRequestSuccess(defaultResponseApiResponse))
        {
            mAsyncTaskDownloaderListener.onRequestSuccess(defaultResponseApiResponse);
        }
        else
        {
            mAsyncTaskDownloaderListener.onRequestFailed(defaultResponseApiResponse);
        }
    }

    private boolean isRequestSuccess(ApiResponse<ResponseT, RequestT> defaultResponseApiResponse)
    {
        return defaultResponseApiResponse.isValid();
    }
}

