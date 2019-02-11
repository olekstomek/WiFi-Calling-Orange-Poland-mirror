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

package pl.orangelabs.wificalling.view.activation_client;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.requests.DefaultActivationServerRequest;
import pl.orangelabs.wificalling.net.responses.DefaultActivationServerResponse;
import pl.orangelabs.wificalling.service.activation_service.ActivationServerListener;
import pl.orangelabs.wificalling.service.activation_service.ActivationState;
import pl.orangelabs.wificalling.service.activation_service.RepeatModeState;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.view.ActivityInit;

public class ActivityActivationSmsWaiter extends AppCompatActivity implements ActivationServerListener<DefaultActivationServerResponse,DefaultActivationServerRequest>
{
    private AlertDialog errorAlertDialog;

    /**
     *
     * @param context
     * @return
     */
    public static Intent getInstance(Context context)
    {
        Intent intent = new Intent(context, ActivityActivationSmsWaiter.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.anim_slide_in_up,
                R.anim.anim_slide_out_up);
        setContentView(R.layout.activity_activation_sms_waiter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        App.getActivationComponent().registerListener(this);
        if (App.getActivationComponent().getActivationState() == ActivationState.SMS_PASSWORD_WAITING)
        {
            App.getActivationComponent().generateNewPassword(false);
        }
        else if (App.getActivationComponent().getActivationState() != ActivationState.ACTIVE)
        {
            App.getActivationComponent().activateAccount(false);
        }
        else
        {
            startInitActivity();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        App.getActivationComponent().unRegisterListener(this);
    }

    private void onActivationFinished()
    {
        startInitActivity();
    }

    private void startInitActivity()
    {
        ActivityInit.startInitActivity(getApplicationContext());
    }

    @Override
    public void onActivatedAccount()
    {
        onActivationFinished();
    }

    @Override
    public void onActivationAwaiting(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
    }

    @Override
    public void onActivationFailed(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
        showErrorDialog(mDefaultResponse);
    }

    private void showErrorDialog(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
        if (mDefaultResponse.getObject().getActivationFailResponseCode() != null
                && !mDefaultResponse.getObject().getActivationFailResponseCode().isShowMessage())
        {
            return;
        }
        if (errorAlertDialog != null)
        {
            errorAlertDialog.dismiss();
        }
        errorAlertDialog = Utils.getRequestFailedBuilder(mDefaultResponse, ActivityActivationSmsWaiter.this, (dialog, which) ->
        {
            dialog.dismiss();
            if (App.getActivationComponent().getRepeatModeState() != RepeatModeState.ON)
            {
                App.getActivationComponent().clearActivation();
            }
        }).show();
    }


    @Override
    public void onNewPasswordSuccess()
    {
        startInitActivity();
    }

    @Override
    public void onNewPasswordAwaiting(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {

    }

    @Override
    public void onNewPasswordFailed(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
        showErrorDialog(mDefaultResponse);
    }

    @Override
    public void onNewCertificateSuccess(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {

    }

    @Override
    public void onNewCertificateFailed(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {

    }
}
