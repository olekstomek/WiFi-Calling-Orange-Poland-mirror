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

package pl.orangelabs.wificalling.model;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.service.activation_service.ActivationState;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.util.PermissionUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.view.helper.TermsAgreementDialog;

/**
 * Created by marcin on 25.05.17.
 */

public class PermissionList
{
    private List<PermissionItem> permissionItems = new ArrayList<>();

    public List<PermissionItem> getPermissionItems()
    {
        return permissionItems;
    }
    public void addAllPermissions()
    {
        getPermissionItems().add(audioPermission);
        getPermissionItems().add(callPermission);
        getPermissionItems().add(smsPermission);
        getPermissionItems().add(drivePermission);
        getPermissionItems().add(turnOnPermission);
        getPermissionItems().add(connectPermission);
        getPermissionItems().add(tAndCPermission);
        getPermissionItems().add(VPNPermission);
        getPermissionItems().add(contactsPermission);
    }
    public void addMissingPermisions(Context context)
    {
        if (!PermissionUtils.isPermissionGranted( Manifest.permission.RECORD_AUDIO, context))
        {
            getPermissionItems().add(audioPermission);
        }
        if (!PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)
                || !PermissionUtils.isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE, context))
        {
            getPermissionItems().add(drivePermission);
        }
        if (!PermissionUtils.isPermissionGranted( Manifest.permission.READ_CALL_LOG, context))
        {
            getPermissionItems().add(callPermission);
        }
        if (!PermissionUtils.isPermissionGranted( Manifest.permission.RECEIVE_SMS, context))
        {
            getPermissionItems().add(smsPermission);
        }
        if (!PermissionUtils.isPermissionGranted(Manifest.permission.READ_CONTACTS, context))
        {
            getPermissionItems().add(contactsPermission);
        }
        SharedPrefs mPrefs = new SharedPrefs(context);
        boolean isAgreementAccepted = mPrefs.Load(TermsAgreementDialog.AGREEMENT_ACCEPTED, false);
        if (!isAgreementAccepted)
        {
            getPermissionItems().add(tAndCPermission);
        }
        if (VpnService.prepare(context) != null)
        {
            getPermissionItems().add(VPNPermission);
        }
        if (App.getActivationComponent().getActivationState() == ActivationState.NON)
        {
            getPermissionItems().add(turnOnPermission);
        }
        if (!ConnectionService.isServiceON(context))
        {
            getPermissionItems().add(connectPermission);
        }
    }
    public static boolean isMissingPermission(Context context)
    {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        SharedPrefs mPrefs = new SharedPrefs(context);
        boolean isAgreementAccepted = mPrefs.Load(TermsAgreementDialog.AGREEMENT_ACCEPTED, false);
        if (!isAgreementAccepted)
        {
            return true;
        }
        if (VpnService.prepare(context) != null)
        {
            return true;
        }
        if (App.getActivationComponent().getActivationState() == ActivationState.NON)
        {
            return true;
        }
        return false;
    }

    private PermissionItem audioPermission = new PermissionItem(R.string.permission_name_audio, R.drawable.pop_6, R.string.permission_name_audio_desc);
    private PermissionItem callPermission = new PermissionItem(R.string.permission_name_calling, R.drawable.pop_5, R.string.permission_name_calling_desc);
    private PermissionItem smsPermission = new PermissionItem(R.string.permission_name_sms, R.drawable.pop_2, R.string.permission_name_sms_desc);
    private PermissionItem drivePermission = new PermissionItem(R.string.permission_name_drive, R.drawable.pop_3, R.string.permission_name_drive_desc);
    private PermissionItem turnOnPermission = new PermissionItem(R.string.permission_name_turn_on, R.drawable.pop_7, R.string.permission_name_turn_on_desc);
    private PermissionItem connectPermission = new PermissionItem(R.string.permission_name_connect, R.drawable.pop_8, R.string.permission_name_connect_desc);
    private PermissionItem tAndCPermission = new PermissionItem(R.string.permission_name_t_c, R.drawable.pop_9, R.string.permission_name_t_c_desc);
    private PermissionItem VPNPermission = new PermissionItem(R.string.permission_name_vpn, R.drawable.pop_1, R.string.permission_name_vpn_desc);
    private PermissionItem contactsPermission = new PermissionItem(R.string.permission_name_contacts, R.drawable.pop_4, R.string.permission_name_contacts_desc);

}
