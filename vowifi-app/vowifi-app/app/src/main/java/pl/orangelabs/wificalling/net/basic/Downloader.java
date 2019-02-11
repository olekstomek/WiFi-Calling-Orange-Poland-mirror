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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.net.basic.MemoryManager.ObjectMemoryCache;
import pl.orangelabs.wificalling.util.Settings;

import static pl.orangelabs.wificalling.util.Settings.FAKE_REST_RESPONSES;


/**
 * @author F
 */
public abstract class Downloader<ResponseT extends Response, RequestT extends Request>
{
    protected static final String CACHE_KEY_START = "REQ-";

    protected final ResponseT downloadInternal(final Context ctx, final String url, @Nullable String hostName, String bodyParams, RequestMethod requestMethod, Class<ResponseT> responseTClass) throws IOException, IllegalAccessException, InstantiationException
    {
        ResponseT responseT;
        if (FAKE_REST_RESPONSES)
        {
            responseT = responseTClass.newInstance();
            final String s = resolveFakeData(ctx, url, responseT);
            if (s != null)
            {
                if (Settings.LOGGING_ENABLED)
                {
                    Log.i(this, "Resolved FAKE DATA: " + s.replaceAll("\\n|\\r", "").replaceAll("\\s+", " "));
                }
                responseT.setPlainTextResponse(s);
                responseT.setResponseCode(0);
            }
        }
        else
        {
            responseT = new UrlLoader<ResponseT>().getRawStringFromUrl(url, bodyParams, hostName, requestMethod, responseTClass);
        }
        if (Settings.LOGGING_ENABLED)
        {
            Log.v(this, "Server response: " + (responseT.getPlainTextResponse() == null ? "" : responseT.getPlainTextResponse().replaceAll("\\n|\\r", "").replaceAll("\\s+", " ")));
        }
        return responseT;
    }

    protected String resolveFakeData(final Context ctx, final String url, ResponseT responseT) throws IOException
    {
        return responseT.getFakeData();
    }

    @NonNull
    protected abstract ApiResponse<ResponseT,RequestT> parse(final ResponseT data, RequestT req);

    private boolean isCachedDataValid(ResponseT data)
    {
        return data.isCachedDataValid();
    }

    @NonNull
    private ApiResponse<ResponseT,RequestT> downloadAndSave(final Context ctx, final RequestT req, final Class<ResponseT> responseTClass)
    {
        try
        {
            DebugPerfCounter.Start(this);
            ResponseT responseT = downloadInternal(ctx, req.buildUrl(),
                    req.getHostName(), req.buildBodyParams(),req.getRequestMethod(), responseTClass);
            final ApiResponse<ResponseT,RequestT> parsedObject = parse(responseT, req);
            if (parsedObject.isValid() && canSaveToCache(req))
            {
               saveCache(ctx, req, responseT.getPlainTextResponse() , parsedObject.getObject());
            }
            DebugPerfCounter.End(this, "Downloading/parsing json response for " + req.buildUrl());
            parsedObject.setState(ApiResponse.ResponseState.FRESH);
            return parsedObject;
        }
        catch (final IOException e)
        {
            Log.d(this, "JSON retrieving error", e);
        } catch (InstantiationException | IllegalAccessException e)
        {
            Log.d(this, "Error", e);
        }
        return new ApiResponse<>(null, ApiResponse.ResponseState.FRESH, ApiResponse.ErrorType.IO, req);
    }

   protected void saveCache(final Context ctx, final RequestT req, final String data, final ResponseT parsedObject)
    {
        saveMemCache(req, parsedObject);
      //  saveFileCache(ctx, req, data);
    }

//    protected void saveFileCache(final Context ctx, final RequestT req, final String data)
//    {
//        File f = FileCache.Me().getFile(ctx, CACHE_KEY_START + Utils.md5(cacheKeyForRequest(req)));
//        RequestCacheManager jcm = new RequestCacheManager();
//        jcm.saveFile(data, f);
//    }

