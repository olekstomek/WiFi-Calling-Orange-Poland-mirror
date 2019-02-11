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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.basic.AsyncTaskDownloaderListener;
import pl.orangelabs.wificalling.net.basic.DefaultAsyncTaskDownloader;
import pl.orangelabs.wificalling.net.requests.NewCertificateRequest;
import pl.orangelabs.wificalling.net.requests.NewPasswordRequest;
import pl.orangelabs.wificalling.net.requests.ProvisioningRequest;
import pl.orangelabs.wificalling.net.responses.DefaultActivationServerResponse;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.util.Settings;
import pl.orangelabs.wificalling.util.Utils;

import static pl.orangelabs.wificalling.service.activation_service.GCMRepeatAuthService.ALIAS;

/**
 * Created by marcin on 25.01.17.
 */

public class ActivationComponentDefault implements ActivationComponent
{
    private final List<WeakReference<ActivationServerListener>> listActivationServerListeners;

    private ActivationDataKeeper activationDataKeeper = null;
    private Context mContext;
    private boolean hasSMSCame = false;
    private RepeatStack repeatStack = null;
    private RepeatModeState repeatModeState = RepeatModeState.OFF;


    public ActivationComponentDefault(Context context)
    {
        mContext = context;
        listActivationServerListeners = Collections.synchronizedList(new LinkedList<>());
        activationDataKeeper = new ActivationDataKeeperDefault(mContext);
    }

    private static ActivationComponent activationComponent = null;

    public static ActivationComponent getInstance(@Nullable Context context)
    {
        if (activationComponent == null)
        {
            if (context == null)
            {
                return null;
            }
            activationComponent = new ActivationComponentDefault(context);
        }
        return activationComponent;
    }

    private ActivationState activationState = null;

    public ActivationState getActivationState()
    {
        if (activationState == null)
        {
            activationState = activationDataKeeper.loadState();
        }
        return activationState;
    }

    @Override
    public boolean isActive()
    {
        return getActivationState() == ActivationState.ACTIVE || getActivationState() == ActivationState.CERT_CHANGING;
    }

    @Override
    public RepeatModeState getRepeatModeState()
    {
        return repeatModeState;
    }

    @Override
    public void clearActivation()
    {
        setActivationState(ActivationState.NON);
        getActivationDataKeeper().clearActivation();
        ConnectionService.turnServiceOFF(mContext);
    }

    public void setActivationState(ActivationState activationState)
    {
        this.activationState = activationState;
        activationDataKeeper.saveState(activationState);
    }

    @Override
    public ActivationDataKeeper getActivationDataKeeper()
    {
        return activationDataKeeper;
    }

    @Override
    public void registerListener(ActivationServerListener activationServerListener)
    {
        if (activationServerListener != null)
        {
            synchronized (listActivationServerListeners)
            {
                listActivationServerListeners.add(new WeakReference<>(activationServerListener));
            }
        }
    }

