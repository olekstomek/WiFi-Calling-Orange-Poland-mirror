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

/**
 * @author F
 */
public class ApiResponse<ResponseT extends Response, RequestT extends Request>
{
    private ResponseT mObject;
    private ResponseState mState;
    private ErrorType mErrorType;
    private Exception mException;

    public RequestT mDefaultRequest;


    public ApiResponse(RequestT defaultRequest)
    {
        this((ResponseT) null,defaultRequest);
    }

    public ApiResponse(final ResponseT object, RequestT defaultRequest)
    {
        this(object, ResponseState.FRESH, defaultRequest);
    }

    public ApiResponse(final ErrorType errorType,RequestT defaultRequest)
    {
        this(null, ResponseState.FRESH, errorType,defaultRequest);
    }

    public ApiResponse(final ErrorType errorType, Exception exception, RequestT defaultRequest)
    {
        this(null, ResponseState.FRESH, errorType, exception, defaultRequest);
    }


    public ApiResponse(final ResponseT object, final ResponseState state, RequestT defaultRequest)
    {
        this(object, state, null, defaultRequest);
    }


    public ApiResponse(final ResponseT object, final ResponseState state, final ErrorType errorType, RequestT defaultRequest)
    {
        mObject = object;
        mState = state;
        mErrorType = errorType;
        mDefaultRequest = defaultRequest;
    }
    public ApiResponse(final ResponseT object, final ResponseState state, final ErrorType errorType, Exception exception, RequestT defaultRequest)
    {
        mObject = object;
        mState = state;
        mErrorType = errorType;
        mException = exception;
        mDefaultRequest = defaultRequest;
    }

    public boolean isValid()
    {
        return getException() == null && getObject() != null && getObject().isDataValid();
    }

    @Override
    public String toString()
    {
        String exception = getException() != null ? getException().getMessage() : "";
        return String.format("api response:[obj:%s, state:%s, errorType:%s, error:%s]", getObject() != null ? getObject().toString() : "", getState(), getErrorType(), exception);
    }

    public enum ResponseState
    {
        CACHED,
        FRESH
    }

    public enum ErrorType
    {
        IO,
        PARSING,
        FORMAT,
        NO_WIFI
    }
    public ResponseT getObject()
    {
        return mObject;
    }

    public void setmObject(ResponseT mObject)
    {
        this.mObject = mObject;
    }

    public ResponseState getState()
    {
        return mState;
    }

    public void setState(ResponseState mState)
    {
        this.mState = mState;
    }

    public ErrorType getErrorType()
    {
        return mErrorType;
    }

    public void setErrorType(ErrorType mErrorType)
    {
        this.mErrorType = mErrorType;
    }

    public Exception getException()
    {
        return mException;
    }

    public void setException(Exception mException)
    {
        this.mException = mException;
    }

    public RequestT getDefaultRequest()
    {
        return mDefaultRequest;
    }

    public void setDefaultRequest(RequestT mDefaultRequest)
    {
        this.mDefaultRequest = mDefaultRequest;
    }
}
