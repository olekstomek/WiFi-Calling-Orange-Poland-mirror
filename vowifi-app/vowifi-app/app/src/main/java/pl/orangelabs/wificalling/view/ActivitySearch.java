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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.WeakHashMap;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.ContactsSearchAdapter;
import pl.orangelabs.wificalling.content.NumberPickerDialogAdapter;
import pl.orangelabs.wificalling.content.VHBase;
import pl.orangelabs.wificalling.content.items.ContactItem;
import pl.orangelabs.wificalling.ctrl.MOFAB;
import pl.orangelabs.wificalling.ctrl.OLPKeyboard;
import pl.orangelabs.wificalling.ctrl.ParallaxRecyclerView;
import pl.orangelabs.wificalling.ctrl.RecyclerSeparatorDecoration;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.t9.T1000Dictionary;
import pl.orangelabs.wificalling.t9.T1000Entry;
import pl.orangelabs.wificalling.util.MemoryCache;
import pl.orangelabs.wificalling.view.helper.PhonePickerDialog;

/**
 * @author F
 */
public class ActivitySearch extends ActivityBase
{
    private static final String STATE_TEXT_INPUT = "ActivitySearch.text";
    private final WeakHashMap<String, List<ContactItem>> mSearchCache = new WeakHashMap<>(10);
    protected View mErrorFrame;
    protected TextView mErrorFrameMessage;
    private ContactsSearchAdapter mAdapter;
    private MOFAB mFab;
    private List<ContactItem> mFullDataSet;
    private String mPreviousTextInput = "";

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initToolbar(false);
        initErrorFrame();
        createTestRecyclerView();

        mFab = (MOFAB) findViewById(R.id.mo_fab);
        mFab.AddCallbacks(() -> Log.i(this, "Expanded"), new OnKeyboardCallback());

        mFab.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
        {
            @Override
            public void onLayoutChange(final View v, final int left, final int top, final int right, final int bottom, final int oldLeft,
                                       final int oldTop, final int oldRight, final int oldBottom)
            {
                mFab.removeOnLayoutChangeListener(this);
                // wait for transition to finish before animating sheet
                mFab.postDelayed(mFab::ShowSheet, getResources().getInteger(android.R.integer.config_shortAnimTime));
            }
        });
    }

    @Override
    protected void onPostResume()
    {
        super.onPostResume();
    }

    protected void initErrorFrame()
    {
        mErrorFrame = findViewById(R.id.misc_error_frame);
        findViewById(R.id.misc_error_frame_button).setVisibility(View.GONE);
        mErrorFrameMessage = (TextView) findViewById(R.id.misc_error_frame_message);
        mErrorFrame.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed()
    {
        if (mFab.IsSheetVisible())
        {
            mFab.HideSheet();
        }
        super.onBackPressed();
    }

    private void createTestRecyclerView()
    {
        final ParallaxRecyclerView recycler = (ParallaxRecyclerView) findViewById(R.id.activity_search_recycler);
        recycler.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE)
            {
                mFab.HideSheet();
            }
            return false;
        });

        final T1000Dictionary.T1000DataSet dataSet = MemoryCache.Me().Load(T1000Dictionary.T1000DataSet.class);

        mAdapter = new ContactsSearchAdapter(this, recycler, (tmp, type) -> Log.i(this, "Swipe " + type));
        final OnContactItemAction onContactItemAction = new OnContactItemAction();
        if (dataSet != null)
        {
            mFullDataSet = Stream.of(dataSet.mEntries).map(entry -> new ContactItem(entry, onContactItemAction)).collect(Collectors.toList());
            mAdapter.mDataSet.addAll(mFullDataSet);
        }

        recycler.setItemAnimator(new DefaultItemAnimator());
        recycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recycler.addItemDecoration(new RecyclerSeparatorDecoration(this));
        recycler.setAdapter(mAdapter);

        if (mFullDataSet == null || mFullDataSet.isEmpty())
        {
            mErrorFrame.setVisibility(View.VISIBLE);
            mErrorFrameMessage.setText(R.string.page_contacts_error_empty);
        }
    }

    private void call(final String number)
    {
        if (number != null && !number.isEmpty())
        {
            SipServiceCommand.makeCall(ActivitySearch.this, number);
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TEXT_INPUT, mPreviousTextInput);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        final String inputTextCache = savedInstanceState.getString(STATE_TEXT_INPUT);
        handleInputTextChange(inputTextCache);
    }

    private void handleInputTextChange(final String text)
    {
        final Stream<ContactItem> stream;
//        long perfStart = System.currentTimeMillis();
        if (mSearchCache.containsKey(text)) // we have this text cached, no need to filter it again
        {
            stream = Stream.of(mSearchCache.get(text));
        }
        else if (mPreviousTextInput.length() < text.length() && text.startsWith(mPreviousTextInput) && mSearchCache.containsKey(
            mPreviousTextInput))
        {
            // user added more characters to search field, filter the previous result, not the full dataset
            stream = Stream.of(mSearchCache.get(mPreviousTextInput));
        }
        else
        {
            if (mFullDataSet == null)
            {
                mPreviousTextInput = text;
                return;
            }
            stream = Stream.of(mFullDataSet);
            if (mPreviousTextInput.length() > text.length()) // backspace/clear -- remove previous cache entry as it's unlikely to be used
            {
                mSearchCache.remove(mPreviousTextInput);
            }
        }
        mAdapter.replaceDataset(stream
            .filter(v -> v.mEntry.matches(text))
            .sorted((v1, v2) -> v2.mEntry.mTimesContacted - v1.mEntry.mTimesContacted)
            .collect(Collectors.toList()));
        mSearchCache.put(text, mAdapter.mDataSet);
        mPreviousTextInput = text;
//        Log.i(this, "[perf] dataset filtering took " + (System.currentTimeMillis() - perfStart) + "ms");
    }

    private class OnContactItemAction implements ContactItem.IOnContactAction
    {
        @Override
        public void onPressed(final VHBase view, final ContactItem item)
        {
            T1000Entry entry = item.mEntry;
            if (!entry.mNumbers.isEmpty())
            {
                if (entry.mNumbers.size() == 1)
                {
                    SipServiceCommand.makeCall(ActivitySearch.this, entry.mNumbers.get(0).mNumber);
                }
                else
                {
                    PhonePickerDialog phonePickerDialog = new PhonePickerDialog(new OnPhoneItemAction());
                    phonePickerDialog.ShowDialAction(ActivitySearch.this, entry.mNumbers);
                }
            }
            else
            {
                Log.w(this, "empty phone list");
                // TODO: 2016-09-30 show some message
            }
        }
    }

    private class OnPhoneItemAction implements NumberPickerDialogAdapter.IOnPhoneAction
    {
        @Override
        public void onPressed(final String number)
        {
            SipServiceCommand.makeCall(ActivitySearch.this, number);
        }
    }

    private class OnKeyboardCallback implements OLPKeyboard.IOnKeyboardCallback
    {

        @Override
        public void onTextChanged(final String text)
        {
            handleInputTextChange(text);
        }

        @Override
        public void onCallPressed(final String text)
        {
            Log.i(this, "CALLING : " + text);
            call(text);
        }

        @Override
        public void onCloseRequested()
        {
            mFab.HideSheet();
        }
    }
}
