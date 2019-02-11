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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.BaseSwipeableAdapter;
import pl.orangelabs.wificalling.content.ContactsPageAdapter;
import pl.orangelabs.wificalling.content.NumberPickerDialogAdapter;
import pl.orangelabs.wificalling.content.RecyclerItemBase;
import pl.orangelabs.wificalling.content.VHBase;
import pl.orangelabs.wificalling.content.holders.ContactsVH;
import pl.orangelabs.wificalling.content.items.ContactItem;
import pl.orangelabs.wificalling.content.items.DividerItem;
import pl.orangelabs.wificalling.ctrl.ParallaxRecyclerView;
import pl.orangelabs.wificalling.ctrl.RecyclerSeparatorDecoration;
import pl.orangelabs.wificalling.model.Dividers;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.t9.T1000Dictionary;
import pl.orangelabs.wificalling.t9.T1000Entry;
import pl.orangelabs.wificalling.util.MemoryCache;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.Utils;
import pl.orangelabs.wificalling.view.helper.PhonePickerDialog;


/**
 * @author F
 */
public class PageContacts extends PageBase
{

    //    private FastScrollRecyclerView mRecycler;
    private ContactsPageAdapter<RecyclerItemBase> mAdapter;

    public PageContacts(final Context context)
    {
        super(context);
        init(context);
    }

    public PageContacts(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public PageContacts(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context)
    {
        final LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View inflated = inf.inflate(R.layout.page_contacts, this, true);
        mRecycler = (ParallaxRecyclerView) inflated.findViewById(R.id.page_contacts_recycler);
//        mRecycler = (FastScrollRecyclerView) inflated.findViewById(R.id.page_contacts_recycler);

        mRecycler.setItemAnimator(new DefaultItemAnimator());
        mRecycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        mRecycler.addItemDecoration(new RecyclerSeparatorDecoration(context));

        mProgress = inflated.findViewById(R.id.progress);
        initErrorFrame(inflated);

        final T1000Dictionary.T1000DataSet dataSet = sortedEntries();
        if (dataSet == null)
        {
            // if dataSet is initially empty, show progress and wait for activity to send info about the resolution
            // (e.g. we might be currently waiting for user's permissions)
            mRecycler.setVisibility(GONE);
            mProgress.setVisibility(VISIBLE);

        }
        else
        {
            mProgress.setVisibility(GONE);
            displayContacts(context, dataSet);
        }
    }


    @Override
    protected void reloadData(final View view)
    {
        if (mParentCallback != null)
        {
            mErrorFrame.setVisibility(GONE);
            mRecycler.setVisibility(GONE);
            mProgress.setVisibility(VISIBLE);
            mParentCallback.requestReloadContacts();
        }
    }

    private void displayContacts(final Context context, final T1000Dictionary.T1000DataSet dataSet)
    {
        final OnContactItemAction onContactItemAction = new OnContactItemAction();
        T1000Entry prevEntry = null;

        mAdapter = new ContactsPageAdapter<>(context, mRecycler, this::onAdapterAction);
        if (dataSet != null)
        {
            for (final T1000Entry entry : dataSet.mEntries)
            {
                String firstLetter = Utils.firstLetterUnicode(entry.sortableName());
//                Utils.deAccent(Utils.firstLetterUnicode(prevEntry.sortableName()));
                if (prevEntry == null || !Utils.firstLetterUnicode(prevEntry.sortableName()).equals(firstLetter))
                {
                    mAdapter.mDataSet.add(new DividerItem(firstLetter, ContactsPageAdapter.VIEW_TYPE_DIVIDER, Dividers.DividerType.CONTACTS));
                }
                mAdapter.mDataSet.add(new ContactItem(entry, onContactItemAction));
                prevEntry = entry;
            }
            Log.i(this, "Updating contacts adapter with " + mAdapter.mDataSet.size() + " entries");
        }
        mRecycler.setAdapter(mAdapter);
    }

    private T1000Dictionary.T1000DataSet sortedEntries()
    {
        final T1000Dictionary.T1000DataSet dataSet = MemoryCache.Me().Load(T1000Dictionary.T1000DataSet.class);
        if (dataSet == null)
        {
            return null;
        }
        final SharedPrefs.ContactDisplayMode sortMode =
            mPrefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_CONTACT_SORTING_MODE, SharedPrefs.Defaults.CONTACT_SORTING_MODE);
        dataSet.sort(sortMode);
        return dataSet;
    }

