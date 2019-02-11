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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.service.receiver.VpnStateReceiver;
import pl.orangelabs.wificalling.sip.BroadcastSipReceiver;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * @author F
 */
public abstract class ActivityBase extends AppCompatActivity
{
    protected SharedPrefs mPrefs;

    protected BroadcastSipReceiver mBroadcastSipReceiver;
    protected VpnStateReceiver mVpnStateReceiver;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        overrideTransitionAnim();
        mPrefs = new SharedPrefs(this);


    }

    /**
     * @param topLevelActivity
     *     determines if this activity should display back arrow on toolbar (only useful on views that aren't on top level)
     * @param hasElevation
     *     removes default elevation from toolbar; needed in cases where toolbar needs to be aligned with other elements on z axis
     */
    public void initToolbar(final boolean topLevelActivity, final boolean hasElevation)
    {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null)
        {
            createTextViewDefaultTitle((TextView) toolbar.findViewById(R.id.toolbar_title));
            setSupportActionBar(toolbar);
            if (!topLevelActivity) // display back arrow
            {
                if (getSupportActionBar() != null)
                {
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
            if (!hasElevation)
            {
                ViewCompat.setElevation(toolbar, 0.0f);
            }
        }
    }

    public void initToolbar(final boolean topLevelActivity)
    {
        initToolbar(topLevelActivity, true);
    }

    @Override
    public void finish()
    {
        overrideTransitionAnim();
        super.finish();
    }

    protected void overrideTransitionAnim()
    {
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out);
    }

    protected void createTextViewDefaultTitle(final TextView tv)
    {
        final String text = getString(R.string.default_toolbar_title);
        final String coloredPart = getString(R.string.default_toolbar_title_colored_part);
        final SpannableString s = createColoredSpan(text, coloredPart);
        tv.setText(s);
    }
    protected void createTextViewEnterDefaultTitle(final TextView tv)
    {
        final String text = getString(R.string.default_toolbar_title) + " \n";
        final String coloredPart = getString(R.string.default_toolbar_title_colored_part);
        final SpannableString s = createColoredSpan(text, coloredPart);
        tv.setText(s);
    }

    @NonNull
    protected SpannableString createColoredSpan(final String text, final String coloredPart)
    {
        return createColoredSpan(text, coloredPart, R.color.textInvertedAccentedSpan);
    }

    @NonNull
    protected SpannableString createColoredSpan(final String text, final String coloredPart, final int colorResId)
    {
        final int coloredIndex = text.indexOf(coloredPart);
        final SpannableString s = new SpannableString(text);
        s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, colorResId)), coloredIndex,
            coloredIndex + coloredPart.length(), 0);
        return s;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void RegisterReceiver()
    {
        if (mBroadcastSipReceiver != null)
        {
            mBroadcastSipReceiver.RegisterSipReceiver(this);
        }
        if (mVpnStateReceiver != null)
        {
            mVpnStateReceiver.RegisterVpnStateReceiver(this);
        }
    }

    protected void UnRegisterReceiver()
    {
        if (mBroadcastSipReceiver != null)
        {
            mBroadcastSipReceiver.UnRegisterSipReceiver(this);
        }
        if (mVpnStateReceiver != null)
        {
            mVpnStateReceiver.UnRegisterVpnStateReceiver(this);
        }
    }
}
