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

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.ctrl.ParallaxRecyclerView;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.t9.T1000Dictionary;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * @author F
 */
public class PageBase extends LinearLayout
{
    protected SharedPrefs mPrefs;
    protected IParentCallback mParentCallback;
    protected View mErrorFrame;
    protected TextView mErrorFrameMessage;
    protected View mErrorFrameButton;
    protected View mProgress;
    protected ParallaxRecyclerView mRecycler;

    public PageBase(final Context context)
    {
        super(context);
        init(context);
    }

    public PageBase(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public PageBase(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context)
    {
        mPrefs = new SharedPrefs(context);
    }

    public PageBase withCallback(final IParentCallback parentCallback)
    {
        mParentCallback = parentCallback;
        return this;
    }

    public void handleCTLScroll(final int offset)
    {

    }


    public void onContactsDataChanged(@Nullable final T1000Dictionary.T1000DataSet entries)
    {

    }
    public void onCallLogsDataChanged(final List<VWCallLog> callLogList){

    }

    protected void initErrorFrame(final View inflated)
    {
        mErrorFrame = inflated.findViewById(R.id.misc_error_frame);
        mErrorFrameButton = inflated.findViewById(R.id.misc_error_frame_button);
        mErrorFrameMessage = (TextView) inflated.findViewById(R.id.misc_error_frame_message);
        mErrorFrame.setVisibility(GONE);
        mErrorFrameButton.setOnClickListener(this::reloadData);
    }

    protected void handleLoadingError(final int messageResId, final boolean showReloadButton)
    {
        if (mRecycler != null)
        {
            mRecycler.setVisibility(GONE);
        }
        if (mProgress != null)
        {
            mProgress.setVisibility(GONE);
        }
        mErrorFrame.setVisibility(VISIBLE);
        mErrorFrameMessage.setText(messageResId);
        mErrorFrameButton.setVisibility(showReloadButton ? VISIBLE : GONE);
    }

    protected void reloadData(final View view)
    {

    }

    public interface IParentCallback
    {
        void requestReloadContacts();
    }
}
