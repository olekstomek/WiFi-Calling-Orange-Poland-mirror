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

package pl.orangelabs.wificalling.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;

import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.requests.DefaultActivationServerRequest;
import pl.orangelabs.wificalling.net.responses.DefaultActivationServerResponse;
import pl.orangelabs.wificalling.service.activation_service.ActivationServerListener;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.view.activation_client.ActivityActivationSmsWaiter;

/**
 * @author F
 */
public class ActivitySettings extends ActivityBase implements ActivationServerListener<DefaultActivationServerResponse,DefaultActivationServerRequest>
{
    public static final int RESULT_CODE_SETTINGS_CHANGED = 1;
    private static final int TAG_SWITCH_TYPE = R.id.settings_entry_switch_type_tag;
    private final List<SettingsValuesHelper> mValuesHelper = new ArrayList<>();
    private AlertDialog errorAlertDialog;
    private AlertDialog progressDialog;
    private boolean isChangingPassword = false;

    private static boolean prefsDefaultValue(final String key)
    {
        switch (key)
        {
            default:
                return false;
            case SharedPrefs.KEY_SETTINGS_CONTACT_DISPLAY_MODE:
                return SharedPrefs.Defaults.CONTACT_DISPLAY_MODE.ordinal() > 0;
            case SharedPrefs.KEY_SETTINGS_CONTACT_SORTING_MODE:
                return SharedPrefs.Defaults.CONTACT_SORTING_MODE.ordinal() > 0;
            case SharedPrefs.KEY_SETTINGS_DIVIDER_MODE:
                return SharedPrefs.Defaults.DIVIDER_MODE.ordinal() > 0;
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // TODO we might need to handle a special case where we need 2x elevation in orange version and 1x in nju (not very important)
        initToolbar(false);

        final View disableButton = findViewById(R.id.activity_settings_change_password);
        disableButton.setOnClickListener(v -> {
            changePassword();
        });

//        mValuesHelper.add(new SettingsValuesHelper(R.id.activity_settings_entry_divider_mode,
//            new SettingsValueStateInfo(R.string.activity_settings_divider_mode_on, R.drawable.ic_settings_divider),
//            new SettingsValueStateInfo(R.string.activity_settings_divider_mode_off, R.drawable.ic_settings_divider),
//            SharedPrefs.KEY_SETTINGS_DIVIDER_MODE));
        mValuesHelper.add(new SettingsValuesHelper(R.id.activity_settings_entry_sorting_mode,
            new SettingsValueStateInfo(R.string.activity_settings_sorting_mode_on, R.drawable.ic_settings_sort1),
            new SettingsValueStateInfo(R.string.activity_settings_sorting_mode_off, R.drawable.ic_settings_sort2),
            SharedPrefs.KEY_SETTINGS_CONTACT_SORTING_MODE));
        mValuesHelper.add(new SettingsValuesHelper(R.id.activity_settings_entry_display_mode,
            new SettingsValueStateInfo(R.string.activity_settings_display_mode_on, R.drawable.ic_settings_sort1),
            new SettingsValueStateInfo(R.string.activity_settings_display_mode_off, R.drawable.ic_settings_sort2),
            SharedPrefs.KEY_SETTINGS_CONTACT_DISPLAY_MODE));
//        mValuesHelper.add(new SettingsValuesHelper(R.id.activity_settings_entry_ringtone,
//            new SettingsValueStateInfo(R.string.activity_settings_ringtone, R.drawable.ic_settings_ringtone),
//            null, null));

        Stream.of(mValuesHelper).forEach(this::initSettingsEntry);
    }

    private void changePassword()
    {
        isChangingPassword = true;
        showProgressDialog();
        App.getActivationComponent().generateNewPassword(true);
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
        App.getActivationComponent().registerListener(this);
        if (isChangingPassword)
        {
            App.getActivationComponent().generateNewPassword(false);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        App.getActivationComponent().unRegisterListener(this);
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        UnRegisterReceiver();
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
        if (errorAlertDialog != null)
        {
            errorAlertDialog.dismiss();
        }
    }

    private void initSettingsEntry(final SettingsValuesHelper valuesHelper)
    {
        final View frame = findViewById(valuesHelper.mFrameId);
        final TextView entryText = (TextView) frame.findViewById(R.id.inc_settings_entry_text);
        final boolean checked = mPrefs.Load(valuesHelper.mPrefsKey, valuesHelper.mDefaultValue);
        entryText.setText(checked ? valuesHelper.mInfoOn.mTextResId : valuesHelper.mInfoOff.mTextResId);
        entryText.setTag(TAG_SWITCH_TYPE, valuesHelper.mPrefsKey);

        final ImageView entryIcon = (ImageView) frame.findViewById(R.id.inc_settings_entry_icon);
        entryIcon.setImageResource(checked ? valuesHelper.mInfoOn.mIconResId : valuesHelper.mInfoOff.mIconResId);

        frame.setOnClickListener(v -> onSettingsEntryPressed(valuesHelper, entryText, entryIcon));
    }

    private void onSettingsEntryPressed(final SettingsValuesHelper val, final TextView entryText, final ImageView entryIcon)
    {
        if (val.mPrefsKey == null) // non-switchable setting
        {
            if (val.mFrameId == R.id.activity_settings_entry_ringtone)
            {
                Snackbar.make(findViewById(R.id.data_root), "TODO wybór dzwonka", Snackbar.LENGTH_SHORT).show();
            }
            return;
        }
        final boolean prevValue = mPrefs.Load(val.mPrefsKey, val.mDefaultValue);
        mPrefs.Save(val.mPrefsKey, !prevValue);
        entryText.setText(prevValue ? val.mInfoOff.mTextResId : val.mInfoOn.mTextResId);
        entryIcon.setImageResource(prevValue ? val.mInfoOff.mIconResId : val.mInfoOn.mIconResId);
        setResult(RESULT_CODE_SETTINGS_CHANGED); // TODO do we want to keep track of settings to actually know if something changed?
    }

    private static final class SettingsValuesHelper
    {
        private final int mFrameId;
        private final SettingsValueStateInfo mInfoOn;
        private final SettingsValueStateInfo mInfoOff;
        private final boolean mDefaultValue;
        private final String mPrefsKey;

        private SettingsValuesHelper(final int frameId, final SettingsValueStateInfo infoOn, final SettingsValueStateInfo infoOff,
                                     final String prefsKey)
        {
            mFrameId = frameId;
            mInfoOn = infoOn;
            mInfoOff = infoOff;
            mDefaultValue = prefsKey == null || prefsDefaultValue(prefsKey);
            mPrefsKey = prefsKey;
        }
    }

    private static final class SettingsValueStateInfo
    {
        private final int mTextResId;
        private final int mIconResId;

        private SettingsValueStateInfo(final int textResId, final int iconResId)
        {
            mTextResId = textResId;
            mIconResId = iconResId;
        }
    }
    @Override
    public void onActivatedAccount()
    {

    }

    @Override
    public void onActivationAwaiting(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {

    }

    @Override
    public void onActivationFailed(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {

    }

    @Override
    public void onNewPasswordSuccess()
    {
        ActivityInit.startInitActivity(getApplicationContext());
    }

    @Override
    public void onNewPasswordAwaiting(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
        dismissProgressDialog();
        ConnectionService.turnServiceOFF(getApplicationContext());
    }

    @Override
    public void onNewPasswordFailed(ApiResponse<DefaultActivationServerResponse,DefaultActivationServerRequest> mDefaultResponse)
    {
        isChangingPassword = false;
        dismissProgressDialog();
        Utils.getRequestFailedBuilder(mDefaultResponse, ActivitySettings.this, (dialog, which) ->
        {
            dialog.dismiss();
        }).show();
    }
    private void showProgressDialog()
    {
        if (progressDialog == null)
        {
            progressDialog = Utils.getProgressDialog(ActivitySettings.this, R.string.connecting);
        }
        progressDialog.show();
    }
    private void dismissProgressDialog()
    {
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
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