    protected void saveMemCache(final RequestT req, final ResponseT parsedObject)
    {
        ObjectMemoryCache.Me().Save(cacheKeyForRequest(req), parsedObject);
    }

    @NonNull
    protected final ApiResponse<ResponseT,RequestT> loadCache(final Context ctx, final RequestT req)
    {
        DebugPerfCounter.Start(this);
        final ResponseT memCached = loadMemCache(req);
        if (isCachedDataValid(memCached))
        {
            DebugPerfCounter.End(this, "Loading json for " + cacheKeyForRequest(req) + " (found in memcache)");
            return new ApiResponse<>(memCached, ApiResponse.ResponseState.CACHED, req);
        }

//        final String data = loadFileCache(ctx, req);
//        if (data == null)
//        {
            return new ApiResponse<>(req);
//        }

//        final ApiResponse<ResponseT> result = parse(data);
//        final boolean dataValid = isCachedDataValid(result.mObject, req);
//        if (dataValid)
//        {
//           saveMemCache(req, result.mObject);
//        }
//        DebugPerfCounter.End(this, "Loading cache/parsing json for " + cacheKeyForRequest(req));
//        result.mState = ApiResponse.ResponseState.CACHED;
//        return dataValid ? result : new ApiResponse<ResponseT>(null, ApiResponse.ResponseState.CACHED, null);
    }

    protected String cacheKeyForRequest(final RequestT req)
    {
        return req.buildUrl();
    }

    protected ResponseT loadMemCache(final RequestT req)
    {
        return ObjectMemoryCache.Me().Load(cacheKeyForRequest(req));
    }

  /* protected String loadFileCache(final Context ctx, final RequestT req)
    {
        File f = FileCache.Me().getFile(ctx, CACHE_KEY_START + Utils.md5(cacheKeyForRequest(req)));
        return new RequestCacheManager().decodeFile(f);
    }*/

    protected boolean canSaveToCache(final RequestT req)
    {
        return true;
    }

    public ApiResponse<ResponseT,RequestT> load(final Context ctx, final RequestT req, Class<ResponseT> responseTClass)
    {
        return load(ctx, req, responseTClass, DownloadMode.PREFER_CACHE);
    }

    @NonNull
    public ApiResponse<ResponseT,RequestT> load(final Context ctx, final RequestT req, Class<ResponseT> responseTClass, final DownloadMode downloadMode)
    {
        ApiResponse.ErrorType downloadErrorType = null;
        if (downloadMode == DownloadMode.FORCE_DOWNLOAD || downloadMode == DownloadMode.PREFER_DOWNLOAD)
        {
            final ApiResponse<ResponseT,RequestT> downloadedData = downloadAndSave(ctx, req, responseTClass);
            downloadErrorType = downloadedData.getErrorType();
            if (downloadedData.isValid() || downloadMode == DownloadMode.FORCE_DOWNLOAD)
            {
                if (!downloadedData.isValid())
                {
                    Log.i(this, "FORCE_DOWNLOAD load failed: requested data couldn't be downloaded");
                }
                return downloadedData;
            }
        }

        ApiResponse<ResponseT, RequestT> cachedData = loadCache(ctx, req);
        if (!cachedData.isValid())
        {
            if (downloadMode == DownloadMode.FORCE_CACHE || downloadMode == DownloadMode.PREFER_DOWNLOAD)
            {
                if (downloadMode == DownloadMode.FORCE_CACHE)
                {
                    Log.i(this, "FORCE_CACHE load failed: requested data not found in cache");
                }
                else
                {
                    Log.i(this, "PREFER_DOWNLOAD load failed: requested data couldn't be downloaded and not found in cache");
                }
                cachedData.setErrorType(downloadErrorType); // restore error from original download if possible
                return cachedData;
            }
            Log.v(this, "Not found in cache: " + req);
            return downloadAndSave(ctx, req, responseTClass);
        }

        Log.v(this, "Loaded from cache: " + req);
        return cachedData;
    }
}
