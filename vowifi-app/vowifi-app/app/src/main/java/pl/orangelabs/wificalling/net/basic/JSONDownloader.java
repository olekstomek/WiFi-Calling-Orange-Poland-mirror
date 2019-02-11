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

import android.support.annotation.NonNull;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;

import pl.orangelabs.log.Log;


/**
 * @author F
 */
public class JSONDownloader<ResponseT extends Response, RequestT extends Request> extends Downloader<ResponseT, RequestT>
{
    protected JSONDownloader()
    {
    }

    @NonNull
    protected ApiResponse<ResponseT, RequestT> parseInternal(ResponseT data, RequestT req) throws JSONException, UnsupportedEncodingException
    {
        if (data.getResponseCode() < 300)
        {
            data.parse();
        }
        return new ApiResponse<>(data, req);
    }

    @NonNull
    @Override
    protected ApiResponse<ResponseT, RequestT> parse(final ResponseT data, RequestT req)
    {
        try
        {
            return parseInternal(data, req);
        }
        catch (JSONException e)
        {
            Log.w(this, "JSON parsing error", e);
            return new ApiResponse<>(ApiResponse.ErrorType.PARSING, req);
        } catch (UnsupportedEncodingException e)
        {
            Log.w(this, "parsing error", e);
            return new ApiResponse<>(ApiResponse.ErrorType.PARSING, req);
        }
    }
}
