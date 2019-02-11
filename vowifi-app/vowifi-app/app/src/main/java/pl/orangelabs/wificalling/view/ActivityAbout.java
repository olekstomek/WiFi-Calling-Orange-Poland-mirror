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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.view.helper.LicensesDialog;
import pl.orangelabs.wificalling.view.helper.TermsAgreementDialog;

/**
 * @author F
 */
public class ActivityAbout extends ActivityBase
{
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        final TextView titleView = (TextView) findViewById(R.id.activity_about_app_name);
        titleView.setText(createColoredSpan(titleView.getText().toString(), getString(R.string.app_name_colored_part), R.color.textAccented));
        final TextView versionView = (TextView) findViewById(R.id.activity_about_version);
        versionView.setText(getString(R.string.activity_about_version, Utils.AppVersion(this)));

        final View entryTos = findViewById(R.id.activity_about_entry_tos);
        entryTos.setOnClickListener(this::onEntryPressedTos);
        final View entryLicenses = findViewById(R.id.activity_about_entry_licenses);
        entryLicenses.setOnClickListener(this::onEntryPressedLicenses);

        initToolbar(false);
    }

    private void onEntryPressedLicenses(final View view)
    {
        new LicensesDialog().show(this);
    }

    private void onEntryPressedTos(final View view)
    {
        new TermsAgreementDialog().showTermsAndAgreementsDialog(this, null, true);
    }
}