    @Override
    public void unRegisterListener(ActivationServerListener activationServerListener)
    {
        if (activationServerListener != null)
        {
            synchronized (listActivationServerListeners)
            {
                for (Iterator<WeakReference<ActivationServerListener>> iterator = listActivationServerListeners.iterator();
                     iterator.hasNext(); )
                {
                    WeakReference<ActivationServerListener> weakRef = iterator.next();
                    if (weakRef.get() == activationServerListener)
                    {
                        iterator.remove();
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void activateAccount(boolean renew)
    {
        if (!hasBeenActivated(mContext))
        {
            if (getActivationState() != ActivationState.SMS_ACTIVATION_WAITING
                    && getActivationState() != ActivationState.STARTING || renew)
            {
                resetActivation();
                DefaultAsyncTaskDownloader<DefaultActivationServerResponse, ProvisioningRequest> defaultAsyncTaskDownloader
                        = new DefaultAsyncTaskDownloader<>(new ProvisioningRequest(), DefaultActivationServerResponse.class ,
                        new AsyncTaskDownloaderListener<DefaultActivationServerResponse, ProvisioningRequest>()
                {
                    @Override
                    public void onRequestSuccess(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
                    {
                        if (mDefaultResponse.getObject().getResponseCode() == 0)
                        {
                            if (mDefaultResponse.getObject().getActivationFailResponseCode() != null
                                    || mDefaultResponse.getObject().getCert() == null)
                            {
                                handleActivationError(mDefaultResponse);
                            }
                            else
                            {
                                new HandleRequestAsyncTask().execute(mDefaultResponse);
                            }
                        }
                        else
                        {
                            handleActivationError(mDefaultResponse);
                        }
                    }

                    @Override
                    public void onRequestFailed(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
                    {
                        broadcastAccountActivationFailed(mDefaultResponse);
                    }
                }, mContext);
                defaultAsyncTaskDownloader.execute();
            }
        }
        else
        {
            performAccountActivated();
        }
    }

    public void setRepeatModeState(RepeatModeState repeatModeState)
    {
        this.repeatModeState = repeatModeState;
    }

    private class HandleRequestAsyncTask extends AsyncTask<ApiResponse<DefaultActivationServerResponse,ProvisioningRequest>,Void, ApiResponse<DefaultActivationServerResponse,ProvisioningRequest>>
    {

        @Override
        protected ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> doInBackground(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest>... mDefaultResponse)
        {
            activationDataKeeper.saveImsiConnectedWithPassword(Utils.getUserImsi(mContext));
            KeystoreHandling.createKeystoreCert(mContext, mDefaultResponse[0].getObject().getCert(),
                     mDefaultResponse[0].mDefaultRequest.getPrivateKey());
            activationDataKeeper.saveCertificateExpirationDate();
            return mDefaultResponse[0];
        }

        @Override
        protected void onPostExecute(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
        {
            super.onPostExecute(mDefaultResponse);
            setActivationState(ActivationState.SMS_ACTIVATION_WAITING);
            broadcastAccountActivateAwaiting(mDefaultResponse);
            if (hasSMSCame)
            {
                performAccountActivated();
            }
            else
            {
                startRepeatRequest();
            }
        }
    }
    private void handleActivationError(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
    {
        if (mDefaultResponse.getObject() != null && mDefaultResponse.getObject().getActivationFailResponseCode() != null &&
                mDefaultResponse.getObject().getActivationFailResponseCode().isRepeatError())
        {
            startRepeatRequest();
        }
        setActivationState(ActivationState.ACTIVATION_ERROR);
        broadcastAccountActivationFailed(mDefaultResponse);
    }

    private void performAccountActivated()
    {
        cancelRepeatRequest(RepeatModeState.OFF);
        setActivationState(ActivationState.ACTIVE);
        broadcastMethodActivated();
    }
    private void performNewPassword()
    {
        setActivationState(ActivationState.ACTIVE);
        broadcastNewPassword();
    }

    private void resetActivation()
    {
        activationDataKeeper.saveActivationDate(new Date().getTime());
        setActivationState(ActivationState.STARTING);
        hasSMSCame = false;
    }

    ApiResponse<DefaultActivationServerResponse,NewPasswordRequest> newPasswordResponse;
    @Override
    public void generateNewPassword(boolean renew)
    {
        if (!hasBeenActivated(mContext) || renew)
        {
            if (getActivationState() != ActivationState.SMS_PASSWORD_WAITING || renew)
            {
                resetActivation();
                DefaultAsyncTaskDownloader<DefaultActivationServerResponse, NewPasswordRequest> defaultAsyncTaskDownloader = new DefaultAsyncTaskDownloader<>
                        (new NewPasswordRequest(), DefaultActivationServerResponse.class,
                                new AsyncTaskDownloaderListener<DefaultActivationServerResponse,NewPasswordRequest>()
                {
                    @Override
                    public void onRequestSuccess(ApiResponse<DefaultActivationServerResponse,NewPasswordRequest> mDefaultResponse)
                    {
                        if (mDefaultResponse.getObject().getResponseCode() == 0)
                        {
                            newPasswordResponse = mDefaultResponse;
                            setActivationState(ActivationState.SMS_PASSWORD_WAITING);
                            broadcastNewPasswordAwaiting(mDefaultResponse);
                            if (hasSMSCame)
                            {
                                performNewPassword();
                            }
                        }
                        else
                        {
                            handleErrorNewPassword(mDefaultResponse);
                        }
                    }

                    @Override
                    public void onRequestFailed(ApiResponse<DefaultActivationServerResponse,NewPasswordRequest> mDefaultResponse)
                    {
                        handleErrorNewPassword(mDefaultResponse);
                    }
                    private void handleErrorNewPassword(ApiResponse<DefaultActivationServerResponse, NewPasswordRequest> mDefaultResponse)
                    {
                        setActivationState(ActivationState.ACTIVE);
                        broadcastNewPasswordFailed(mDefaultResponse);
                    }

                }, mContext);
                defaultAsyncTaskDownloader.execute();
            }
            else
            {
                broadcastNewPasswordAwaiting(newPasswordResponse);
            }
        }
        else
        {
            performNewPassword();
        }
    }

    private ApiResponse<DefaultActivationServerResponse,NewCertificateRequest> newCertificateResponse = null;
    @Override
    public void generateNewCertificate(boolean renew)
    {
        if (newCertificateResponse == null || renew)
        {
            newCertificateResponse = null;
            if (getActivationState() != ActivationState.CERT_CHANGING)
            {
                setActivationState(ActivationState.CERT_CHANGING);
                DefaultAsyncTaskDownloader<DefaultActivationServerResponse, NewCertificateRequest> defaultAsyncTaskDownloader
                        = new DefaultAsyncTaskDownloader<>(new NewCertificateRequest(),
                        DefaultActivationServerResponse.class, new AsyncTaskDownloaderListener<DefaultActivationServerResponse,
                        NewCertificateRequest>()
                {
                    @Override
                    public void onRequestSuccess(ApiResponse<DefaultActivationServerResponse, NewCertificateRequest> mDefaultResponse)
                    {
                        if (mDefaultResponse.getObject().getResponseCode() == 0)
                        {
                            KeystoreHandling.createKeystoreCert(mContext, mDefaultResponse.getObject().getCert(),
                                    mDefaultResponse.mDefaultRequest.getPrivateKey());
                            newCertificateResponse = mDefaultResponse;
                            activationDataKeeper.saveCertificateExpirationDate();
                            setActivationState(ActivationState.ACTIVE);
                            broadcastNewCertificate(mDefaultResponse);
                        }
                        else
                        {
                            setActivationState(ActivationState.ACTIVATION_ERROR);
                            broadcastNewCertificateFailed(mDefaultResponse);
                        }
                    }

                    @Override
                    public void onRequestFailed(ApiResponse<DefaultActivationServerResponse, NewCertificateRequest> mDefaultResponse)
                    {
                        broadcastNewCertificateFailed(mDefaultResponse);
                    }
                }, mContext);
                defaultAsyncTaskDownloader.execute();
            }
        }
        else
        {
            broadcastNewCertificate(newCertificateResponse);
        }
    }

    public void startRepeatRequest()
    {
        if (getRepeatModeState() == RepeatModeState.OFF)
        {
            setRepeatModeState(RepeatModeState.ON);
            repeatStack = new RepeatStack();
            makeRepeatRequest();
        }
    }

    @Override
    public RepeatStack getRepeatStack()
    {
        if(repeatStack == null)
        {
            repeatStack = new RepeatStack();
        }
        return repeatStack;
    }

    @Override
    public void makeRepeatRequest()
    {
        Log.d(this, "makeRepeatRequest()");
        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(mContext);
        PeriodicTask task = new PeriodicTask.Builder()
                .setService(GCMRepeatAuthService.class)
                .setTag(ALIAS)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setPeriod(repeatStack.getTime())
                .setFlex(10L)
                .setRequiresCharging(false)
                .setUpdateCurrent(true)
                .build();
        mGcmNetworkManager.schedule(task);
    }

    @Override
    public void cancelRepeatRequest(RepeatModeState repeatModeState)
    {
        Log.d(this, "cancelRepeatRequest()");
        setRepeatModeState(repeatModeState);
        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(mContext);
        mGcmNetworkManager.cancelTask(ALIAS, GCMRepeatAuthService.class);
    }

    @Override
    public void onSMSHasCame(String body)
    {
        new HandleSMSAsyncTask().execute(body);
    }
    private class HandleSMSAsyncTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... smsBody)
        {
            parseAndSaveSMS(smsBody[0], mContext);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            hasSMSCame = true;
            if (getActivationState() == ActivationState.SMS_ACTIVATION_WAITING)
            {
                performAccountActivated();
            }
            else if(getActivationState() == ActivationState.SMS_PASSWORD_WAITING)
            {
                performNewPassword();
            }
        }
    }
    private static int USER_NUMBER_INDEX = 0;
    private static int SIP_INDEX = USER_NUMBER_INDEX + 1;
    private static int VPN_INDEX = SIP_INDEX + 1;

    private void parseAndSaveSMS(String msgBody, Context context)
    {
        String[] sms = msgBody.split(";");
        String
                userNumber =
                "+" + sms[USER_NUMBER_INDEX].substring(sms[USER_NUMBER_INDEX].indexOf(":") + 1, sms[USER_NUMBER_INDEX].length());
        String passwordSIP = sms[SIP_INDEX].substring(0, sms[SIP_INDEX].length());
        String passwordVPN = sms[VPN_INDEX].substring(0, sms[VPN_INDEX].length());
        activationDataKeeper.saveVPNPassword(passwordVPN);
        activationDataKeeper.saveSIPPassword(passwordSIP);
        activationDataKeeper.saveUsersNumber(userNumber);
    }
    private boolean hasBeenActivated(Context context)
    {
        if (getActivationState() == ActivationState.ACTIVE)
        {
            return true;
        }
        else if (getActivationState() == ActivationState.SMS_ACTIVATION_WAITING)
        {
            Cursor cursor = null;
            try
            {
                cursor = context.getContentResolver().query(Uri.parse("content://sms/inbox"),
                        new String[]{"date", "body"}, "address = ? AND date > ?",
                        new String[]{Settings.SMS_AUTHOR, "" + activationDataKeeper.loadActivationDate()}, null);
                if (cursor != null && cursor.getCount() > 0)
                {
                    cursor.moveToFirst();
                    parseAndSaveSMS(cursor.getString(cursor.getColumnIndex("body")), mContext);
                    return true;
                }
                else
                {
                    return false;
                }
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
            }
        }
        return false;
    }

    private void broadcastMethodActivated()
    {
        broadcastMethod(object -> object.onActivatedAccount());
    }
    private void broadcastAccountActivateAwaiting(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onActivationAwaiting(mDefaultResponse));
    }
    private void broadcastAccountActivationFailed(ApiResponse<DefaultActivationServerResponse,ProvisioningRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onActivationFailed(mDefaultResponse));
    }
    private void broadcastNewPassword()
    {
        broadcastMethod(object -> object.onNewPasswordSuccess());
    }
    private void broadcastNewPasswordAwaiting(ApiResponse<DefaultActivationServerResponse,NewPasswordRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onNewPasswordAwaiting(mDefaultResponse));
    }
    private void broadcastNewPasswordFailed(ApiResponse<DefaultActivationServerResponse, NewPasswordRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onNewPasswordFailed(mDefaultResponse));
    }
    private void broadcastNewCertificate(ApiResponse<DefaultActivationServerResponse, NewCertificateRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onNewCertificateSuccess(mDefaultResponse));
    }
    private void broadcastNewCertificateFailed(ApiResponse<DefaultActivationServerResponse, NewCertificateRequest> mDefaultResponse)
    {
        broadcastMethod(object -> object.onNewCertificateFailed(mDefaultResponse));
    }

    private void broadcastMethod(BroadCastMethod<ActivationServerListener> broadCastMethod)
    {
        for (Iterator<WeakReference<ActivationServerListener>> iterator = listActivationServerListeners.iterator();
             iterator.hasNext();)
        {
            WeakReference<ActivationServerListener> weakRef = iterator.next();
            if (weakRef.get() != null)
            {
                broadCastMethod.broadcastMethod(weakRef.get());
            }
        }
    }

    public interface BroadCastMethod<Object>
    {
        void broadcastMethod(Object object);
    }
}