    @Override
    public void handleCTLScroll(final int offset)
    {
        mRecycler.handleExternalScroll(offset);
    }

    @Override
    public void onContactsDataChanged(final T1000Dictionary.T1000DataSet entries)
    {
        if (entries == null)
        {
            handleLoadingError(R.string.page_contacts_error_permissions, true);
        }
        else if (entries.mEntries.isEmpty())
        {
            handleLoadingError(R.string.page_contacts_error_empty, false);
        }
        else
        {
            mErrorFrame.setVisibility(GONE);
            mProgress.setVisibility(GONE);
            mRecycler.setVisibility(VISIBLE);
            displayContacts(getContext(), sortedEntries());
        }
    }

    private void onAdapterAction(final VHBase vhBase, final BaseSwipeableAdapter.SwipeActionType swipeActionType)
    {
        Log.i(this, "Adapter action: " + swipeActionType);
        if (swipeActionType.equals(BaseSwipeableAdapter.SwipeActionType.CALL))
        {
            T1000Entry entry = ((ContactItem) mAdapter.mDataSet.get(vhBase.getAdapterPosition())).mEntry;
            if (!entry.mNumbers.isEmpty())
            {
                if (entry.mNumbers.size() == 1)
                {
                    SipServiceCommand.makeCall(getContext(), entry.mNumbers.get(0).mNumber);
                }
                else
                {
                    PhonePickerDialog phonePickerDialog = new PhonePickerDialog(new OnPhoneItemAction());
                    phonePickerDialog.ShowDialAction(getContext(), entry.mNumbers);
                }
            }
            else
            {
                Log.w(this, "empty phone list");
                // TODO: 2016-09-30 show some message
            }
        }
    }

    private class OnContactItemAction implements ContactItem.IOnContactAction
    {
        @Override
        public void onPressed(final VHBase view, final ContactItem item)
        {
            Log.i(this, "pressed contact " + item.mEntry.displayableName());
            final Intent intent = new Intent(getContext(), ActivityContactDetails.class);
            intent.putExtra(ActivityContactDetails.CONTACT_DETAILS_PARAM_ID, item.mEntry.mId);
            intent.putExtra(ActivityContactDetails.CONTACT_DETAILS_PARAM_THUMBNAIL_URI, item.mEntry.mThumbnailUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                final Activity activity = (Activity) getContext();
                final View statusBar = activity.findViewById(android.R.id.statusBarBackground);
                final View navigationBar = activity.findViewById(android.R.id.navigationBarBackground);

                final List<Pair<View, String>> pairs = new ArrayList<>();
                pairs.add(Pair.create(((ContactsVH) view).mHeader, getContext().getString(R.string.animation_contact_details_name)));
                pairs.add(Pair.create(((ContactsVH) view).mImageView, getContext().getString(R.string.animation_contact_details_photo)));
                if (statusBar != null)
                {
                    pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
                }
                if (navigationBar != null)
                {
                    pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
                }

                final ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pairs.toArray(new Pair[pairs.size()]));

                getContext().startActivity(intent, options.toBundle());
            }
            else
            {
                getContext().startActivity(intent);
            }
        }
    }

    private class OnPhoneItemAction implements NumberPickerDialogAdapter.IOnPhoneAction
    {

        @Override
        public void onPressed(final String number)
        {
            SipServiceCommand.makeCall(getContext(), number);
        }
    }

}
