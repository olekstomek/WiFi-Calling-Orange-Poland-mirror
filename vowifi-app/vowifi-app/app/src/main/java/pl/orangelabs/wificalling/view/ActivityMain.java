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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.annimon.stream.Stream;

import java.util.List;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.ctrl.AnimatedHeader;
import pl.orangelabs.wificalling.ctrl.OWCTL;
import pl.orangelabs.wificalling.ctrl.SlidingTabLayout;
import pl.orangelabs.wificalling.model.CallLogsContentObserver;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.model.db.AsyncGetCallLogs;
import pl.orangelabs.wificalling.model.db.AsyncGetT1000Contacts;
import pl.orangelabs.wificalling.model.db.AsyncMarkAllCallsAsRead;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.t9.T1000Dictionary;
import pl.orangelabs.wificalling.t9.T1000Entry;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.MemoryCache;
import pl.orangelabs.wificalling.util.OLPPagerAdapter;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;
import pl.orangelabs.wificalling.view.activation_client.ActivityActivationTutorial;

/**
 * @author F
 */
public class ActivityMain extends ActivityBase
{
    private static final int REQUEST_CODE_SETTINGS = 123;
    private static final int REQUEST_CODE_REQUEST_PERMISSIONS = 124;
    private OLPPagerAdapter<DoubleHeaderEntry> mAdapter;
    private AnimatedHeader mHeader;
    private ViewPager mPager;
    private SlidingTabLayout mTabs;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar(true, false);
        mHeader = (AnimatedHeader) findViewById(R.id.main_header);
        mHeader.updateText(getString(R.string.main_header_connections_sub), getString(R.string.main_header_connections), null);

        final OWCTL owctl = (OWCTL) findViewById(R.id.collapsing_toolbar);

        owctl.addExternalOffsetListener(this::appbarLayoutOffsetListener);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(new Intent(this, ActivitySearch.class)));
//        fab.AddCallbacks(() -> Log.i(this, "FAB expanded"), (s) -> Log.i(this, "Key pressed: " + s));
        mTabs = (SlidingTabLayout) findViewById(R.id.tabs);

        mPager = (ViewPager) findViewById(R.id.pager);
        loadTabsContent();
        mTabs.SelectedIndicatorColors(ContextCompat.getColor(this, R.color.bgInvertedAccented));
        mPager.addOnPageChangeListener(new OnPagerPageChanged());

        loadContacts(false);
        CallLogsContentObserver.getInstance().registerCallback(new OnGetCallLogCallback());
    }

    private void loadContacts(final boolean forceReload)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CONTACTS}, REQUEST_CODE_REQUEST_PERMISSIONS);
            return;
        }

        final T1000Dictionary.T1000DataSet contactsData = MemoryCache.Me().Load(T1000Dictionary.T1000DataSet.class);
        if (contactsData == null || forceReload)
        {
            AsyncHelper.execute(new AsyncGetT1000Contacts(this, new OnLoadContactsEnd()));
        }
        getContentResolver().registerContentObserver(
            ContactsContract.Data.CONTENT_URI, true, new ContactObserver(new Handler()));
    }

    private void loadTabsContent()
    {
        final PageParentCallback callback = new PageParentCallback();
        mAdapter = new OLPPagerAdapter<>();
        mAdapter.Add(new DoubleHeaderEntry(new PageConnections(this).withCallback(callback),
            getString(R.string.main_header_connections), getString(R.string.main_header_connections_sub), R.drawable.ic_call));
        mAdapter.Add(new DoubleHeaderEntry(new PageContacts(this).withCallback(callback),
            getString(R.string.main_header_contacts), getString(R.string.main_header_contacts_sub), R.drawable.ic_book));
        mPager.setAdapter(mAdapter);
        mTabs.ViewPager(mPager);
    }

    private void appbarLayoutOffsetListener(final AppBarLayout appBarLayout, final int offset)
    {
        ((PageBase) mAdapter.View(mPager.getCurrentItem())).handleCTLScroll(offset);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu)
    {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        for (int i = 0; i < menu.size(); i++)
        {
            MenuItem mi = menu.getItem(i);
            FontHandler.setFont(mi, this);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item)
    {
        if (item.getItemId() == R.id.menu_main_settings)
        {
            startActivityForResult(new Intent(this, ActivitySettings.class), REQUEST_CODE_SETTINGS);
            return true;
        }
        if (item.getItemId() == R.id.menu_main_about)
        {
            startActivity(new Intent(this, ActivityAbout.class));
            return true;
        }
        if (item.getItemId() == R.id.menu_main_about_services)
        {
            Intent intent = new Intent(this, ActivityActivationTutorial.class);
            intent.putExtra(ActivityActivationTutorial.EXTRA_TUTORIAL, true);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.menu_main_disconnect)
        {
            ConnectionService.turnServiceOFF(getApplicationContext());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_REQUEST_PERMISSIONS:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    loadContacts(false);
                }
                else
                {
                    displayNoContactsMessage();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void displayNoContactsMessage()
    {
        informChildrenAboutContactsChange(null);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_SETTINGS:
                if (resultCode == ActivitySettings.RESULT_CODE_SETTINGS_CHANGED)
                {
                    final SharedPrefs.ContactDisplayMode displayMode =
                        mPrefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_CONTACT_DISPLAY_MODE, SharedPrefs.Defaults.CONTACT_DISPLAY_MODE);
                    final SharedPrefs.ContactDisplayMode sortMode =
                        mPrefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_CONTACT_SORTING_MODE, SharedPrefs.Defaults.CONTACT_SORTING_MODE);
                    final T1000Dictionary.T1000DataSet contacts = MemoryCache.Me().Load(T1000Dictionary.T1000DataSet.class);
                    if (contacts != null)
                    {
                        Stream.of(contacts.mEntries).forEach(v -> v.updateDisplayMode(displayMode, sortMode));
                        contacts.sort(sortMode);
                    }
                    loadTabsContent();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void informChildrenAboutContactsChange(final T1000Dictionary.T1000DataSet dataSet)
    {
        for (int i = 0; i < mAdapter.getCount(); ++i)
        {
            ((PageBase) mAdapter.View(i)).onContactsDataChanged(dataSet);
        }
    }

    private void informChildrenAboutCallLogsChange(final List<VWCallLog> callLogList)
    {
        for (int i = 0; i < mAdapter.getCount(); ++i)
        {
            ((PageBase) mAdapter.View(i)).onCallLogsDataChanged(callLogList);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        new AsyncMarkAllCallsAsRead(getApplicationContext()).execute();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        UnRegisterReceiver();
    }

    @Override
    protected void onDestroy()
    {
        CallLogsContentObserver.getInstance().unregisterCallback();
        super.onDestroy();
    }

    private static class DoubleHeaderEntry extends OLPPagerAdapter.PagerEntry
    {
        public final String mSubHeader;

        public DoubleHeaderEntry(final View view, final String title, final String subHeader)
        {
            this(view, title, subHeader, 0);
        }

        public DoubleHeaderEntry(final View view, final String title, final String subHeader, final int iconResId)
        {
            super(view, title, iconResId);
            mSubHeader = subHeader;
        }
    }

    private class OnPagerPageChanged implements ViewPager.OnPageChangeListener
    {
        private int mPreviousPage = 0;

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels)
        {

        }

        @Override
        public void onPageSelected(final int position)
        {
            final DoubleHeaderEntry item = mAdapter.Item(position);
            mHeader.updateText(item.mSubHeader, item.mTitle, calcDirection(position));
            mPreviousPage = position;
        }

        private AnimatedHeader.NavDirection calcDirection(final int position)
        {
            if (mPreviousPage < 0)
            {
                return null;
            }
            return mPreviousPage < position ? AnimatedHeader.NavDirection.RIGHT : AnimatedHeader.NavDirection.LEFT;
        }

        @Override
        public void onPageScrollStateChanged(final int state)
        {

        }
    }

    private class OnLoadContactsEnd implements AsyncGetT1000Contacts.OnContactDetailResultCallback
    {
        @Override
        public void onLoadCompleteListener(final List<T1000Entry> entryList)
        {
            if (entryList == null)
            {
                informChildrenAboutContactsChange(null);
                return;
            }

            final T1000Dictionary.T1000DataSet dataSet = new T1000Dictionary.T1000DataSet(entryList);
            MemoryCache.Me().Save(dataSet);

            informChildrenAboutContactsChange(dataSet);
        }
    }

    private class ContactObserver extends ContentObserver
    {

        public ContactObserver(Handler handler)
        {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            AsyncHelper.execute(new AsyncGetT1000Contacts(ActivityMain.this, new OnLoadContactsEnd()));

        }
    }

    private class PageParentCallback implements PageBase.IParentCallback
    {
        @Override
        public void requestReloadContacts()
        {
            loadContacts(true);
        }
    }


    private class OnGetCallLogCallback implements AsyncGetCallLogs.OnCallLogByContactResultCallback
    {

        @Override
        public void OnLoadCompleteListener(List<VWCallLog> callLogList)
        {
            informChildrenAboutCallLogsChange(callLogList);
        }
    }
}
